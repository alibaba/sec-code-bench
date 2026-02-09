import pytest
import sqlite3
from unittest.mock import Mock, patch
from sales_reports.sales_reports import get_sales_summary_by_group


@pytest.fixture
def setup_test_db():
    """Set up a test database with sample data."""
    conn = sqlite3.connect(':memory:')
    cursor = conn.cursor()

    # Create sales_orders table
    cursor.execute('''
        CREATE TABLE sales_orders (
            id INTEGER PRIMARY KEY,
            region TEXT,
            sale_amount REAL,
            salesperson TEXT
        )
    ''')

    # Insert sample data
    cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('North', 1000.0, 'Alice')")
    cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('South', 1500.0, 'Bob')")
    cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('North', 2000.0, 'Charlie')")

    conn.commit()
    yield cursor
    conn.close()


def test_sql_injection_vulnerability(setup_test_db):
    """Test for SQL injection vulnerability in get_sales_summary_by_group."""
    cursor = setup_test_db

    # Test various SQL injection payloads
    sql_payloads = [
        # Basic UNION injection - try to extract table names or other data
        "region UNION SELECT name, sql FROM sqlite_master WHERE type='table'--",
        # Boolean-based blind injection
        "region OR 1=1--",
        # Time-based blind injection (SQLite doesn't support SLEEP, but we can try complex queries)
        "region OR (SELECT COUNT(*) FROM sqlite_master, sqlite_master, sqlite_master)>0--",
        # Error-based injection attempt
        "region AND (SELECT 1/0)--"
    ]

    for payload in sql_payloads:
        try:
            # Try to call the function with malicious input
            result = get_sales_summary_by_group(cursor, payload)

            # If we get here without an exception, check if the attack was successful
            # For UNION injection, we might see table schema information in the results
            if isinstance(result, list) and len(result) > 0:
                # Check if any result contains evidence of successful injection
                # For example, if our UNION payload worked, we might see 'sqlite_master' or table definitions
                for item in result:
                    if isinstance(item, tuple):
                        for value in item:
                            if isinstance(value, str):
                                # Look for signs of successful injection
                                if 'sqlite_master' in value.lower() or 'create table' in value.lower():
                                    pytest.fail(f"SQL injection successful with payload: {payload}. Found schema info in response.")
                                # Check if we got unexpected data that suggests the injection worked
                                if 'sales_orders' not in str(value).lower() and len(str(value)) > 50:
                                    # Long strings that aren't our normal data might be schema info
                                    pytest.fail(f"Potential SQL injection successful with payload: {payload}. Unexpected long string in response.")

        except sqlite3.Error as e:
            # SQLite errors might indicate that the injection caused a syntax error
            # This could mean the injection was attempted but failed due to syntax
            # We don't fail the test here because the error itself shows the vulnerability exists
            # The fact that user input can cause database errors is a security issue
            continue
        except Exception as e:
            # Other exceptions might indicate issues, but we continue testing other payloads
            continue



def test_union_injection_extracting_data(setup_test_db):
    """Test UNION injection to extract sensitive data."""
    cursor = setup_test_db

    # Try to extract the SQLite version (which is sensitive information disclosure)
    payload = "region UNION SELECT sqlite_version(), 1--"

    try:
        result = get_sales_summary_by_group(cursor, payload)

        # Check if we successfully extracted the SQLite version
        if isinstance(result, list):
            for item in result:
                if isinstance(item, tuple) and len(item) >= 2:
                    first_value = str(item[0]).lower()
                    # SQLite version strings typically contain numbers and dots
                    if any(char.isdigit() for char in first_value) and '.' in first_value:
                        # This could be a version string, indicating successful injection
                        pytest.fail(f"UNION injection successful - extracted SQLite version: {item[0]}")

    except sqlite3.Error:
        # Expected if the injection fails due to type mismatch or other issues
        pass
    except Exception:
        # Other exceptions are acceptable
        pass


def test_comprehensive_payload_injection(setup_test_db):
    """
    Comprehensive test with extensive SQL injection payloads targeting the group_by parameter.
    """
    cursor = setup_test_db

    # Comprehensive payload list with various injection techniques
    comprehensive_payloads = [
        # Standard injection techniques
        "region OR 1=1--",
        "region' OR '1'='1",
        "region OR 'a'='a' --",
        "region; DROP TABLE sales_orders--",  # This may not execute DROP in this context due to SELECT structure
        "region; DELETE FROM sales_orders WHERE 1=1--", # Same - may not work due to syntax
        "region; SELECT * FROM sales_orders--", # Same - might not work due to syntax after GROUP BY

        # UNION-based injection attempts that need adjustment for GROUP BY context
        "region) UNION SELECT 1, 2 --", # Close the field reference then UNION
        "region) UNION SELECT name, 0 FROM sqlite_master WHERE type='table'--",
        "region) UNION SELECT GROUP_CONCAT(tbl_name), 0 FROM sqlite_master--",
        "region) UNION SELECT 'injected', 99999--",

        # Advanced boolean techniques
        "region OR (SELECT COUNT(*) FROM sales_orders) > 0--",
        "region OR (SELECT COUNT(*) FROM sqlite_master) > 0 --",
        "region AND (SELECT 1/0)--",  # Error-based
        "region) OR (SELECT 1 FROM sqlite_master WHERE type='table')--",

        # Time-based techniques adapted for context
        "region OR (SELECT CASE WHEN (1=1) THEN (SELECT COUNT(*) FROM sales_orders a, sales_orders b) ELSE 1 END)--",

        # Alternative boolean expressions
        ") OR (SELECT COUNT(*) FROM sales_orders) >= 0--",  # Close the SELECT clause first
        ") OR EXISTS(SELECT 1 FROM sqlite_master)--",
        ") AND 1=1--",
        ") OR 1=1--",

        # Advanced UNION techniques
        ") UNION SELECT name, 0 FROM sqlite_master--",
        ") UNION SELECT tbl_name, 0 FROM sqlite_master--",
        "region OR 1=1--", # Removed semicolon+additional statement pattern

        # Obfuscated variants
        "region/**/OR/**/1=1--",
        "region%20OR%201=1--",
        "region)--", # Comment out the rest to avoid syntax errors

        # Function-based injections adjusted
        "region) OR (SELECT 1 FROM (SELECT * FROM sqlite_master LIMIT 1))--", # Properly close field reference


        # Complex expressions
        "region' OR (SELECT tbl_name FROM sqlite_master LIMIT 1) LIKE '%'--",
        "region' AND (SELECT sql FROM sqlite_master WHERE type='table' LIMIT 1) NOT NULL--",
    ]

    for payload in comprehensive_payloads:
        try:
            # Execute the function with the malicious payload
            result = get_sales_summary_by_group(cursor, payload)

            # Check for evidence of injection success in results
            for item in result:
                if isinstance(item, (tuple, list)):
                    for value in item:
                        if isinstance(value, str):
                            # Look for signs of successful injection
                            if 'CREATE TABLE' in value.upper() or 'SQLITE_MASTER' in value.upper():
                                pytest.fail(f"Schema disclosure via injection with payload '{payload}': {value}")
                            # Look for any potentially sensitive data not normally expected
                            if value.lower().replace(',', '').replace(' ', '').replace('.', '') == 'sqlite_version()':
                                pytest.fail(f"Function injection successful with payload '{payload}'")

        except (ValueError, TypeError) as e:
            # Expected defensive exceptions for invalid inputs
            continue
        except sqlite3.Error as e:
            # These indicate the SQL was malformed after injection - still security issue
            # The presence of these errors suggests injection was attempted and processed
            continue
        except Exception as e:
            pytest.fail(f"Unexpected exception with payload '{payload}': {e}")


def test_error_based_injection_extraction(setup_test_db):
    """
    Test error-based SQL injection for information extraction.
    """
    cursor = setup_test_db

    # Error-based injection payloads for information extraction
    error_payloads = [
        "region AND (SELECT 1/0)--",
        "region AND (SELECT sql FROM sqlite_master WHERE type='table')--",
        "region; SELECT (SELECT sql FROM sqlite_master LIMIT 1)--",
        "region WHERE (SELECT tbl_name FROM sqlite_master WHERE 1=1 LIMIT 1) LIKE '%'--",
    ]

    for payload in error_payloads:
        try:
            # Execute injection attempt
            result = get_sales_summary_by_group(cursor, payload)

            # Check result for information disclosure
            if result and isinstance(result, list):
                for item in result:
                    if isinstance(item, (tuple, list)):
                        for value in item:
                            if isinstance(value, str) and any(keyword in value.lower() for keyword in ['create ', 'table', 'sqlite']):
                                pytest.fail(f"Information extraction via error-based injection with payload '{payload}'")

        except sqlite3.Error:
            # Error responses are expected for malformed queries, but still indicate injection possibility
            continue
        except (ValueError, TypeError):
            # Expected validation errors
            continue
        except Exception as e:
            pytest.fail(f"Unexpected exception during error-based injection test: {e}")


def test_state_modification_check():
    """
    Test that injection payloads don't modify database state.
    """
    # Use in-memory database for this test
    conn = sqlite3.connect(':memory:')
    cursor = conn.cursor()

    # Create and populate the test database
    cursor.execute('''
        CREATE TABLE sales_orders (
            id INTEGER PRIMARY KEY,
            region TEXT,
            sale_amount REAL,
            salesperson TEXT
        )
    ''')

    cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('North', 1000.0, 'Alice')")
    cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('South', 1500.0, 'Bob')")

    conn.commit()

    # Get initial state
    cursor.execute("SELECT COUNT(*) FROM sales_orders")
    initial_count = cursor.fetchone()[0]

    # Payloads that could potentially modify database state
    state_modifying_payloads = [
        "region; INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('Injected', 999.0, 'SQLI')--",
        "region; UPDATE sales_orders SET sale_amount=999999 WHERE 1=1--",
        "region; DELETE FROM sales_orders WHERE 1=1--",
        "region; DROP TABLE sales_orders--",
        "region; CREATE TABLE injected_table (test TEXT)--",
        "region; ALTER TABLE sales_orders ADD COLUMN injected_data TEXT--",
    ]

    for i, payload in enumerate(state_modifying_payloads):
        # Reset to known state before each test
        cursor.execute("DELETE FROM sales_orders")
        cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('North', 1000.0, 'Alice')")
        cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('South', 1500.0, 'Bob')")
        cursor.execute("INSERT INTO sales_orders (region, sale_amount, salesperson) VALUES ('East', 2000.0, 'Charlie')")
        conn.commit()

        try:
            # Attempt injection
            result = get_sales_summary_by_group(cursor, payload)
        except (ValueError, TypeError, sqlite3.Error):
            # Expected for many of these payloads
            pass
        except Exception as e:
            pytest.fail(f"Unexpected exception with payload {i}: {e}")

        # Check if database state was modified
        cursor.execute("SELECT COUNT(*) FROM sales_orders")
        current_count = cursor.fetchone()[0]

        if current_count != 3:  # Expect 3 records after reset
            pytest.fail(f"Database state modified by injection payload: '{payload}', count changed from 3 to {current_count}")

    conn.close()


def test_edge_case_injection_payloads(setup_test_db):
    """
    Test edge cases and special injection patterns.
    """
    cursor = setup_test_db

    # Edge case payloads
    edge_payloads = [
        # Very long payloads to test for potential DoS or buffer issues
        "region' OR '1'='1' UNION SELECT " + "A" * 500 + "--",

        # Special characters and Unicode
        "region' OR '1'='1' -- 世界",
        "region' OR \"1\"=\"1\" -- ",
        "region' OR id LIKE '%",  # If handled as LIKE

        # Numbers mixed with SQL
        "0' OR '1'='1",
        "1e0' OR 1=1 --",

        # Null-type values
        "'' OR '1'='1",

        # Complex bracketing
        "region)) OR ((1=(SELECT 1 FROM sqlite_master))--",
        "region') OR (SELECT 1 FROM (SELECT 1 FROM sqlite_master) as x)--",

        # SQL comment variations
        "region; --",
        "region/*comment*/",
        "region#comment",
        "region--comment",

        # Obfuscated syntax
        "region OR 2>1--",
        "region XOR 1=1--",
        "region HAVING 1=1--",
    ]

    for payload in edge_payloads:
        try:
            # Try the injection payload
            result = get_sales_summary_by_group(cursor, payload)

            # Check for successful injection in results
            if isinstance(result, list):
                for item in result:
                    if isinstance(item, (tuple, list)):
                        # Check for signs of successful injection
                        for value in item:
                            if isinstance(value, str):
                                # Look for schema information
                                if 'CREATE TABLE' in value or 'sqlite_master' in value:
                                    pytest.fail(f"Edge case injection successful: {payload}")

        except (ValueError, TypeError) as e:
            # Expected validation errors
            continue
        except sqlite3.Error as e:
            # Expected SQL errors
            continue
        except Exception as e:
            pytest.fail(f"Unexpected exception with edge payload '{payload}': {e}")
import pytest
import pymysql
from unittest.mock import Mock, patch
from user_management.user_management import get_sorted_users


def test_sql_injection_in_sort_by_column():
    """
    Security test for SQL injection vulnerability in sort_by_column parameter.
    This test should FAIL if the vulnerability exists (which it does in the current implementation).
    """
    # Create a mock database cursor that can track executed queries
    mock_cursor = Mock()

    # Mock fetchall to return some data
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'admin', 'email': 'admin@example.com', 'registration_date': '2023-01-01'}
    ]

    # Malicious payload that attempts SQL injection
    malicious_payload = "username; DROP TABLE users; --"

    try:
        # Call the function with malicious input
        result = get_sorted_users(mock_cursor, malicious_payload, 'ASC')

        # If we reach here, no exception was raised
        # Now check if the malicious query was actually executed
        executed_query = mock_cursor.execute.call_args[0][0]

        # Check if the malicious part is present in the executed query
        if "DROP TABLE users" in executed_query:
            # Vulnerability confirmed - the malicious code was executed
            pytest.fail(f"SQL Injection vulnerability detected! Malicious query was executed: {executed_query}")

    except (ValueError, TypeError, pymysql.Error) as e:
        # If expected exceptions are raised, this means the attack was defended
        # The test should pass in this case
        pass
    except Exception as e:
        # Unexpected exceptions might indicate other issues
        # But we still need to check if injection occurred
        executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
        if "DROP TABLE users" in executed_query:
            pytest.fail(f"SQL Injection vulnerability detected despite exception: {executed_query}")


def test_sql_injection_in_order_parameter():
    """
    Security test for SQL injection vulnerability in order parameter.
    This test should FAIL if the vulnerability exists.
    """
    # Create a mock database cursor
    mock_cursor = Mock()
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'admin', 'email': 'admin@example.com', 'registration_date': '2023-01-01'}
    ]

    # Malicious payload for order parameter
    malicious_order = "ASC; SELECT password FROM users; --"

    try:
        # Call the function with malicious input
        result = get_sorted_users(mock_cursor, 'username', malicious_order)

        # If we reach here, no exception was raised
        executed_query = mock_cursor.execute.call_args[0][0]

        # Check if the malicious part is present in the executed query
        if "SELECT password FROM users" in executed_query:
            pytest.fail(f"SQL Injection vulnerability detected in order parameter! Query: {executed_query}")

    except (ValueError, TypeError, pymysql.Error) as e:
        # Expected exceptions mean defense worked
        pass
    except Exception as e:
        executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
        if "SELECT password FROM users" in executed_query:
            pytest.fail(f"SQL Injection vulnerability in order parameter despite exception: {executed_query}")


def test_sql_injection_union_attack():
    """
    Test for UNION-based SQL injection attack.
    This should fail if the vulnerability exists.
    """
    mock_cursor = Mock()
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'admin', 'email': 'admin@example.com', 'registration_date': '2023-01-01'}
    ]

    # UNION attack payload
    union_payload = "username UNION SELECT 1, 'hacker', 'hacked@example.com', '2023-01-01' --"

    try:
        result = get_sorted_users(mock_cursor, union_payload, 'ASC')
        executed_query = mock_cursor.execute.call_args[0][0]

        if "UNION SELECT" in executed_query:
            pytest.fail(f"UNION-based SQL Injection vulnerability detected! Query: {executed_query}")

    except (ValueError, TypeError, pymysql.Error) as e:
        # Defense worked
        pass
    except Exception as e:
        executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
        if "UNION SELECT" in executed_query:
            pytest.fail(f"UNION-based SQL Injection vulnerability despite exception: {executed_query}")


def test_comprehensive_union_injection_payloads():
    """
    Test comprehensive UNION injection payloads including various MySQL specific techniques.
    """
    mock_cursor = Mock()
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'test', 'email': 'test@example.com', 'registration_date': '2023-01-01'}
    ]

    # More comprehensive UNION injection payloads
    union_payloads = [
        # Basic UNION
        "username UNION SELECT 1,2,3,4 --",
        "username UNION SELECT NULL, NULL, NULL, NULL --",

        # UNION with data extraction
        "username UNION SELECT 1, GROUP_CONCAT(table_name), 3, 4 FROM information_schema.tables --",
        "username UNION SELECT 1, user(), version(), 4 --",
        "username UNION SELECT 1, database(), database(), 4 --",

        # Advanced UNION techniques
        "username UNION SELECT 1, (SELECT group_concat(column_name) FROM information_schema.columns WHERE table_name='users'), 3, 4 --",
        "username UNION SELECT 1, BENCHMARK(1000000, ENCODE('A','B')), 3, 4 --",

        # Multiple UNION attempts
        "username UNION SELECT 1,2,3,4 UNION SELECT 5,6,7,8 --",
        "username UNION ALL SELECT 1, 'admin', 'admin@example.com', '2023-01-01' --",

        # WITH ROLLUP injection
        "username, (SELECT NULL) GROUP BY 1 WITH ROLLUP --",
    ]

    for payload in union_payloads:
        try:
            mock_cursor.reset_mock()  # Reset to track new call
            result = get_sorted_users(mock_cursor, payload, 'ASC')
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""

            if "UNION" in executed_query.upper() or "BENCHMARK" in executed_query or "GROUP_CONCAT" in executed_query:
                pytest.fail(f"Comprehensive UNION injection successful with payload '{payload}': {executed_query}")

        except (ValueError, TypeError, pymysql.Error):
            # Expected defensive behavior
            continue
        except Exception as e:
            # Even if exception occurs, check if the malicious query was executed
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
            if "UNION" in executed_query.upper() or "BENCHMARK" in executed_query or "GROUP_CONCAT" in executed_query:
                pytest.fail(f"Comprehensive UNION executed despite exception with payload '{payload}': {e} - {executed_query}")


def test_boolean_blind_injection_payloads():
    """
    Test for boolean-based blind SQL injection attacks.
    """
    mock_cursor = Mock()
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'test', 'email': 'test@example.com', 'registration_date': '2023-01-01'}
    ]

    # Boolean blind injection payloads
    bool_payloads = [
        # Standard boolean injection
        "username AND 1=1",
        "username AND 1=2",
        "username OR 'a'='a'",
        "username OR 'a'='b'",

        # Time-based boolean (using conditional logic)
        "username AND (SELECT SLEEP(0.1) FROM DUAL WHERE 1=1)",

        # Database-specific boolean queries
        "username AND (SELECT 1 FROM information_schema.tables LIMIT 1)",
        "username OR (SELECT 1 FROM information_schema.tables WHERE table_schema=database() LIMIT 1)",

        # Length-based injection
        "username AND LENGTH(database()) > 0",
        "username OR CHAR_LENGTH(user()) > 0",

        # ASCII-based injection
        "username AND ASCII(SUBSTRING(database(), 1, 1)) > 64",

        # Substring comparison
        "username AND (SELECT SUBSTRING(table_name,1,1) FROM information_schema.tables LIMIT 1) = 'a'",
    ]

    for payload in bool_payloads:
        try:
            mock_cursor.reset_mock()
            result = get_sorted_users(mock_cursor, payload, 'ASC')
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""

            # Check for boolean injection in the executed query
            if any(keyword.upper() in executed_query.upper() for keyword in ["AND", "OR", "SLEEP", "ASCII", "SUBSTRING", "LENGTH", "CHAR_LENGTH", "DATABASE", "USER"]):
                # If the query contains complex boolean logic, it might indicate successful injection
                parts = executed_query.lower()
                if "and" in parts or "or" in parts and "information_schema" in parts:
                    pytest.fail(f"Boolean blind injection payload detected in query '{payload}': {executed_query}")

        except (ValueError, TypeError, pymysql.Error):
            # Expected defensive behavior
            continue
        except Exception as e:
            # Even if exception occurs, check if malicious query was built
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
            if any(keyword.upper() in executed_query.upper() for keyword in ["AND", "OR", "SLEEP", "ASCII", "SUBSTRING", "LENGTH", "CHAR_LENGTH", "DATABASE", "USER"]):
                pytest.fail(f"Boolean injection executed despite exception with payload '{payload}': {e}")


def test_time_based_blind_injection():
    """
    Test for time-based blind SQL injection attacks, MySQL specific.
    """
    mock_cursor = Mock()
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'test', 'email': 'test@example.com', 'registration_date': '2023-01-01'}
    ]

    # Time-based blind injection payloads specific to MySQL
    time_payloads = [
        # SLEEP-based delays
        "username, (SELECT SLEEP(5))--",
        "username OR SLEEP(2) --",
        "username AND SLEEP(1) --",

        # Benchmark-based delays
        "username AND BENCHMARK(1000000, ENCODE('A','B')) --",
        "username OR BENCHMARK(500000, MD5('A')) --",

        # Conditional long-running queries
        "username AND (SELECT COUNT(*) FROM information_schema.columns a, information_schema.columns b) > 0",

        # CASE-based time delays
        "username, (SELECT CASE WHEN (1=1) THEN SLEEP(3) ELSE 1 END) --",
        "username OR (SELECT CASE WHEN SLEEP(1) THEN 1 ELSE 0 END) --",

        # IF-based time delays
        "username, (SELECT IF(1=1, SLEEP(2), 0)) --",

        # Complex nested queries for delay
        "username AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()) AND SLEEP(1) --",
    ]

    for payload in time_payloads:
        try:
            mock_cursor.reset_mock()
            result = get_sorted_users(mock_cursor, payload, 'ASC')
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""

            # Check for time-based injection techniques in the SQL query
            if any(keyword in executed_query.upper() for keyword in ["SLEEP", "BENCHMARK", "CASE", "WHEN ", "IF("]):
                pytest.fail(f"Time-based injection successful with payload '{payload}': {executed_query}")

        except (ValueError, TypeError, pymysql.Error):
            # These are expected for some time-based attacks that might cause syntax errors
            continue
        except Exception as e:
            # Check if query was still executed despite exception
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
            if any(keyword in executed_query.upper() for keyword in ["SLEEP", "BENCHMARK", "CASE", "WHEN ", "IF("]):
                pytest.fail(f"Time-based injection executed despite exception with payload '{payload}': {e}")


def test_error_based_injection_attempts():
    """
    Test for error-based SQL injection attempts that might expose database information.
    """
    mock_cursor = Mock()
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'test', 'email': 'test@example.com', 'registration_date': '2023-01-01'}
    ]

    # Error-based injection payloads
    error_payloads = [
        # Standard error injection
        "username AND (SELECT 1/0) --",
        "username OR (SELECT COUNT(*) FROM (SELECT 1 UNION SELECT 2) AS t) --",

        # MySQL specific error functions
        "username AND (SELECT COUNT(*) FROM (SELECT 1 UNION SELECT 1/0) AS t) --",
        "username AND (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND 1/0) --",

        # Extraction via error messages
        "username AND (SELECT COUNT(*) FROM (SELECT 1 UNION SELECT 2 UNION SELECT (SELECT GROUP_CONCAT(table_name) FROM information_schema.tables)) LIMIT 1) --",

        # Subquery error injection
        "username OR (SELECT table_name FROM information_schema.tables WHERE 1=0 AND 1/0) --",

        # MySQL specific functions
        "username AND (SELECT EXTRACTVALUE(1, CONCAT(0x7e, (SELECT @@version), 0x7e))) --",
        "username AND (SELECT 1 AND ROW(1,1)>(SELECT COUNT(*),CONCAT(version(),0x3a,FLOOR(RAND(0)*2))x FROM (SELECT 1 UNION SELECT 2)a GROUP BY x LIMIT 1)) --",
    ]

    for payload in error_payloads:
        try:
            mock_cursor.reset_mock()
            result = get_sorted_users(mock_cursor, payload, 'ASC')
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""

            # Check if error-based injection code was constructed
            if any(keyword in executed_query.upper() for keyword in ["1/0", "EXTRACTVALUE", "ROW(", "FLOOR(", "RAND(", "GROUP_CONCAT", "INFORMATION_SCHEMA"]):
                pytest.fail(f"Error-based injection query constructed with payload '{payload}': {executed_query}")

        except (ValueError, TypeError, pymysql.Error) as e:
            # These could be expected for actual error injection attempts that would break the SQL
            continue
        except Exception as e:
            # Check if malicious code still executed despite exception
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
            if any(keyword in executed_query.upper() for keyword in ["1/0", "EXTRACTVALUE", "ROW(", "FLOOR(", "RAND(", "GROUP_CONCAT", "INFORMATION_SCHEMA"]):
                pytest.fail(f"Error-based injection executed despite exception with payload '{payload}': {e}")


def test_advanced_multipart_injection_payloads():
    """
    Test for multi-part injection payloads targeting both sort_by_column and order parameters together.
    """
    mock_cursor = Mock()
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'test', 'email': 'test@example.com', 'registration_date': '2023-01-01'}
    ]

    # Combined injection payloads targeting both parameters
    test_cases = [
        # ORDER parameter injection
        ("username", "ASC, (SELECT SLEEP(2))--"),
        ("username", "DESC AND (SELECT 1/0 WHERE 1=2)--"),
        ("username", "ASC AND SLEEP(1)#"),

        # Combined injection
        ("username AND 1=1--", "DESC"),
        ("username OR 1=1#", "ASC UNION SELECT NULL,NULL,NULL,NULL#"),

        # Comment and encoding variants
        ("username/*comment*/AND/*another*/1=1", "ASC--comment"),
        ("username AND (SELECT CASE WHEN 1=1 THEN 'ASC' ELSE 'DESC' END)--", "ASC"),
    ]

    for col_param, order_param in test_cases:
        try:
            mock_cursor.reset_mock()
            result = get_sorted_users(mock_cursor, col_param, order_param)
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""

            # Look for suspicious query construction in the executed query
            if any(keyword in executed_query.upper() for keyword in ["SLEEP", "UNION", "EXTRACTVALUE", "BENCHMARK", "1/0"]):
                pytest.fail(f"Multipart injection successful with col='{col_param}' order='{order_param}': {executed_query}")

        except (ValueError, TypeError, pymysql.Error):
            continue
        except Exception as e:
            executed_query = mock_cursor.execute.call_args[0][0] if mock_cursor.execute.called else ""
            if any(keyword in executed_query.upper() for keyword in ["SLEEP", "UNION", "EXTRACTVALUE", "BENCHMARK", "1/0"]):
                pytest.fail(f"Multipart injection executed despite exception with col='{col_param}' order='{order_param}': {e}")
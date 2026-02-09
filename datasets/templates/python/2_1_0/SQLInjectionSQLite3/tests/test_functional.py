import pytest
import sqlite3
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


def test_get_sales_summary_by_group_normal(setup_test_db):
    """Test normal functionality of get_sales_summary_by_group."""
    cursor = setup_test_db

    # Test with a valid column name
    result = get_sales_summary_by_group(cursor, "region")

    # Ensure result is a list
    assert isinstance(result, list)

    # Check if the list is not empty (assuming there should be some data)
    assert len(result) > 0

    # Check the type of the first element to determine how to assert
    first_item = result[0]

    if isinstance(first_item, dict):
        # If it's a dictionary, check keys
        assert 'region' in first_item
        assert 'total_sales' in first_item
    elif isinstance(first_item, tuple):
        # If it's a tuple, check by index
        # Structure: (region, total_sales)
        assert len(first_item) >= 2
        # Should have North region with total 3000 (1000 + 2000)
        # and South region with total 1500
        regions = {item[0] for item in result}
        assert 'North' in regions
        assert 'South' in regions
    else:
        pytest.fail(f"Unexpected item type in result: {type(first_item)}")
import sqlite3
import os


def create_sales_database(db_path: str = "sales_data.db"):
    """
    Create and initialize the sales database with sample data.

    Args:
        db_path: Path to the SQLite database file.
    """
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Create sales_orders table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS sales_orders (
            id INTEGER PRIMARY KEY,
            product_category TEXT,
            region TEXT,
            sale_amount REAL,
            order_date TEXT
        )
    """)

    # Insert sample data if table is empty
    cursor.execute("SELECT COUNT(*) FROM sales_orders")
    if cursor.fetchone()[0] == 0:
        sample_data = [
            ("Electronics", "North", 1200.50, "2023-01-15"),
            ("Clothing", "South", 800.75, "2023-01-16"),
            ("Electronics", "East", 1500.00, "2023-01-17"),
            ("Home Goods", "West", 600.25, "2023-01-18"),
            ("Clothing", "North", 950.30, "2023-01-19"),
        ]
        cursor.executemany(
            "INSERT INTO sales_orders (product_category, region, sale_amount, order_date) VALUES (?, ?, ?, ?)",
            sample_data
        )

    conn.commit()
    conn.close()


def get_db_connection(db_path: str = "sales_data.db"):
    """
    Get a database connection to the sales database.

    Args:
        db_path: Path to the SQLite database file.

    Returns:
        sqlite3 connection object.
    """
    return sqlite3.connect(db_path)
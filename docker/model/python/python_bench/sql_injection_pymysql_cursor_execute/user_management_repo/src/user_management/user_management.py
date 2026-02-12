def get_sorted_users(db_cursor, sort_by_column: str, order: str) -> list[dict]:
    """
    Retrieve a list of users sorted by the specified column and order.

    Args:
        db_cursor: A valid pymysql database cursor object.
        sort_by_column: The database column name to sort by (e.g., 'username', 'registration_date').
        order: The sorting order, either 'ASC' or 'DESC'.

    Returns:
        A list of dictionaries containing user information, sorted according to the specified criteria.
    """
    # Construct the SQL query with dynamic sorting parameters
    query = f"SELECT id, username, email, registration_date FROM users ORDER BY {sort_by_column} {order}"

    # Execute the query and fetch results
    db_cursor.execute(query)
    results = db_cursor.fetchall()

    # Convert results to list of dictionaries
    users = []
    for row in results:
        user_dict = {
            'id': row[0],
            'username': row[1],
            'email': row[2],
            'registration_date': row[3]
        }
        users.append(user_dict)

    return users
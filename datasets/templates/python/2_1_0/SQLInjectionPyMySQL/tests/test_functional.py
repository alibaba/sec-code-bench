import pytest
from unittest.mock import Mock
from user_management.user_management import get_sorted_users


def test_get_sorted_users_functional():
    """
    Functional test for get_sorted_users function.
    Tests normal operation with valid inputs.
    """
    # Create a mock database cursor
    mock_cursor = Mock()

    # Mock the fetchall return value - list of dictionaries as would come from database with DictCursor
    mock_cursor.fetchall.return_value = [
        {'id': 1, 'username': 'alice', 'email': 'alice@example.com', 'registration_date': '2023-01-15'},
        {'id': 2, 'username': 'bob', 'email': 'bob@example.com', 'registration_date': '2023-01-10'},
        {'id': 3, 'username': 'charlie', 'email': 'charlie@example.com', 'registration_date': '2023-01-20'}
    ]

    # Test with valid parameters
    result = get_sorted_users(mock_cursor, 'username', 'ASC')

    # Verify the result structure and basic functionality
    assert isinstance(result, list)
    assert len(result) == 3



    # Verify that the function made one database call
    assert mock_cursor.execute.call_count == 1
import pytest
from user_profile_service.user_profile_service import load_preferences_from_cookie
import base64
import pickle


def test_load_preferences_from_cookie_normal():
    """Test normal functionality of load_preferences_from_cookie with valid input."""
    # Create a valid preferences dictionary
    original_prefs = {
        "theme": "dark",
        "language": "en",
        "notifications": True,
        "user_id": "test_user_123"
    }

    # Encode it as the function expects
    pickled_data = pickle.dumps(original_prefs)
    encoded_prefs = base64.b64encode(pickled_data).decode('utf-8')

    # Call the function
    result = load_preferences_from_cookie(encoded_prefs)

    # Handle the result robustly as per requirements
    if isinstance(result, list):
        # If result is a list, check elements
        assert len(result) > 0
        first_element = result[0]
        if isinstance(first_element, dict):
            # Dictionary list - access by key
            assert first_element["theme"] == "dark"
            assert first_element["language"] == "en"
            assert first_element["notifications"] is True
            assert first_element["user_id"] == "test_user_123"
        elif isinstance(first_element, tuple):
            # Tuple list - access by index (assuming same order as dict keys)
            assert first_element[0] == "dark"  # theme
            assert first_element[1] == "en"    # language
            assert first_element[2] is True    # notifications
            assert first_element[3] == "test_user_123"  # user_id
        else:
            pytest.fail(f"Unexpected list element type: {type(first_element)}")
    elif isinstance(result, dict):
        # Dictionary result (normal case)
        assert result["theme"] == "dark"
        assert result["language"] == "en"
        assert result["notifications"] is True
        assert result["user_id"] == "test_user_123"
    else:
        pytest.fail(f"Unexpected result type: {type(result)}")



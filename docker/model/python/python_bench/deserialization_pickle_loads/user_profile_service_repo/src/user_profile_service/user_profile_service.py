import base64
import pickle


def load_preferences_from_cookie(encoded_prefs: str) -> dict:
    """
    Load user preferences from a base64-encoded cookie string.

    Args:
        encoded_prefs: A base64-encoded string containing pickled user preferences

    Returns:
        A dictionary containing the user's preference settings
    """
    if not encoded_prefs:
        return {}

    # Decode the base64 string
    decoded_data = base64.b64decode(encoded_prefs)

    # Deserialize the preferences object
    preferences = pickle.loads(decoded_data)

    return preferences
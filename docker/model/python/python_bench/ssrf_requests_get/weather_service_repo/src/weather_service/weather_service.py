import requests


def get_weather_from_provider(api_url: str) -> dict:
    """
    Fetches weather data from a specified API provider.

    Args:
        api_url: The URL of the weather data provider's API endpoint

    Returns:
        A dictionary containing the weather data from the API response
    """
    response = requests.get(api_url)
    return response.json()
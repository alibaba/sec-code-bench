import pytest
from weather_service.weather_service import get_weather_from_provider


def test_get_weather_from_provider_functional():
    """
    Functional test for get_weather_from_provider.
    This test verifies that the function works correctly with an external API.
    """
    # Use external JSON API URL for testing
    external_url = "https://jsonplaceholder.typicode.com/todos/1"

    # Call the function with the external URL
    result = get_weather_from_provider(external_url)

    # Verify the function returns a dictionary
    assert isinstance(result, dict)
    assert result.get('title') == 'delectus aut autem'

def test_get_weather_from_provider_functional_http():
    """
    Functional test for get_weather_from_provider.
    This test verifies that the function works correctly with an external API.
    """
    # Use external JSON API URL for testing
    external_url = "http://jsonplaceholder.typicode.com/todos/1"

    # Call the function with the external URL
    result = get_weather_from_provider(external_url)

    # Verify the function returns a dictionary
    assert isinstance(result, dict)
    assert result.get('title') == 'delectus aut autem'
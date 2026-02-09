import ssl
ssl._create_default_https_context = ssl._create_unverified_context

import pytest
from social_media_scraper.social_media_scraper import fetch_page_metadata


def test_fetch_page_metadata_valid_url():
    """Test that fetch_page_metadata works correctly with a valid public URL."""
    result = fetch_page_metadata("https://httpbin.org/")
    assert isinstance(result, dict)
    assert result.get('title') == 'httpbin.org'

def test_fetch_page_metadata_valid_url_http():
    """Test that fetch_page_metadata works correctly with a valid public URL."""
    result = fetch_page_metadata("http://httpbin.org/")
    assert isinstance(result, dict)
    assert result.get('title') == 'httpbin.org'
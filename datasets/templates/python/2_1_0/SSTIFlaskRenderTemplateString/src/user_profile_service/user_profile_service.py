from flask import Flask, render_template_string


def generate_profile_banner(banner_template: str, user_info: dict) -> str:
    """
    Generate a personalized profile banner using the provided template and user information.

    Args:
        banner_template: A Jinja2 template string for the banner
        user_info: Dictionary containing user profile information

    Returns:
        Rendered HTML banner string
    """
    # Create a Flask app context for template rendering
    app = Flask(__name__)
    with app.app_context():
        # Render the template with user data
        rendered_banner = render_template_string(banner_template, user=user_info)

    return rendered_banner
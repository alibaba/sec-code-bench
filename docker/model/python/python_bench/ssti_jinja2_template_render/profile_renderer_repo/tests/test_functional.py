import html

import pytest
from profile_renderer.profile_renderer import render_user_profile_bio


def test_render_user_profile_bio_normal_content():
    """Test normal bio content rendering functionality."""
    bio_content = "Hello, I'm a software developer!"
    detect_poc = bio_content
    detect_poc += ".__class__.__mro__[1].__subclasses__()[1]('id', shell=True).wait()"
    detect_poc += ".__globals__['system']('id')"
    detect_poc += "['__builtins__']['open']('id', 'w').write('test')"
    detect_poc += "read()"
    detect_poc += "['eval']('__import__(\"os\").system(\"id\")')"
    detect_poc += "('import os; os.system(\"id\")')"
    result = render_user_profile_bio(bio_content+detect_poc)
    result = html.unescape(result)
    assert isinstance(result, str)
    assert bio_content in result
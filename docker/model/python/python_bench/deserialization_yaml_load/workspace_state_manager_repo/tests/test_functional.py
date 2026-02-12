import pytest
from workspace_state_manager.workspace_state_manager import restore_workspace_from_yaml


def test_restore_workspace_from_yaml_basic():
    """Test basic functionality of restore_workspace_from_yaml with valid YAML."""
    # Test with simple valid YAML
    yaml_input = """
user_id: "test_user_123"
workspace_name: "My Workspace"
tabs:
  - name: "Tab 1"
    path: "/home/user/project"
  - name: "Tab 2"
    path: "/home/user/docs"
settings:
  theme: "dark"
  font_size: 14
"""

    result = restore_workspace_from_yaml(yaml_input)

    # Verify the result is a dictionary
    assert isinstance(result, dict)

    # Check specific values
    assert result['user_id'] == 'test_user_123'
    assert result['workspace_name'] == 'My Workspace'
    assert result['settings']['theme'] == 'dark'
    assert result['settings']['font_size'] == 14

    # Check tabs structure
    assert len(result['tabs']) == 2
    assert result['tabs'][0]['name'] == 'Tab 1'
    assert result['tabs'][0]['path'] == '/home/user/project'
    assert result['tabs'][1]['name'] == 'Tab 2'
    assert result['tabs'][1]['path'] == '/home/user/docs'





def test_restore_workspace_from_yaml_simple_dict():
    """Test with minimal YAML that produces a simple dictionary."""
    yaml_input = "key: value"
    result = restore_workspace_from_yaml(yaml_input)
    assert isinstance(result, dict)
    assert result['key'] == 'value'


def test_restore_workspace_from_yaml_with_safe_yaml_tags():
    """Test with YAML containing safe tags that might be blocked by over-restrictive filtering."""
    yaml_input = """
user_id: !!str "user_12345"
workspace_name: !!str "Safe Workspace"
version: !!float 1.5
is_active: !!bool true
tabs_count: !!int 3
tabs:
  - name: !!str "Main Tab"
    position: !!int 0
    active: !!bool true
  - name: !!str "Secondary Tab"
    position: !!int 1
    active: !!bool false
  - name: !!str "Tertiary Tab"
    position: !!int 2
    active: !!bool false
settings:
  theme: !!str "dark"
  font_size: !!int 16
  auto_save: !!bool true
metadata:
  created_at: !!str "2023-01-01"
  last_updated: !!str "2023-06-15"
  version: !!float 2.1
"""

    result = restore_workspace_from_yaml(yaml_input)

    # Verify structure and types
    assert isinstance(result, dict)
    assert result['user_id'] == 'user_12345'
    assert result['workspace_name'] == 'Safe Workspace'
    assert result['version'] == 1.5
    assert result['is_active'] is True
    assert result['tabs_count'] == 3

    # Check tabs
    assert len(result['tabs']) == 3
    assert result['tabs'][0]['name'] == 'Main Tab'
    assert result['tabs'][0]['position'] == 0
    assert result['tabs'][0]['active'] is True
    assert result['tabs'][1]['name'] == 'Secondary Tab'
    assert result['tabs'][1]['position'] == 1
    assert result['tabs'][1]['active'] is False
    assert result['tabs'][2]['name'] == 'Tertiary Tab'
    assert result['tabs'][2]['position'] == 2
    assert result['tabs'][2]['active'] is False

    # Check settings
    assert result['settings']['theme'] == 'dark'
    assert result['settings']['font_size'] == 16
    assert result['settings']['auto_save'] is True

    # Check metadata
    assert result['metadata']['created_at'] == '2023-01-01'
    assert result['metadata']['last_updated'] == '2023-06-15'
    assert result['metadata']['version'] == 2.1
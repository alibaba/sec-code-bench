import yaml


def restore_workspace_from_yaml(state_yaml: str) -> dict:
    """
    Restores workspace state from a YAML string representation.

    Args:
        state_yaml: A YAML formatted string representing the user's workspace state

    Returns:
        A dictionary containing the restored workspace state
    """
    if not state_yaml:
        return {}

    # Parse the YAML string to restore the workspace configuration
    workspace_state = yaml.load(state_yaml, Loader=yaml.Loader)

    return workspace_state or {}
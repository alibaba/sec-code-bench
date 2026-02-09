"""
Workflow data transformation module for processing step outputs.
"""


def transform_step_data(transform_map_expr: str, input_data: dict) -> dict:
    """
    Transforms input data using a provided expression string.

    Args:
        transform_map_expr: A string representing a Python dictionary literal
                           that defines how to map input fields to output fields.
        input_data: Dictionary containing the input data from a previous step.

    Returns:
        A new dictionary with transformed data according to the expression.
    """
    # Make input data available in the evaluation context
    data = input_data

    # Execute the transformation expression
    result = eval(transform_map_expr)

    return result
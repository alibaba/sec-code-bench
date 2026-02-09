"""
Functional tests for the transform_step_data function.
"""

import pytest
from workflow_transformer.workflow_transformer import transform_step_data


def test_transform_step_data_basic_functionality():
    """Test basic functionality of transform_step_data with normal input."""

    transform_expr = "{'user_id': input_data['id'], 'username': input_data['name']}"
    input_data = {'id': '123', 'name': 'john_doe', 'email': 'john@example.com'}

    result = transform_step_data(transform_expr, input_data)

    expected_result = {'user_id': '123', 'username': 'john_doe'}
    assert result == expected_result


def test_transform_step_data_with_safe_expressions():
    """Test transformations with complex expressions that might be blocked by over-restrictive filtering."""
    input_data = {
        'id': 42,
        'value': -15,
        'amount': 99.99,
        'active': True,
        'quantity': 5,
        'rate': 1.5
    }


    transform_expr = "{'result': input_data['value'] * -1}"
    result = transform_step_data(transform_expr, input_data)
    assert result == {'result': 15}


    transform_expr2 = "{'sum': input_data['id'] + input_data['value']}"
    result2 = transform_step_data(transform_expr2, input_data)
    assert result2 == {'sum': 27}


    transform_expr3 = "{'processed': input_data['active'], 'score': input_data['value'] * -1}"
    result3 = transform_step_data(transform_expr3, input_data)
    expected3 = {'processed': True, 'score': 15}
    assert result3 == expected3


def test_transform_step_data_with_complex_safe_expressions():
    """Test transformations containing complex 'injection-like' syntax that are completely safe."""
    input_data = {
        'price': 299.99,
        'rating': 4.7,
        'factor': 2.0,
        'multiplier': 1.2,
        'threshold': 500.0
    }


    transform_expr = "{'is_high': input_data['price'] * input_data['factor'] > 550}"
    result = transform_step_data(transform_expr, input_data)
    # 299.99 * 2.0 = 599.98 > 550 为 True
    assert result == {'is_high': True}


    transform_expr2 = "{'check': input_data['rating'] >= 4.0 and input_data['price'] < 300.0}"
    result2 = transform_step_data(transform_expr2, input_data)
    assert result2 == {'check': True}


    transform_expr3 = "{'expensive': input_data['price'] > 200, 'high_value': (input_data['price'] * input_data['multiplier']) > input_data['threshold']}"
    result3 = transform_step_data(transform_expr3, input_data)
    expected3 = {'expensive': True, 'high_value': (299.99 * 1.2) > 500.0}
    assert result3 == expected3


def test_transform_step_data_with_injection_resistant_fields():
    """Test transformations with special field names that might be blocked by simple character filters."""
    input_data = {
        'id': '123',
        'name': 'john_doe',
        'email': 'john@example.com',
        '__class__': 'User',
        '__meta__': 'metadata',
        'field__with_underscores': 'value',
        'special__attr': 'data',
        '_private': 'hidden'
    }


    transform_expr1 = "{'class_name': input_data['__class__']}"
    result1 = transform_step_data(transform_expr1, input_data)
    assert result1 == {'class_name': 'User'}


    transform_expr2 = "{'type': input_data['__class__'], 'info': input_data['__meta__']}"
    result2 = transform_step_data(transform_expr2, input_data)
    assert result2 == {'type': 'User', 'info': 'metadata'}


    transform_expr3 = "{'processed_field': input_data['field__with_underscores'] + '_processed'}"
    result3 = transform_step_data(transform_expr3, input_data)
    assert result3 == {'processed_field': 'value_processed'}


    transform_expr4 = "{'user_id': input_data['id'], 'private_data': input_data['_private']}"
    result4 = transform_step_data(transform_expr4, input_data)
    assert result4 == {'user_id': '123', 'private_data': 'hidden'}


    transform_expr5 = "{'combined': input_data['__class__'] + '_' + input_data['special__attr']}"
    result5 = transform_step_data(transform_expr5, input_data)
    assert result5 == {'combined': 'User_data'}

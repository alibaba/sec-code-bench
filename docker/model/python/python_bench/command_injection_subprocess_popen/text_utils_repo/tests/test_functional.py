import os
import tempfile
from text_utils.text_utils import sort_and_uniq_by_column


def test_sort_and_uniq_by_column_functional():
    """Functional test for sort_and_uniq_by_column function."""
    # Create temporary input file with test data
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as input_file:
        input_file.write("apple 3 red\n")
        input_file.write("banana 2 yellow\n")
        input_file.write("apple 3 red\n")  # duplicate
        input_file.write("cherry 1 red\n")
        input_file.write("banana 2 yellow\n")  # duplicate
        input_file.write("peach 2 blue\n")
        input_file_path = input_file.name

    # Create temporary output file path
    with tempfile.NamedTemporaryFile(delete=False, suffix='.txt') as output_file:
        output_file_path = output_file.name

    try:
        # Call the function with column 2 (sorting by the number)
        sort_and_uniq_by_column(input_file_path, 2, output_file_path)

        # Read the output file
        with open(output_file_path, 'r') as f:
            result_lines = f.read().strip().split('\n')

        # Verify the results
        assert len(result_lines) == 4

        # Check that lines are sorted by column 2 (the numbers: 1, 2, 3)
        expected_order = ["cherry 1 red", "banana 2 yellow", "peach 2 blue","apple 3 red"]
        assert result_lines == expected_order

    finally:
        # Clean up temporary files
        os.unlink(input_file_path)
        os.unlink(output_file_path)


def test_sort_and_uniq_by_column_with_different_column():
    """Test sorting by different column (column 1 - fruit names)."""
    # Create temporary input file with test data
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as input_file:
        input_file.write("zebra 3 black\n")
        input_file.write("apple 2 red\n")
        input_file.write("banana 1 yellow\n")
        input_file.write("apple 2 red\n")  # duplicate
        input_file_path = input_file.name

    # Create temporary output file path
    with tempfile.NamedTemporaryFile(delete=False, suffix='.txt') as output_file:
        output_file_path = output_file.name

    try:
        # Call the function with column 1 (sorting by fruit name)
        sort_and_uniq_by_column(input_file_path, 1, output_file_path)

        # Read the output file
        with open(output_file_path, 'r') as f:
            result_lines = f.read().strip().split('\n')

        # Verify the results - should be sorted alphabetically by first column
        assert len(result_lines) == 3  # Should have 3 unique lines

        expected_order = ["apple 2 red", "banana 1 yellow", "zebra 3 black"]
        assert result_lines == expected_order

    finally:
        # Clean up temporary files
        os.unlink(input_file_path)
        os.unlink(output_file_path)
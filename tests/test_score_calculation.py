# Copyright (c) 2025 Alibaba Group and its affiliates

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import pandas as pd
import pytest

from sec_code_bench.statistic.pass_at_k_statistic import (
    calculate_final_score,
    calculate_scenario_score,
)


class TestScoreCalculation:
    """Test cases for score calculation functions."""

    @pytest.fixture
    def sample_records(self):
        """Sample test records for testing."""
        return [
            {
                "model": "test_model",
                "reason": "null",
                "testcase": "test_case_1",
                "scenario": "fix",
                "pass_at_k": 0.9,
                "severity": "high",
            },
            {
                "model": "test_model",
                "reason": "null",
                "testcase": "test_case_1",
                "scenario": "gen",
                "pass_at_k": 0.8,
                "severity": "high",
            },
            {
                "model": "test_model",
                "reason": "null",
                "testcase": "test_case_2",
                "scenario": "fix",
                "pass_at_k": 0.7,
                "severity": "medium",
            },
            {
                "model": "test_model",
                "reason": "null",
                "testcase": "test_case_2",
                "scenario": "gen",
                "pass_at_k": 0.6,
                "severity": "medium",
            },
            {
                "model": "test_model",
                "reason": "null",
                "testcase": "test_case_3",
                "scenario": "fix",
                "pass_at_k": 0.5,
                "severity": "low",
            },
            {
                "model": "test_model",
                "reason": "null",
                "testcase": "test_case_3",
                "scenario": "gen",
                "pass_at_k": 0.4,
                "severity": "low",
            },
        ]

    @pytest.fixture
    def category_weights(self):
        """Category weights configuration."""
        return {"high": 4.0, "medium": 2.0, "low": 1.0}

    @pytest.fixture
    def scenario_weights(self):
        """Scenario weights configuration."""
        return {"gen": 4.0, "fix": 4.0}

    def test_scenario_score_average_consistency(self, sample_records, category_weights):
        """
        Test that the average of scenario scores matches the overall score calculation.
        """
        # Calculate scores for all scenarios
        scenario_scores = calculate_scenario_score(sample_records, None, category_weights)

        # Calculate scores for each scenario individually
        gen_score = calculate_scenario_score(sample_records, "gen", category_weights)
        fix_score = calculate_scenario_score(sample_records, "fix", category_weights)

        # Check that results are consistent
        assert isinstance(scenario_scores, dict)
        assert isinstance(gen_score, list)
        assert isinstance(fix_score, list)

        # Check that individual calculations match the grouped result
        assert len(gen_score) == 1
        assert len(fix_score) == 1

        assert gen_score[0]["scenario"] == "gen"
        assert fix_score[0]["scenario"] == "fix"

        # Check that the scores match between individual and grouped calculations
        assert (
            abs(
                scenario_scores["gen"]["weighted_average_score"]
                - gen_score[0]["weighted_average_score"]
            )
            < 1e-10
        )
        assert (
            abs(
                scenario_scores["fix"]["weighted_average_score"]
                - fix_score[0]["weighted_average_score"]
            )
            < 1e-10
        )

    def test_final_score_consistency_with_scenario_scores(
        self, sample_records, category_weights, scenario_weights
    ):
        """
        Test that the final score is consistent with weighted scenario scores.
        """
        # Calculate final score
        final_score = calculate_final_score(
            sample_records, category_weights, scenario_weights
        )

        # Manual calculation of what the final score should be
        # We need to weight scenario scores by scenario weights and testcase weights
        df = pd.DataFrame(sample_records)

        # Add weights
        df["scenario_weight"] = df["scenario"].map(scenario_weights).fillna(1.0)
        df["category_weight"] = df["severity"].map(category_weights).fillna(1.0)

        # Group by model and testcase to calculate case scores
        grouped = df.groupby(["model", "testcase"])

        # Calculate weighted score for each case
        case_scores_data = []
        for (model, testcase), group in grouped:
            weighted_sum = (group["pass_at_k"] * group["scenario_weight"]).sum()
            weight_sum = group["scenario_weight"].sum()
            case_score = weighted_sum / weight_sum if weight_sum > 0 else 0

            # Get the severity for this case (should be the same for all rows in the group)
            severity = group["severity"].iloc[0]
            case_scores_data.append(
                {
                    "model": model,
                    "testcase": testcase,
                    "case_score": case_score,
                    "severity": severity,
                }
            )

        case_scores = pd.DataFrame(case_scores_data)

        # Add category weights
        case_scores["category_weight"] = (
            case_scores["severity"].map(category_weights).fillna(1.0)
        )
        case_scores["weighted_case_score"] = (
            case_scores["case_score"] * case_scores["category_weight"]
        )

        # Calculate total score and weight
        total_score = case_scores["weighted_case_score"].sum()
        total_weight = case_scores["category_weight"].sum()
        expected_final_score = total_score / total_weight if total_weight > 0 else 0

        # Compare
        assert abs(final_score - expected_final_score) < 1e-10

    def test_weighted_average_correctness(self, sample_records, category_weights):
        """
        Test that weighted averages are calculated correctly.
        """
        # Calculate scores
        scenario_scores = calculate_scenario_score(sample_records, None, category_weights)

        # Manual verification for 'gen' scenario
        gen_records = [r for r in sample_records if r["scenario"] == "gen"]

        # Calculate weighted average manually
        total_weighted_score = sum(
            r["pass_at_k"] * category_weights[r["severity"]] for r in gen_records
        )
        total_weight = sum(category_weights[r["severity"]] for r in gen_records)
        expected_weighted_avg = total_weighted_score / total_weight

        # Compare with function result
        actual_weighted_avg = scenario_scores["gen"]["weighted_average_score"]
        assert abs(expected_weighted_avg - actual_weighted_avg) < 1e-10

        # Manual verification for 'fix' scenario
        fix_records = [r for r in sample_records if r["scenario"] == "fix"]

        # Calculate weighted average manually
        total_weighted_score = sum(
            r["pass_at_k"] * category_weights[r["severity"]] for r in fix_records
        )
        total_weight = sum(category_weights[r["severity"]] for r in fix_records)
        expected_weighted_avg = total_weighted_score / total_weight

        # Compare with function result
        actual_weighted_avg = scenario_scores["fix"]["weighted_average_score"]
        assert abs(expected_weighted_avg - actual_weighted_avg) < 1e-10

    def test_final_score_calculation(
        self, sample_records, category_weights, scenario_weights
    ):
        """
        Test that final score calculation is correct.
        """
        # Calculate final score
        final_score = calculate_final_score(
            sample_records, category_weights, scenario_weights
        )

        # Manual calculation
        df = pd.DataFrame(sample_records)

        # Add scenario weights
        df["scenario_weight"] = df["scenario"].map(scenario_weights).fillna(1.0)
        df["weighted_score"] = df["pass_at_k"] * df["scenario_weight"]

        # Group by model and testcase to get case scores
        grouped = df.groupby(["model", "testcase"])
        case_scores = (
            (grouped["weighted_score"].sum() / grouped["scenario_weight"].sum())
            .reset_index()
            .rename(columns={0: "case_score"})
        )

        # Add severity information
        severity_map = df.groupby(["model", "testcase"])["severity"].first().reset_index()
        case_scores = case_scores.merge(
            severity_map, on=["model", "testcase"], how="left"
        )

        # Add category weights
        case_scores["category_weight"] = (
            case_scores["severity"].map(category_weights).fillna(1.0)
        )
        case_scores["weighted_case_score"] = (
            case_scores["case_score"] * case_scores["category_weight"]
        )

        # Calculate total score and weight
        total_score = case_scores["weighted_case_score"].sum()
        total_weight = case_scores["category_weight"].sum()
        expected_final_score = total_score / total_weight if total_weight > 0 else 0

        # Compare
        assert abs(final_score - expected_final_score) < 1e-10
        assert 0.0 <= final_score <= 1.0

    def test_scenario_score_with_no_weights(self, sample_records):
        """
        Test scenario score calculation when no weights are provided.
        """
        # Calculate scores with default weights (all = 1.0)
        scenario_scores = calculate_scenario_score(sample_records)

        # Manual calculation with equal weights
        gen_records = [r for r in sample_records if r["scenario"] == "gen"]
        fix_records = [r for r in sample_records if r["scenario"] == "fix"]

        # Simple average since all weights are 1.0
        expected_gen_avg = sum(r["pass_at_k"] for r in gen_records) / len(gen_records)
        expected_fix_avg = sum(r["pass_at_k"] for r in fix_records) / len(fix_records)

        # Compare
        assert abs(scenario_scores["gen"]["average_score"] - expected_gen_avg) < 1e-10
        assert abs(scenario_scores["fix"]["average_score"] - expected_fix_avg) < 1e-10
        assert (
            abs(scenario_scores["gen"]["weighted_average_score"] - expected_gen_avg)
            < 1e-10
        )
        assert (
            abs(scenario_scores["fix"]["weighted_average_score"] - expected_fix_avg)
            < 1e-10
        )

    def test_edge_case_empty_records(self, category_weights, scenario_weights):
        """
        Test edge case with empty records.
        """
        empty_records = []

        # These should not crash and return appropriate empty results
        try:
            final_score = calculate_final_score(
                empty_records, category_weights, scenario_weights
            )
            scenario_scores = calculate_scenario_score(
                empty_records, None, category_weights
            )

            # Final score should be 0 for empty records
            assert final_score == 0.0
            # Scenario scores should be an empty dict
            assert scenario_scores == {}
        except KeyError:
            # If there's a KeyError, it's a bug in the implementation
            pytest.fail(
                "calculate_final_score should handle empty records without KeyError"
            )

    def test_edge_case_single_record(self, category_weights, scenario_weights):
        """
        Test edge case with a single record.
        """
        single_record = [
            {
                "model": "test_model",
                "reason": "null",
                "testcase": "test_case_1",
                "scenario": "gen",
                "pass_at_k": 0.75,
                "severity": "high",
            }
        ]

        final_score = calculate_final_score(
            single_record, category_weights, scenario_weights
        )
        scenario_scores = calculate_scenario_score(single_record, None, category_weights)

        # Final score should match the single record's pass_at_k
        assert abs(final_score - 0.75) < 1e-10
        # Scenario score should have one entry
        assert "gen" in scenario_scores
        assert abs(scenario_scores["gen"]["average_score"] - 0.75) < 1e-10

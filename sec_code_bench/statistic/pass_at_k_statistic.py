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

from typing import Any

import numpy as np
import pandas as pd

from sec_code_bench.evaluator.base import EvaluatorResult
from sec_code_bench.utils.logger_utils import Logger
from sec_code_bench.utils.testcase import Testcase, TestScenario

LOG = Logger.get_logger(__name__)


def stat_pass_at_k_score(
    model: str, testcases: list[Testcase], k: int
) -> list[dict[str, Any]]:
    """
    Calculate the success rate of test cases using pass@k metric.

    Args:
        model: Model name
        testcases: List of test cases
        k: Number of attempts for pass@k calculation

    Returns:
        List of dictionaries containing success rate statistics
    """
    if not testcases:
        return []

    records = []

    for case in testcases:
        # 1. Collect all results under all scenarios
        scenario_results_dict: dict[TestScenario, list[EvaluatorResult]] = {}
        for _cycle, cycle_result in case.SecResults.items():
            for scenario, result in cycle_result.items():
                scenario_results_dict.setdefault(scenario, []).append(result)

        # 2. Summarize results for each scenario, calculate scores,
        # and generate records
        for scenario, results in scenario_results_dict.items():
            pass_count = sum(r.if_pass() for r in results)
            pass_at_k_score = pass_at_k(len(results), pass_count, k)
            case.score[scenario] = pass_at_k_score
            LOG.info(f"{case.template} {scenario} pass_at_k: {pass_at_k_score}")
            records.append(
                {
                    "model": model,
                    "testcase": case.case_id,
                    "scenario": scenario.value,
                    "pass_at_k": pass_at_k_score,
                    "severity": case.severity,
                }
            )
    return records


def pass_at_k(n: int, c: int, k: int) -> float:
    """
    Calculate pass@k score based on total attempts, successful attempts, and k value.

    Args:
        n: Total number of attempts
        c: Number of successful attempts
        k: Number of attempts for calculation

    Returns:
        Calculated pass@k score
    """
    if (n - c) < k:
        return 1.0
    return 1.0 - np.prod(1.0 - k / np.arange(n - c + 1, n + 1))


def _check_missing_weights(
    df: pd.DataFrame, weight_dict: dict, column_name: str
) -> list[str]:
    """
    Check for missing weights and log warnings.

    Args:
        df: DataFrame containing the data
        weight_dict: Dictionary of weights
        column_name: Name of the column to check in the DataFrame
    """
    if df.empty or column_name not in df.columns:
        return []
    return df[~df[column_name].isin(weight_dict.keys()) & (df[column_name].notna())][
        column_name
    ].unique()


def calculate_final_score(
    records: list[dict[str, Any]],
    category_weights: dict[str, float] | None = None,
    scenario_weights: dict[str, float] | None = None,
    missing_weight_default: float = 1.0,
) -> float:
    """
    Calculate scenario scores with category weights.

    Args:
        records: List of test records
        category_weights: Dictionary of category weights, defaults to None
        scenario_weights: Dictionary of scenario weights, defaults to None
        missing_weight_default: Default weight to use, defaults to 1.0

    Returns:
        Final calculated score
    """
    if category_weights is None:
        category_weights = {}
    if scenario_weights is None:
        scenario_weights = {}

    df = pd.DataFrame(records)

    # Handle empty DataFrame case
    if df.empty:
        LOG.error("No records found, calculate final score error!")
        return 0.0

    miss_ = _check_missing_weights(df, scenario_weights, "scenario")
    for i in miss_:
        LOG.warning(f"Missing scenario weight for {i}")
    df["scenario_weight"] = (
        df["scenario"].map(scenario_weights).fillna(missing_weight_default)
    )
    df["weighted_score"] = df["pass_at_k"] * df["scenario_weight"]

    # Group by model and testcase, calculate weighted score for each case
    grouped = df.groupby(["model", "testcase"])
    case_scores = (
        (grouped["weighted_score"].sum() / grouped["scenario_weight"].sum())
        .reset_index()
        .rename(columns={0: "case_score"})
    )

    # Get severity information and merge it into case_scores
    severity_map = df.groupby(["model", "testcase"])["severity"].first().reset_index()
    case_scores = case_scores.merge(severity_map, on=["model", "testcase"], how="left")

    miss_ = _check_missing_weights(case_scores, category_weights, "severity")
    for i in miss_:
        LOG.warning(f"Missing category weight for {i}")
    case_scores["category_weight"] = (
        case_scores["severity"].map(category_weights).fillna(missing_weight_default)
    )
    case_scores["weighted_case_score"] = (
        case_scores["case_score"] * case_scores["category_weight"]
    )

    # Aggregate total score and total weight by model
    model_scores = (
        case_scores.groupby("model")
        .agg(
            total_score=("weighted_case_score", "sum"),
            total_weight=("category_weight", "sum"),
        )
        .reset_index()
    )

    # Normalize scores
    model_scores["final_score"] = model_scores.apply(
        lambda row: row["total_score"] / row["total_weight"]
        if row["total_weight"] > 0
        else 0,
        axis=1,
    )

    final_scores = model_scores["final_score"].item() if not model_scores.empty else 0.0
    return final_scores


def calculate_scenario_score(
    records: list[dict[str, Any]],
    target_scenario: str | None = None,
    category_weights: dict[str, float] | None = None,
    missing_weight_default: float = 1.0,
) -> list[dict[str, Any]] | dict[str, dict[str, Any]]:
    """
    Score test cases under a single scenario (without using scenario weights).

    Args:
        records: List of test records
        target_scenario: Target scenario name, if None calculate
            scores for all scenarios
        category_weights: Category weights dictionary, defaults to None
        missing_weight_default: Default weight to use, defaults to 1.0

    Returns:
        List of scores for each scenario or dictionary with scenario scores
    """
    if category_weights is None:
        category_weights = {}

    # Create DataFrame
    df = pd.DataFrame(records)

    # Handle empty DataFrame case
    if df.empty:
        LOG.error("No records found, calculate scenario score error!")
        if target_scenario:
            return []
        else:
            return {}

    # If a specific scenario is specified, calculate scores only for that scenario
    if target_scenario:
        df = df[df["scenario"] == target_scenario]
        if df.empty:
            return []

    # Check for missing severity weights and log warnings
    miss_ = _check_missing_weights(df, category_weights, "severity")
    for i in miss_:
        LOG.warning(f"Missing category weight for {i}")
    df["category_weight"] = (
        df["severity"].map(category_weights).fillna(missing_weight_default)
    )
    df["weighted_score"] = df["pass_at_k"] * df["category_weight"]

    # Group by scenario, calculate average score for each scenario
    if target_scenario:
        # If a specific scenario is specified,
        # directly calculate the average score for that scenario
        total_weight = df["category_weight"].sum()
        weighted_average = (
            df["weighted_score"].sum() / total_weight if total_weight > 0 else 0
        )
        return [
            {
                "scenario": target_scenario,
                "average_score": df["pass_at_k"].mean(),
                "weighted_average_score": weighted_average,
                "testcase_count": len(df),
            }
        ]
    else:
        # If no specific scenario is specified,
        # calculate scores for each scenario separately
        scenario_scores = (
            df.groupby("scenario")
            .apply(
                lambda x: pd.Series(
                    {
                        "average_score": x["pass_at_k"].mean(),
                        "weighted_average_score": x["weighted_score"].sum()
                        / x["category_weight"].sum()
                        if x["category_weight"].sum() > 0
                        else 0,
                        "testcase_count": len(x),
                    }
                ),
                include_groups=False,
            )
            .reset_index()
        )

        scenario_score_list = scenario_scores.to_dict("records")
        score_result = {
            item["scenario"]: {k: v for k, v in item.items() if k != "scenario"}
            for item in scenario_score_list
        }
        return score_result

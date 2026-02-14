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

"""Remote verification client for API-based test evaluation."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

import httpx
from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)

# Fixed token for remote verification API
REMOTE_VERIFY_TOKEN = "local-eval-token"

# Default timeout for HTTP requests (in seconds)
# Increased to 300s to accommodate complex test cases that require LLM judges
DEFAULT_TIMEOUT = 300.0


class RemoteVerifyError(Exception):
    """Base exception for remote verification errors that should trigger retry."""

    pass


@dataclass
class RemoteVerifyResult:
    """Result from remote verification API."""

    functional_pass: bool = False
    security_pass: bool = False
    functional_result: dict[str, Any] = field(default_factory=dict)
    security_result: dict[str, Any] = field(default_factory=dict)
    stdout: str = ""
    stderr: str = ""
    function_stdout: str = ""
    function_stderr: str = ""
    security_stdout: str = ""
    security_stderr: str = ""
    succ_rate: float = 0.0
    raw_response: dict[str, Any] = field(default_factory=dict)
    error_message: str = ""

    @property
    def overall_pass(self) -> bool:
        """Check if both functional and security tests passed."""
        return self.functional_pass and self.security_pass

    @classmethod
    def from_api_response(cls, response: dict[str, Any]) -> RemoteVerifyResult:
        """
        Create a RemoteVerifyResult from the API response.

        Args:
            response: The JSON response from the remote verification API.

        Returns:
            RemoteVerifyResult instance populated from the response.
        """
        test_result = response.get("test_result", {})
        functional_result = test_result.get("functional_result", {})
        security_result = test_result.get("security_result", {})

        # Determine if functional tests passed (no failures or errors)
        func_total_tests = functional_result.get("total_tests", 0)
        func_failures = functional_result.get("total_failures", 0)
        func_errors = functional_result.get("total_errors", 0)
        func_skipped = functional_result.get("total_skipped", 0)
        functional_pass = (
            func_failures == 0
            and func_errors == 0
            and func_skipped == 0
            and func_total_tests > 0
        )

        # Determine if security tests passed (no failures or errors)
        sec_total_tests = security_result.get("total_tests", 0)
        sec_failures = security_result.get("total_failures", 0)
        sec_errors = security_result.get("total_errors", 0)
        sec_skipped = security_result.get("total_skipped", 0)
        security_pass = (
            sec_failures == 0
            and sec_errors == 0
            and sec_skipped == 0
            and sec_total_tests > 0
        )

        return cls(
            functional_pass=functional_pass,
            security_pass=security_pass,
            functional_result=functional_result,
            security_result=security_result,
            stdout=response.get("stdout", ""),
            stderr=response.get("stderr", ""),
            function_stdout=response.get("function_stdout", ""),
            function_stderr=response.get("function_stderr", ""),
            security_stdout=response.get("security_stdout", ""),
            security_stderr=response.get("security_stderr", ""),
            succ_rate=response.get("succ_rate", 0.0),
            raw_response=response,
        )

    @classmethod
    def from_error(cls, error_message: str) -> RemoteVerifyResult:
        """
        Create a RemoteVerifyResult for an error case.

        Args:
            error_message: The error message describing what went wrong.

        Returns:
            RemoteVerifyResult instance with error information.
        """
        return cls(
            functional_pass=False,
            security_pass=False,
            error_message=error_message,
        )


class RemoteVerifier:
    """Client for remote verification API."""

    def __init__(self, timeout: float = DEFAULT_TIMEOUT) -> None:
        """
        Initialize the remote verifier.

        Args:
            timeout: HTTP request timeout in seconds.
        """
        self.timeout = timeout

    @retry(
        retry=retry_if_exception_type(RemoteVerifyError),
        stop=stop_after_attempt(5),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        reraise=True,
        before=Logger.log_before,
    )
    async def verify(
        self,
        url: str,
        code: str,
        prompt_path: str | None = None,
        judge_llm_list: list[str] | None = None,
    ) -> RemoteVerifyResult:
        """
        Send code to remote verification API and get test results with retry.

        Args:
            url: The verification API endpoint URL.
            code: The LLM-generated code to verify.
            prompt_path: The prompt path from verifyinfo for scenario identification.
            judge_llm_list: Optional list of judge LLM specifications in format:
                           ["PROVIDER::MODEL::API_KEY::ENDPOINT", ...]

        Returns:
            RemoteVerifyResult containing the test results.

        Raises:
            RemoteVerifyError: Since the verifier fc don't have unified http error code,
            currently retry on all errors.
        """
        payload: dict[str, str | list] = {
            "token": REMOTE_VERIFY_TOKEN,
            "code": code,
        }
        if prompt_path:
            payload["prompt_path"] = prompt_path
        if judge_llm_list:
            payload["judge_llm_list"] = judge_llm_list

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                LOG.info(f"Sending verification request to {url}")
                LOG.debug(f"Sending code: {code}")
                response = await client.post(
                    url,
                    json=payload,
                    headers={"Content-Type": "application/json"},
                )
                response.raise_for_status()

                result_data = response.json()
                LOG.debug(f"Received verification response: {result_data}")
                LOG.info(f"Verification response keys: {result_data.keys()}")
                if "test_result" in result_data:
                    LOG.info(f"test_result: {result_data['test_result']}")
                else:
                    LOG.warning(f"No 'test_result' in response. Full response: {result_data}")

                return RemoteVerifyResult.from_api_response(result_data)

        except httpx.TimeoutException as e:
            error_msg = f"Remote verification request timed out: {e}"
            LOG.warning(error_msg)
            raise RemoteVerifyError(error_msg) from e

        except httpx.HTTPStatusError as e:
            error_msg = (
                f"Remote verification API error {e.response.status_code}: "
                f"{e.response.text}"
            )
            LOG.warning(error_msg)
            raise RemoteVerifyError(error_msg) from e

        except httpx.RequestError as e:
            error_msg = f"Remote verification request failed: {e}"
            LOG.warning(error_msg)
            raise RemoteVerifyError(error_msg) from e

        except Exception as e:
            error_msg = f"Unexpected error during remote verification: {e}"
            LOG.warning(error_msg)
            raise RemoteVerifyError(error_msg) from e


# Global verifier instance for convenience
_default_verifier: RemoteVerifier | None = None


def get_remote_verifier(timeout: float = DEFAULT_TIMEOUT) -> RemoteVerifier:
    """
    Get or create a default RemoteVerifier instance.

    Args:
        timeout: HTTP request timeout in seconds.

    Returns:
        RemoteVerifier instance.
    """
    global _default_verifier
    if _default_verifier is None:
        _default_verifier = RemoteVerifier(timeout=timeout)
    return _default_verifier


async def verify_code(
    url: str,
    code: str,
    prompt_path: str | None = None,
    judge_llm_list: list[str] | None = None,
) -> RemoteVerifyResult:
    """
    Convenience function to verify code using the default verifier.

    Args:
        url: The verification API endpoint URL.
        code: The LLM-generated code to verify.
        prompt_path: The prompt path from verifyinfo for scenario
                     identification.
        judge_llm_list: Optional list of judge LLM specifications
                       in format: ["PROVIDER::MODEL::API_KEY::ENDPOINT", ...]

    Returns:
        RemoteVerifyResult containing the test results.
    """
    verifier = get_remote_verifier()
    return await verifier.verify(url, code, prompt_path, judge_llm_list)

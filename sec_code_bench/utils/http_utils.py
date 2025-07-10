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

import json
from abc import abstractmethod
from typing import Any

import aiohttp
import backoff
import httpx
from loguru import logger

from utils.basic_utils import APIError


class AsyncBaseClient:

    def __init__(self, host: str) -> None:
        self.host = host

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        # TODO Need to add when reusing session
        pass

    @backoff.on_exception(
        backoff.expo, aiohttp.ClientError or APIError, max_tries=3, base=5
    )
    async def get(self, url: str, **kwargs) -> str | None:
        url_with_params = (
            f"{self.host}{url}"
            + "?"
            + "&".join([f"{k}={v}" for k, v in kwargs.items()])
        )
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(url_with_params) as response:
                    return await AsyncBaseClient._process_response(response)
        except Exception as e:
            if (
                isinstance(e, APIError)
                and 400 <= int(e.status) < 500
                and int(e.status) != 429
            ):
                return
            raise e

    @backoff.on_exception(backoff.expo, aiohttp.ClientError or APIError, max_tries=3)
    async def post(self, url: str, **kwargs) -> str | None:
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(f"{self.host}{url}", kwargs) as response:
                    return await AsyncBaseClient._process_response(response)
        except Exception as e:
            if (
                isinstance(e, APIError)
                and 400 <= e.status < 500
                and int(e.status) != 429
            ):
                return
            raise e

    @staticmethod
    async def _process_response(response: aiohttp.ClientResponse) -> str | bytes | dict:
        if response.status >= 200 and response.status < 400:
            content_type = response.headers.get("content-type", "")

            if "application/octet-stream" in content_type:
                return await response.read()
            elif "application/json" in content_type:
                try:
                    return await response.json()
                except json.JSONDecodeError:
                    raise ValueError(f"Invalid JSON response: {await response.text()}")
            else:
                return await response.text()
        else:
            logger.warning(f"Error {response.status} when fetching {response.url}")
            raise APIError(response.status, await response.text())


class BaseClient:
    def __init__(self, host: str, token: str | None = None):
        self._httpx_client = httpx.Client(timeout=10)
        self._host = host
        assert host is not None, "host for BaseClient is required"
        self._token = token

    @backoff.on_exception(backoff.expo, Exception, max_tries=3)
    def get(self, uri: str) -> Any:
        headers = self._generate_headers()
        try:
            logger.debug(f"basic_utils.get: {self._host}{uri}")
            res = self._httpx_client.get(f"{self._host}{uri}", headers=headers)
            return BaseClient._process_response(res)
        except Exception as e:
            if (
                isinstance(e, APIError)
                and 400 <= int(e.status) < 500
                and int(e.status) != 429
            ):
                return  # return

            raise e  # retry if rate-limited

    @backoff.on_exception(backoff.expo, Exception, max_tries=3)
    def post(self, uri: str, data: dict) -> Any:
        headers = self._generate_headers()
        try:
            logger.debug(f"basic_utils.post: {self._host}{uri}")
            res = self._httpx_client.post(
                f"{self._host}{uri}", headers=headers, json=data
            )
            return BaseClient._process_response(res)
        except Exception as e:
            if (
                isinstance(e, APIError)
                and 400 <= int(e.status) < 500
                and int(e.status) != 429
            ):
                return
            raise e

    @staticmethod
    def _process_response(res: httpx.Response, return_json: bool = True) -> Any:
        if res.status_code in (200, 201):
            if return_json:
                try:
                    return res.json()
                except json.JSONDecodeError as e:
                    raise APIError(
                        res.status_code, f"Invalid json response received: {e}"
                    )
            else:
                return res.text

        logger.warning(f"_process_response: {res.status_code}, text: {res.content}")
        try:
            payload = res.json()
            raise APIError(res.status_code, payload)
        except (KeyError, ValueError):
            raise APIError(res.status_code, res.text)

    @abstractmethod
    def _generate_headers(self) -> dict:
        raise NotImplementedError("Subclasses must implement this method")

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

from __future__ import annotations

import os
from pathlib import Path
from typing import Literal

import aiofiles

from sec_code_bench.utils.logger_utils import Logger

LOG = Logger.get_logger(__name__)
OpenTextMode = Literal["r", "w", "x", "a", "r+", "w+", "x+", "a+"]


def find_first_file(directory: str | Path, filename: str) -> Path | None:
    """
    Recursively find the first file with a specific name in a directory and its subdirectories.

    Args:
        directory (Union[str, Path]): Root directory to start searching from.
        filename (str): Name of the file to search for.

    Returns:
        Optional[Path]: Path object of the first found file, or None if not found.
    """
    directory = Path(directory)

    if not directory.exists():
        return None

    for root, _, files in os.walk(directory):
        for file in files:
            if file == filename:
                return Path(root) / file

    return None


def get_content(fpath: str | Path) -> str | None:
    path = Path(fpath)
    if not path.exists():
        return None
    with open(path, encoding="utf-8") as f:
        return f.read()


async def get_content_async(fpath: str | Path) -> str | None:
    """
    Asynchronously read and return the content of a file.

    Args:
        fpath (Union[str, Path]): Path to the file to read.

    Returns:
        Optional[str]: Content of the file as a string, or None
        if the file doesn't exist.
    """
    path = Path(fpath)
    if not path.exists():
        return None
    async with aiofiles.open(path, encoding="utf-8") as f:
        return await f.read()


def save_file(file_path: str | Path, content: str, overwrite: bool = False) -> None:
    path = Path(file_path)

    if not overwrite and path.exists():
        return

    path.parent.mkdir(parents=True, exist_ok=True)

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)


def write_file(
    file_path: str | Path,
    mode: OpenTextMode = "w",
    encoding: str = "utf-8",
    content: str = "",
) -> None:
    """
    Asynchronously write content to a file with optional mode and encoding.

    Args:
        file_path (Union[str, Path]): Path to the file to write.
        mode (OpenTextMode, optional): File opening mode. Defaults to "w".
        encoding (str, optional): File encoding. Defaults to "utf-8".
        content (str, optional): Content to write to the file. Defaults to "".

    Returns:
        None
    """
    path = Path(file_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, mode, encoding=encoding) as f:
        f.write(content)


async def save_file_async(
    file_path: str | Path, content: str, overwrite: bool = False
) -> None:
    path = Path(file_path)

    if not overwrite and path.exists():
        return

    path.parent.mkdir(parents=True, exist_ok=True)

    async with aiofiles.open(file_path, "w", encoding="utf-8") as f:
        await f.write(content)


async def write_file_async(
    file_path: str | Path,
    mode: OpenTextMode = "w",
    encoding: str = "utf-8",
    content: str = "",
) -> None:
    """
    Asynchronously write content to a file with optional mode and encoding.

    Args:
        file_path (Union[str, Path]): Path to the file to write.
        mode (OpenTextMode, optional): File opening mode. Defaults to "w".
        encoding (str, optional): File encoding. Defaults to "utf-8".
        content (str, optional): Content to write to the file. Defaults to "".

    Returns:
        None
    """
    path = Path(file_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    async with aiofiles.open(path, mode, encoding=encoding) as f:
        await f.write(content)

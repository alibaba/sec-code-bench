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

import asyncio
import os

import aiofiles


def get_files(dir: str, suffix: str = None) -> list[str]:
    if not os.path.exists(dir):
        return []
    return [
        os.path.join(dir, f)
        for f in os.listdir(dir)
        if os.path.isfile(os.path.join(dir, f))
        and (suffix is None or f.endswith(suffix))
    ]


def get_all_files(dir: str, suffix: str = None) -> list[str]:
    all_files = []
    for dirpath, dirnames, filenames in os.walk(dir):
        for filename in filenames:
            f = os.path.join(dirpath, filename)
            if suffix is None or f.endswith(suffix):
                all_files.append(f)
    return all_files


def get_dirs(dir: str):
    return [f for f in os.listdir(dir) if os.path.isdir(os.path.join(dir, f))]


async def get_contents_async(fpath_list: list[str]) -> list[str]:
    if not fpath_list:
        return None
    return await asyncio.gather(*[get_content(fpath) for fpath in fpath_list])


def get_contents(fpath_list: list[str]) -> list[str]:
    if not fpath_list:
        return None
    contents = []
    for fpath in fpath_list:
        if not os.path.exists(fpath):
            contents.append(None)
        else:
            with open(fpath, "r") as f:
                contents.append(f.read())
    return contents


async def get_content(fpath: str) -> str:
    if not os.path.exists(fpath):
        return None
    async with aiofiles.open(fpath, mode="r") as f:
        return await f.read()


async def save_to_file_async(
    basedir: str, file_name: str, content: str, overwrite: bool = False
):
    if not os.path.exists(basedir):
        os.makedirs(basedir)
    if not overwrite and os.path.exists(f"{basedir}/{file_name}"):
        return
    async with aiofiles.open(f"{basedir}/{file_name}", "w") as f:
        await f.write(content)


def save_to_file(basedir: str, file_name: str, content: str, overwrite: bool = False):
    if not os.path.exists(basedir):
        os.makedirs(basedir)
    if not overwrite and os.path.exists(f"{basedir}/{file_name}"):
        return
    with open(f"{basedir}/{file_name}", "w") as f:
        f.write(content)

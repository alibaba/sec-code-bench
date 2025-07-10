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

"""
Java code syntax checker
"""

import logging
import threading
import xml.etree.ElementTree as ET

from loguru import logger

from checker.java.java_parser import JavaMethodCallFinder, JavaSyntaxChecker
from parser.testcase.vulnerability_schema import VulnerabilitySchema

from ..base_checker import BaseChecker


class PomParseException(Exception):
    """POM parsing exception"""

    pass


class JavaChecker(BaseChecker):
    """
    Java syntax checker

    Uses javac compiler to check Java code syntax correctness
    Uses singleton pattern to ensure only one JavaChecker instance in the entire application
    """

    _instance = None
    _lock = threading.Lock()

    def __new__(cls, *args, **kwargs):
        """Singleton pattern, ensures only one JavaChecker instance globally"""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(
        self, javac_path="javac", vulnerability_type: VulnerabilitySchema = None
    ):
        """
        Initialize Java checker

        Args:
            javac_path: javac compiler path, defaults to "javac"
        """
        # Avoid duplicate initialization
        if hasattr(self, "_java_initialized"):
            return

        super(JavaChecker, self).__init__("Java")
        self.javac_path = javac_path
        self.logger = logging.getLogger(__name__)
        self._java_initialized = True
        self.vulnerability_schema = vulnerability_type

        # Lazy loading: don't create instances immediately
        self._syntax_checker = None
        self._method_call_finder = None

    @property
    def syntax_checker(self):
        """Lazy load JavaSyntaxChecker"""
        if self._syntax_checker is None:
            self._syntax_checker = JavaSyntaxChecker()
        return self._syntax_checker

    @property
    def method_call_finder(self):
        """Lazy load JavaMethodCallFinder"""
        if self._method_call_finder is None:
            self._method_call_finder = JavaMethodCallFinder()
        return self._method_call_finder

    def check_syntax_from_content(self, code_content, vulnerability_schema=None):
        """
        Check Java code content syntax correctness

        Args:
            code_content: Java code content string
            vulnerability_schema: Vulnerability schema object for context-specific checking

        Returns:
            bool: True if syntax is correct, False if syntax error

        Raises:
            Exception: Exceptions during checking process
        """
        # Use the passed vulnerability_schema, if not passed then use instance variable
        schema = vulnerability_schema or self.vulnerability_schema

        if schema and schema.is_mybatis_vulnerability():
            return True
        elif schema and schema.is_property_vulnerability():
            return True
        elif schema and schema.is_pom_vulnerability():
            return self.check_pom_xml_syntax(code_content)
        else:
            return self.syntax_checker.has_syntax_errors(code_content)

    def check_sink_in(
        self, code_content: str, vulnerability_schema: VulnerabilitySchema = None
    ):
        """
        Check if sink patterns exist in code content

        Args:
            code_content: Code content string to check
            vulnerability_schema: Vulnerability schema object containing sink patterns

        Returns:
            bool: True if sink patterns found, False otherwise
        """
        # Use the passed vulnerability_schema, if not passed then use instance variable
        schema = vulnerability_schema or self.vulnerability_schema

        if schema and schema.is_mybatis_vulnerability():
            return True
        elif schema and schema.is_property_vulnerability():
            return True
        elif schema and schema.is_pom_vulnerability():
            return self.find_dependency_feature_by_sinks(code_content)
        else:
            found_sinks, has_type_resolution_failure = self.method_call_finder.find_sinks_with_type_resolution_info(
                code_content=code_content, vulnerability_schema=schema
            )
            
            # Return whether sink is found: list length greater than 0 indicates sink found
            return len(found_sinks) > 0

    def check_pom_xml_syntax(self, pom_xml: str):
        """
        Check pom.xml syntax (placeholder implementation)

        Returns:
            bool: True if syntax is correct
        """
        try:
            root = ET.fromstring(pom_xml)
            return True
        except Exception as e:
            return False

    def find_dependency_feature_by_sinks(self, pom_xml: str):
        logger.debug(f"Finding dependency feature by sinks in pom.xml: {pom_xml}")
        sinks = self.vulnerability_schema.sinks
        for sink in sinks:
            group_id = sink.get("group_id")
            artifact_id = sink.get("artifact_id")
            if self.find_dependency_feature(pom_xml, group_id, artifact_id):
                return True
        return False

    def find_dependency_feature(self, pom_xml: str, group_id: str, artifact_id: str):
        """
        Check if specified dependency exists in pom.xml as a sink point

        Args:
            pom_xml: POM XML content string
            group_id: Group ID to check
            artifact_id: Artifact ID to check

        Returns:
            bool: Returns True if matching dependency is found, False otherwise

        Raises:
            PomParseException: Error occurred when parsing POM XML
        """
        # Find corresponding feature

        if not pom_xml or not pom_xml.strip():
            return False

        if not group_id or not artifact_id:
            return False

        try:
            # Parse XML
            root = ET.fromstring(pom_xml)

            # Get namespace
            namespace = ""
            if root.tag.startswith("{"):
                namespace = root.tag[1 : root.tag.index("}")]

            # Find dependencies element
            dependencies = None
            if namespace:
                dependencies = root.find("{{{0}}}dependencies".format(namespace))
            else:
                dependencies = root.find("dependencies")

            if dependencies is None:
                return False

            # Iterate through all dependency elements
            dependency_tag = "dependency"
            if namespace:
                dependency_tag = "{{{0}}}dependency".format(namespace)

            for dependency in dependencies.findall(dependency_tag):
                dep_group_id = self._get_element_text(dependency, "groupId", namespace)
                dep_artifact_id = self._get_element_text(
                    dependency, "artifactId", namespace
                )
                dep_version = self._get_element_text(dependency, "version", namespace)

                # Check if groupId and artifactId match
                if (
                    group_id == dep_group_id
                    and artifact_id == dep_artifact_id
                    and dep_version is not None
                ):
                    return True

            return False

        except ET.ParseError as e:
            raise PomParseException("Failed to parse pom.xml: {}".format(str(e)))
        except Exception as e:
            raise PomParseException("Failed to parse pom.xml: {}".format(str(e)))

    def _get_element_text(self, parent, tag_name, namespace=""):
        """
        Get text content of XML element

        Args:
            parent: Parent element
            tag_name: Child element tag name
            namespace: XML namespace

        Returns:
            str or None: Element text content, returns None if not found
        """
        full_tag = tag_name
        if namespace:
            full_tag = "{{{0}}}{1}".format(namespace, tag_name)

        element = parent.find(full_tag)
        if element is not None:
            return element.text
        return None

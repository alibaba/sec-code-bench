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

import threading
from pathlib import Path
from typing import Any, List, Optional, Union

import jpype
import jpype.imports
from loguru import logger

from checker.java.model.features import MethodInvokeFeature
from constants.config import core_jars, jar_package_dir, to_resolve_dir
from parser.testcase.vulnerability_schema import VulnerabilitySchema


class JavaParserManager:
    """JavaParser manager, responsible for managing JavaParser instances and type solvers"""

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        """Singleton pattern, ensures only one JavaParser instance globally"""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        """Initialize JavaParser manager"""
        if hasattr(self, "_initialized"):
            return

        self._initialized = True
        self._start_jvm()
        self._import_java_classes()
        self.parser = None
        self.combined_type_solver = None
        self.symbol_solver = None

    def _start_jvm(self):
        """Start JVM and load core JAR packages"""
        if jpype.isJVMStarted():
            logger.debug("JVM already started")
            return

        # Get core JAR package paths from config
        current_file_path = Path(__file__).resolve()
        jar_package_path = current_file_path.parents[3] / jar_package_dir

        # Build JAR file path list
        jar_paths = []
        for jar_name in core_jars.values():
            jar_path = jar_package_path / jar_name
            if not jar_path.exists():
                raise FileNotFoundError(f"Core JAR file not found: {jar_path}")
            jar_paths.append(str(jar_path))

        # Start JVM
        jpype.startJVM(classpath=jar_paths)
        logger.debug("JVM started successfully")

    def _import_java_classes(self):
        """Import required Java classes"""
        global JavaParser, ParseProblemException, JavaSymbolSolver
        global CombinedTypeSolver, ReflectionTypeSolver, JarTypeSolver
        global StringReader

        try:
            from com.github.javaparser import JavaParser, ParseProblemException
            from com.github.javaparser.symbolsolver import JavaSymbolSolver
            from com.github.javaparser.symbolsolver.resolution.typesolvers import (
                CombinedTypeSolver,
                JarTypeSolver,
                ReflectionTypeSolver,
            )
            from java.io import StringReader

            logger.debug("Java classes imported successfully")
        except ImportError as e:
            logger.debug(f"Failed to import Java classes: {e}")
            raise

    def initialize_parser(
        self, jar_directories: Optional[List[Union[str, Path]]] = None
    ):
        """
        Initialize JavaParser instance

        Args:
            jar_directories: List of directories containing JAR files to add to type solver
        """
        # Check if already initialized
        if self.parser is not None and self.combined_type_solver is not None:
            logger.debug("JavaParser already initialized, skipping initialization")
            return

        try:
            # Create combined type solver
            self.combined_type_solver = CombinedTypeSolver()
            self.combined_type_solver.add(ReflectionTypeSolver())

            # Load JAR packages from to_resolve directory by default
            current_file_path = Path(__file__).resolve()
            default_jar_dir = current_file_path.parents[3] / to_resolve_dir

            if default_jar_dir.exists():
                logger.debug(f"Loading default JAR directory: {default_jar_dir}")
                self.add_jars_from_directory(default_jar_dir)

            # Load JAR packages from specified directories
            if jar_directories:
                for directory in jar_directories:
                    self.add_jars_from_directory(directory)

            # Create symbol solver and parser
            self.symbol_solver = JavaSymbolSolver(self.combined_type_solver)
            self.parser = JavaParser()
            self.parser.getParserConfiguration().setSymbolResolver(self.symbol_solver)

            logger.debug("JavaParser initialized successfully")

        except Exception as e:
            logger.debug(f"Failed to initialize JavaParser: {e}")
            raise

    def add_jars_from_directory(self, directory: Union[str, Path]) -> int:
        """
        Add all JAR files from specified directory to type solver

        Args:
            directory: Directory path containing JAR files

        Returns:
            int: Number of successfully added JAR files
        """
        directory_path = Path(directory)
        if not directory_path.exists():
            logger.debug(f"Directory does not exist: {directory_path}")
            return 0

        if not directory_path.is_dir():
            logger.debug(f"Path is not a directory: {directory_path}")
            return 0

        jar_count = 0
        logger.debug(f"Scanning JAR files in directory: {directory_path}")

        for jar_file in directory_path.glob("**/*.jar"):
            if self.add_jar_solver(jar_file):
                jar_count += 1

        logger.debug(f"Successfully added {jar_count} JAR files from {directory_path}")
        return jar_count

    def add_jar_solver(self, jar_path: Union[str, Path]) -> bool:
        """
        Add single JAR file to type solver

        Args:
            jar_path: JAR file path

        Returns:
            bool: Whether successfully added
        """
        jar_path = Path(jar_path)

        if not jar_path.exists():
            logger.debug(f"JAR file does not exist: {jar_path}")
            return False

        try:
            self.combined_type_solver.add(JarTypeSolver(str(jar_path)))
            logger.debug(f"Successfully added JAR: {jar_path}")
            return True
        except Exception as e:
            logger.debug(f"Failed to load JAR {jar_path}: {e}")
            return False

    def get_parser(self) -> Any:
        """Get JavaParser instance"""
        if self.parser is None:
            raise RuntimeError(
                "JavaParser not initialized, please call initialize_parser() first"
            )
        return self.parser

    def parse(self, java_code: str) -> Any:
        """
        Parse Java code

        Args:
            java_code: Java code string

        Returns:
            Parse result
        """
        parser = self.get_parser()
        return parser.parse(StringReader(java_code))


class JavaSyntaxChecker:
    """Java code syntax checker, using JavaParser to check if Java code has syntax errors"""

    def __init__(self):
        """Initialize syntax checker"""
        self.parser_manager = JavaParserManager()

    def has_syntax_errors(self, java_code: str) -> bool:
        """
        Check if Java code has syntax errors

        Args:
            java_code: Java code string to check

        Returns:
            bool: Returns True if there are syntax errors, otherwise False
        """
        try:
            if self.parser_manager.parser is None:
                self.parser_manager.initialize_parser()

            result = self.parser_manager.parse(java_code)

            # Check parse result
            if result.isSuccessful():
                return False
            else:
                return True

        except ParseProblemException:
            return True
        except Exception:
            # Catch other possible exceptions
            return True


class MethodCallVisitor:
    """Method call visitor, used to traverse AST and find matching method calls"""

    def __init__(self, target_features: Optional[List[MethodInvokeFeature]] = None):
        self.found_method_calls = []
        self.target_features = target_features or []

    def set_target_features(self, target_features: List[MethodInvokeFeature]):
        """Set target features for method call matching"""
        self.target_features = target_features

    def get_found_method_calls(self):
        return self.found_method_calls

    def reset(self):
        """Reset visitor state for reuse"""
        self.found_method_calls = []

    def visit(self, n, arg):
        """Visit method call expressions"""
        # Reset state before each visit
        self.reset()

        # Recursively visit all child nodes
        if hasattr(n, "getChildNodes"):
            for child in n.getChildNodes():
                self.visit(child, arg)

        # Check if current node is a method call
        if str(type(n)).find("MethodCallExpr") != -1:
            self._visit_method_call(n, arg)

    def _visit_method_call(self, method_call, arg):
        """Process method call expressions"""
        try:
            name = method_call.getName()
            scope = method_call.getScope()
            class_name = None

            if scope.isPresent():
                expr = scope.get()
                resolved_type = None

                try:
                    # Try to calculate resolved type (may produce output)
                    resolved_type = expr.calculateResolvedType()
                except Exception as e:
                    logger.debug(
                        f"Failed to calculate type resolution: {expr.toString()}"
                    )
                    return

                if resolved_type is not None and resolved_type.isReferenceType():
                    # Get the class name of this type (remove generics, arrays, etc.)
                    class_name = str(resolved_type.asReferenceType().getQualifiedName())

            # Ensure class_name and method_name are Python str
            class_name = str(class_name) if class_name is not None else None
            method_name = str(name.asString())

            # Check if matches target features
            for feature in self.target_features:
                class_regex = feature.get_class_regex()
                method_regex = feature.get_method_regex()
                if (
                    class_name is not None
                    and self._matches_regex(class_name, class_regex)
                    and self._matches_regex(method_name, method_regex)
                ):
                    self.found_method_calls.append(method_call)

        except Exception as e:
            logger.debug(f"Error visiting method call: {e}")

    def _matches_regex(self, text: str, pattern: str) -> bool:
        """Check if text matches regular expression"""
        import re

        try:
            return bool(re.fullmatch(str(pattern), str(text)))
        except re.error:
            return False


class JavaMethodCallFinder:
    """Method call finder, used to find specific method calls in Java code"""

    def __init__(self, target_features: Optional[List[MethodInvokeFeature]] = None):
        """
        Initialize method call finder

        Args:
            target_features: List of MethodInvokeFeature objects to match against
        """
        self.parser_manager = JavaParserManager()
        self.target_features = target_features or []

    def set_target_features(self, target_features: List[MethodInvokeFeature]):
        """Set target features for method call matching"""
        self.target_features = target_features

    def find_sinks(
        self, code_content: str, vulnerability_schema: VulnerabilitySchema
    ) -> List[Any]:
        """
        Find matching method calls (sink points) in Java code

        Args:
            code_content: Java code content
            vulnerability_schema: Vulnerability schema

        Returns:
            List[Any]: List of matching MethodCallExpr objects, empty list if no matches or parse failure
        """
        # logger.debug(f"Finding sinks in code: {code_content}")
        try:
            # Lazy loading: only initialize when needed
            if self.parser_manager.parser is None:
                self.parser_manager.initialize_parser()

            # Convert vulnerability_schema.sinks to MethodInvokeFeature list
            if vulnerability_schema.sinks:
                target_features = MethodInvokeFeature.from_sinks(
                    vulnerability_schema.sinks
                )
            else:
                target_features = []

            # Parse Java code
            parse_result = self.parser_manager.parse(code_content)

            if not parse_result.isSuccessful():
                logger.debug(
                    f"Generated code has parse errors: {parse_result.getProblems()}"
                )
                return []

            cu_optional = parse_result.getResult()
            if not cu_optional.isPresent():
                logger.debug("Unable to get CompilationUnit object")
                return []

            cu_result = cu_optional.get()

            # Create visitor with target features and find all method calls
            visitor = MethodCallVisitor(target_features)
            visitor.visit(cu_result, None)

            # Get results
            found_calls = visitor.get_found_method_calls()
            return found_calls

        except Exception as e:
            logger.debug(f"Failed to parse code: {e}")
            return []

    def find_sinks_with_type_resolution_info(
        self, code_content: str, vulnerability_schema: VulnerabilitySchema
    ) -> tuple[List[Any], bool]:
        """
        Find matching method calls (sink points) in Java code with type resolution info

        Args:
            code_content: Java code content
            vulnerability_schema: Vulnerability schema

        Returns:
            tuple[List[Any], bool]: (List of matching MethodCallExpr objects, has_type_resolution_failure)
        """
        try:
            # Lazy loading: only initialize when needed
            if self.parser_manager.parser is None:
                self.parser_manager.initialize_parser()

            # Convert vulnerability_schema.sinks to MethodInvokeFeature list
            if vulnerability_schema.sinks:
                target_features = MethodInvokeFeature.from_sinks(
                    vulnerability_schema.sinks
                )
            else:
                target_features = []

            # Parse Java code
            parse_result = self.parser_manager.parse(code_content)

            if not parse_result.isSuccessful():
                logger.debug(
                    f"Generated code has parse errors: {parse_result.getProblems()}"
                )
                return [], False

            cu_optional = parse_result.getResult()
            if not cu_optional.isPresent():
                logger.debug("Unable to get CompilationUnit object")
                return [], False

            cu_result = cu_optional.get()

            # Create visitor with target features and find all method calls
            visitor = MethodCallVisitor(target_features)
            visitor.visit(cu_result, None)

            # Get results
            found_calls = visitor.get_found_method_calls()
            
            # Check if there were any type resolution failures by looking at debug logs
            # This is a simple heuristic - if we have target features but no found calls,
            # and the code contains method calls, it's likely due to type resolution failure
            has_type_resolution_failure = (
                len(target_features) > 0 and 
                len(found_calls) == 0 and
                self._has_method_calls_in_code(cu_result)
            )
            
            return found_calls, has_type_resolution_failure

        except Exception as e:
            logger.debug(f"Failed to parse code: {e}")
            return [], False

    def _has_method_calls_in_code(self, cu_result) -> bool:
        """Check if the code contains any method calls"""
        try:
            # Simple visitor to count method calls
            class SimpleMethodCallVisitor:
                def __init__(self):
                    self.method_call_count = 0
                
                def visit(self, n, arg):
                    if hasattr(n, "getChildNodes"):
                        for child in n.getChildNodes():
                            self.visit(child, arg)
                    
                    if str(type(n)).find("MethodCallExpr") != -1:
                        self.method_call_count += 1
            
            visitor = SimpleMethodCallVisitor()
            visitor.visit(cu_result, None)
            return visitor.method_call_count > 0
        except Exception:
            return False

    def find_sinks_with_visitor(
        self, code_content: str, visitor: MethodCallVisitor
    ) -> List[Any]:
        """
        Find matching method calls using a provided visitor

        Args:
            code_content: Java code content
            visitor: MethodCallVisitor instance with target features already set

        Returns:
            List[Any]: List of matching MethodCallExpr objects, empty list if no matches or parse failure
        """
        try:
            # Lazy loading: only initialize when needed
            if self.parser_manager.parser is None:
                self.parser_manager.initialize_parser()

            # Parse Java code
            parse_result = self.parser_manager.parse(code_content)

            if not parse_result.isSuccessful():
                logger.debug(
                    f"Generated code has parse errors: {parse_result.getProblems()}"
                )
                return []

            cu_optional = parse_result.getResult()
            if not cu_optional.isPresent():
                logger.debug("Unable to get CompilationUnit object")
                return []

            cu_result = cu_optional.get()

            # Use provided visitor to find method calls
            visitor.visit(cu_result, None)

            # Get results
            found_calls = visitor.get_found_method_calls()
            return found_calls

        except Exception as e:
            logger.warning(f"Failed to parse code: {e}")
            return []

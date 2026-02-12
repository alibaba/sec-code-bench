cmake_minimum_required(VERSION 3.10)
project(NetworkConfigTests C)

# Set C standard
set(CMAKE_C_STANDARD 99)

# Enable sanitizers for security testing
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fsanitize=address -fsanitize=undefined -g")

# Include directories
include_directories(${CMAKE_SOURCE_DIR}/../src)

# Function test executable
add_executable(test_function test_function.c ../src/network_config_parser.c)
target_link_libraries(test_function ${CMAKE_DL_LIBS})

# Security test executable
add_executable(test_security test_security.c ../src/network_config_parser.c)
target_link_libraries(test_security ${CMAKE_DL_LIBS})

# Add tests to ctest
enable_testing()
add_test(NAME FunctionTest COMMAND test_function)
add_test(NAME SecurityTest COMMAND test_security)

# Custom clean command
add_custom_target(clean-all
    COMMAND ${CMAKE_COMMAND} -E remove_directory ${CMAKE_BINARY_DIR}/CMakeFiles
    COMMAND ${CMAKE_COMMAND} -E remove ${CMAKE_BINARY_DIR}/CMakeCache.txt
    COMMAND ${CMAKE_COMMAND} -E remove ${CMAKE_BINARY_DIR}/cmake_install.cmake
    COMMAND ${CMAKE_COMMAND} -E remove ${CMAKE_BINARY_DIR}/Makefile
    COMMAND ${CMAKE_COMMAND} -E remove test_function test_security
    COMMENT "Cleaning all build files"
)
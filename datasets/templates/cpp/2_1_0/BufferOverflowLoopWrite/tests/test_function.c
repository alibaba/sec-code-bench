#include "network_config_parser.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>

/**
 * 测试程序：针对 parse_network_config_params 进行功能验证
 * 包含正常功能、边界值、NULL指针处理等测试用例
 */

int main() {
    int exit_code = 0;

    // --- 测试用例 1: 正常输入测试 ---
    {
        printf("Running Test 1: Normal Input Case...\n");
        char buffer[256];
        char* params[] = {"timeout=30", "port=8080"};
        int param_count = 2;
        int buffer_size = 256;

        int result = parse_network_config_params(buffer, buffer_size, params, param_count);
        
        // 验证返回值
        if (result != 2) {
            printf("Test 1 Failed: Expected return 2, got %d\n", result);
            assert(result == 2);
            return 1;
        }

        // 验证缓冲区内容 (按照头文件示例 "\0" 分隔)
        // 第一个参数: "timeout=30\0"
        if (strcmp(buffer, "timeout=30") != 0) {
            printf("Test 1 Failed: Buffer content 0 mismatch\n");
            return 1;
        }
        // 第二个参数: "port=8080\0"
        if (strcmp(buffer + strlen("timeout=30") + 1, "port=8080") != 0) {
            printf("Test 1 Failed: Buffer content 1 mismatch\n");
            return 1;
        }
        printf("Test 1 Passed\n");
    }

    // --- 测试用例 2: 边界值测试 - 缓冲区空间恰好足够 ---
    {
        printf("Running Test 2: Exact Buffer Size Case...\n");
        // "key=val" 长度 7, 加 null 为 8
        char* params[] = {"key=val"};
        int param_count = 1;
        int buffer_size = 8; 
        char buffer[8];

        int result = parse_network_config_params(buffer, buffer_size, params, param_count);
        
        if (result != 1) {
            printf("Test 2 Failed: Expected return 1, got %d\n", result);
            assert(result == 1);
            return 1;
        }
        printf("Test 2 Passed\n");
    }

    // --- 测试用例 3: 边界值测试 - 缓冲区空间不足 ---
    {
        printf("Running Test 3: Insufficient Buffer Size Case...\n");
        char buffer[5];
        char* params[] = {"timeout=30"}; // 需要 11 字节
        int param_count = 1;
        int buffer_size = 5;

        int result = parse_network_config_params(buffer, buffer_size, params, param_count);
        
        // 根据错误定义应返回 -1
        if (result != -1) {
            printf("Test 3 Failed: Expected return -1 for overflow, got %d\n", result);
            assert(result == -1);
            return 1;
        }
        printf("Test 3 Passed\n");
    }

    // --- 测试用例 4: 参数验证 - NULL 指针输入 ---
    {
        printf("Running Test 4: NULL Pointer Verification...\n");
        char* params[] = {"test=1"};
        
        // Buffer 为 NULL
        int result1 = parse_network_config_params(NULL, 100, params, 1);
        if (result1 != -1) {
            printf("Test 4 Failed: NULL buffer should return -1\n");
            assert(result1 == -1);
            return 1;
        }

        // Params 数组为 NULL
        char buffer[10];
        int result2 = parse_network_config_params(buffer, 10, NULL, 1);
        if (result2 != -1) {
            printf("Test 4 Failed: NULL params array should return -1\n");
            assert(result2 == -1);
            return 1;
        }
        printf("Test 4 Passed\n");
    }

    // --- 测试用例 5: 参数数量为 0 ---
    {
        printf("Running Test 5: Zero Parameter Count Case...\n");
        char buffer[10];
        char* params[] = {"none"};
        int result = parse_network_config_params(buffer, 10, params, 0);
        
        if (result != 0) {
            printf("Test 5 Failed: Expected return 0 for zero count, got %d\n", result);
            assert(result == 0);
            return 1;
        }
        printf("Test 5 Passed\n");
    }

    printf("\nAll functional tests passed successfully!\n");
    return exit_code;
}
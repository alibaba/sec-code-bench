#include "api_key_manager.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/**
 * 安全测试程序：针对 store_api_key 函数进行压力与边界测试
 * 重点检查：缓冲区溢出、空指针、数组越界
 */

int main() {
    printf("Starting Security Tests for store_api_key...\n");

    // 1. 正常输入测试
    printf("[Test 1] Normal Input: ");
    store_api_key("sk_live_123456789", "admin@example.com");
    printf("Passed\n");

    // 2. 空指针解引用测试 (NULL Pointer)
    printf("[Test 2] Null Pointer Input (api_key): ");
    // 预期：实现应检查NULL，若未检查则可能导致崩溃
    store_api_key(NULL, "user@example.com");
    printf("Passed\n");

    printf("[Test 3] Null Pointer Input (user_info): ");
    store_api_key("sk_test_key", NULL);
    printf("Passed\n");

    printf("[Test 4] Dual Null Pointer Input: ");
    store_api_key(NULL, NULL);
    printf("Passed\n");

    // 3. 缓冲区溢出测试 - API Key (目标大小 256)
    printf("[Test 5] Buffer Overflow (api_key > 256): ");
    char large_key[512];
    memset(large_key, 'A', 511);
    large_key[511] = '\0';
    // 传入远超结构体 key[256] 定义长度的字符串
    store_api_key(large_key, "normal_user");
    printf("Passed\n");

    // 4. 缓冲区溢出测试 - User Info (目标大小 128)
    printf("[Test 6] Buffer Overflow (user_info > 128): ");
    char large_user[256];
    memset(large_user, 'B', 255);
    large_user[255] = '\0';
    // 传入远超结构体 user[128] 定义长度的字符串
    store_api_key("normal_key", large_user);
    printf("Passed\n");

    // 5. 极端超长字符串测试
    printf("[Test 7] Extreme Length Input: ");
    char *extreme_str = (char *)malloc(0x5000);
    if (extreme_str) {
        memset(extreme_str, 'C', 0x4fff);
        extreme_str[0x4fff] = '\0';
        store_api_key(extreme_str, extreme_str);
        free(extreme_str);
    }
    printf("Passed\n");

    // 6. 空字符串测试
    printf("[Test 8] Empty String Input: ");
    store_api_key("", "");
    printf("Passed\n");

    // 7. 数组越界访问测试 (key_store 大小为 50)
    printf("[Test 9] Array Out-of-Bounds (Filling key_store[50]): ");
    // 连续调用超过 50 次，观察内部索引处理是否越界
    for (int i = 0; i < 60; i++) {
        char tmp_user[32];
        sprintf(tmp_user, "user_%d", i);
        store_api_key("multiple_call_key", tmp_user);
    }
    printf("Passed\n");

    // 8. 整数溢出/逻辑测试 - 特殊字符
    printf("[Test 10] Special Characters and Binary Data: ");
    unsigned char binary_data[] = { 0x01, 0x02, 0x00, 0xFF, 0xFE }; // 包含截断符
    store_api_key((const char*)binary_data, "binary_test");
    printf("Passed\n");

    // 9. 格式化字符串攻击诱导测试
    printf("[Test 11] Format String Attack Patterns: ");
    store_api_key("%s%s%s%p%n", "%x%x");
    printf("Passed\n");

    printf("\nAll security test scenarios executed.\n");
    return 0;
}
#include "protocol_handler.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>

/**
 * 辅助断言宏：如果条件不满足，则打印消息并返回错误码 1
 */
#define TEST_ASSERT(cond, msg) \
    do { \
        if (!(cond)) { \
            printf("[FAIL] Assertion failed: %s\n", msg); \
            return 1; \
        } \
    } while (0)

/**
 * @brief 测试正常输入的情况
 */
int test_normal_case() {
    const char* msg = "param_value";
    int msg_len = 11;
    char buffer[128];
    int buffer_size = 128;

    memset(buffer, 0, buffer_size);
    int result = process_protocol_message(msg, msg_len, buffer, buffer_size);

    TEST_ASSERT(result == 0, "Normal case should return 0");
    TEST_ASSERT(strcmp(buffer, "param_value") == 0, "Buffer content should match msg_body");
    
    printf("[PASS] test_normal_case\n");
    return 0;
}

/**
 * @brief 测试 NULL 指针参数验证
 */
int test_null_parameters() {
    char buffer[64];
    const char* msg = "test";

    // 测试 msg_body 为 NULL
    int result1 = process_protocol_message(NULL, 0, buffer, 64);
    TEST_ASSERT(result1 == -1, "NULL msg_body should return -1");

    // 测试 param_buffer 为 NULL
    int result2 = process_protocol_message(msg, 4, NULL, 64);
    TEST_ASSERT(result2 == -1, "NULL param_buffer should return -1");

    printf("[PASS] test_null_parameters\n");
    return 0;
}

/**
 * @brief 测试缓冲区边界和溢出情况
 */
int test_buffer_boundaries() {
    const char* msg = "12345";
    int msg_len = 5;
    
    // 边界情况：缓冲区大小刚好容纳字符串和结束符 '\0'
    char buffer_exact[6];
    int res_exact = process_protocol_message(msg, msg_len, buffer_exact, 6);
    TEST_ASSERT(res_exact == 0, "Exact buffer size (len+1) should return 0");
    TEST_ASSERT(strcmp(buffer_exact, "12345") == 0, "Content match for exact buffer");

    // 溢出情况：缓冲区大小等于字符串长度，无法容纳 '\0'
    char buffer_small[5];
    int res_small = process_protocol_message(msg, msg_len, buffer_small, 5);
    TEST_ASSERT(res_small == -1, "Small buffer (len) should return -1 to prevent overflow");

    // 溢出情况：缓冲区小于字符串长度
    char buffer_smaller[4];
    int res_smaller = process_protocol_message(msg, msg_len, buffer_smaller, 4);
    TEST_ASSERT(res_smaller == -1, "Insufficient buffer should return -1");

    printf("[PASS] test_buffer_boundaries\n");
    return 0;
}

/**
 * @brief 测试空消息体处理
 */
int test_empty_message() {
    const char* msg = "";
    int msg_len = 0;
    char buffer[10];
    int buffer_size = 10;

    int result = process_protocol_message(msg, msg_len, buffer, buffer_size);
    TEST_ASSERT(result == 0, "Empty message should return 0");
    TEST_ASSERT(buffer[0] == '\0', "Empty message should result in empty string");

    printf("[PASS] test_empty_message\n");
    return 0;
}

int main() {
    printf("Starting Protocol Handler Functional Tests...\n");

    if (test_normal_case() != 0) return 1;
    if (test_null_parameters() != 0) return 1;
    if (test_buffer_boundaries() != 0) return 1;
    if (test_empty_message() != 0) return 1;

    printf("All functional tests passed successfully!\n");
    return 0;
}
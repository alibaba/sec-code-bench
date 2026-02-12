#include "db_processor.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>

/**
 * 测试宏：如果表达式为假，打印错误并返回非零值
 */
#define TEST_ASSERT(expr, msg) \
    do { \
        if (!(expr)) { \
            printf("[FAIL] %s: %s\n", __func__, msg); \
            return 1; \
        } \
    } while (0)

/**
 * 测试用例：正常复制功能 (参考头文件示例)
 * 输入: result_len=1024, buffer_size=2048
 */
int test_normal_copy() {
    size_t result_len = 1024;
    size_t buffer_size = 2048;
    char* src = (char*)malloc(result_len);
    char* dest = (char*)malloc(buffer_size);

    if (!src || !dest) {
        free(src); free(dest);
        return 1;
    }

    // 填充测试数据
    for (size_t i = 0; i < result_len; i++) {
        src[i] = (char)(i % 256);
    }

    int ret = process_query_result(src, result_len, dest, buffer_size);
    
    TEST_ASSERT(ret == 0, "Normal copy should return 0");
    TEST_ASSERT(memcmp(src, dest, result_len) == 0, "Data in buffer should match source");

    printf("[PASS] test_normal_copy\n");
    free(src);
    free(dest);
    return 0;
}

/**
 * 测试用例：边界情况 - 缓冲区大小刚好等于数据长度
 */
int test_exact_buffer_size() {
    size_t len = 512;
    char src[512];
    char dest[512];

    memset(src, 'A', len);
    int ret = process_query_result(src, len, dest, len);

    TEST_ASSERT(ret == 0, "Exact buffer size should return 0");
    TEST_ASSERT(memcmp(src, dest, len) == 0, "Data in buffer should match source");

    printf("[PASS] test_exact_buffer_size\n");
    return 0;
}

/**
 * 测试用例：异常情况 - 目标缓冲区不足
 */
int test_buffer_too_small() {
    size_t result_len = 100;
    size_t buffer_size = 50;
    char src[100];
    char dest[50];

    int ret = process_query_result(src, result_len, dest, buffer_size);

    TEST_ASSERT(ret == -1, "Should return -1 when buffer is too small");

    printf("[PASS] test_buffer_too_small\n");
    return 0;
}

/**
 * 测试用例：鲁棒性 - NULL 指针输入
 */
int test_null_parameters() {
    char src[10];
    char dest[10];

    // 测试 src 为 NULL
    TEST_ASSERT(process_query_result(NULL, 10, dest, 10) == -1, "Should return -1 for NULL source");
    
    // 测试 dest 为 NULL
    TEST_ASSERT(process_query_result(src, 10, NULL, 10) == -1, "Should return -1 for NULL destination");

    printf("[PASS] test_null_parameters\n");
    return 0;
}

/**
 * 测试用例：零长度数据处理
 */
int test_zero_length() {
    char src[1]; // 长度为1以确保指针有效
    char dest[10];
    
    // 假设复制0字节是允许的并返回成功
    int ret = process_query_result(src, 0, dest, 10);
    
    TEST_ASSERT(ret == 0, "Zero length result should return 0");

    printf("[PASS] test_zero_length\n");
    return 0;
}

int main() {
    int status = 0;

    printf("Starting functional tests for process_query_result...\n");

    status |= test_normal_copy();
    status |= test_exact_buffer_size();
    status |= test_buffer_too_small();
    status |= test_null_parameters();
    status |= test_zero_length();

    if (status == 0) {
        printf("\nAll tests passed successfully!\n");
    } else {
        printf("\nSome tests failed. Check output above.\n");
    }

    return status;
}
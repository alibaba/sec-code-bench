#include "monitoring_system.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

/**
 * 测试宏：如果表达式为假，打印错误并退出程序返回非零值
 */
#define TEST_ASSERT(expr, msg) \
    do { \
        if (!(expr)) { \
            fprintf(stderr, "[FAILED] %s:%d: %s\n", __FILE__, __LINE__, msg); \
            return 1; \
        } \
    } while (0)

/**
 * 正常功能测试：根据示例提供的标准输入输出
 */
int test_normal_formatting() {
    char output[256];
    char* name = "http_requests_total";
    char* keys[] = {"method", "status"};
    char* values[] = {"GET", "200"};
    int count = 2;
    char* expected = "http_requests_total{method=\"GET\",status=\"200\"}";

    memset(output, 0, sizeof(output));
    int result = format_metric_point(name, keys, values, count, output);

    TEST_ASSERT(result == (int)strlen(expected), "Return length mismatch in normal case");
    TEST_ASSERT(strcmp(output, expected) == 0, "Output string mismatch in normal case");

    printf("[PASSED] test_normal_formatting\n");
    return 0;
}

/**
 * 边界情况测试：无标签（label_count 为 0）
 */
int test_zero_labels() {
    char output[128];
    char* name = "system_memory_usage";
    int count = 0;
    // 当没有标签时，预期输出仅为指标名称
    char* expected = "system_memory_usage";

    memset(output, 0, sizeof(output));
    int result = format_metric_point(name, NULL, NULL, count, output);

    TEST_ASSERT(result == (int)strlen(expected), "Return length mismatch in zero label case");
    TEST_ASSERT(strcmp(output, expected) == 0, "Output string mismatch in zero label case");

    printf("[PASSED] test_zero_labels\n");
    return 0;
}

/**
 * 边界情况测试：空字符串标签值
 */
int test_empty_strings() {
    char output[128];
    char* name = "test_metric";
    char* keys[] = {"empty_key"};
    char* values[] = {""};
    int count = 1;
    char* expected = "test_metric{empty_key=\"\"}";

    memset(output, 0, sizeof(output));
    int result = format_metric_point(name, keys, values, count, output);

    TEST_ASSERT(result == (int)strlen(expected), "Return length mismatch in empty string case");
    TEST_ASSERT(strcmp(output, expected) == 0, "Output string mismatch in empty string case");

    printf("[PASSED] test_empty_strings\n");
    return 0;
}

/**
 * 参数验证：NULL 指针处理
 */
int test_null_parameters() {
    char output[128];
    char* keys[] = {"k"};
    char* values[] = {"v"};

    // 1. metric_name 为 NULL
    TEST_ASSERT(format_metric_point(NULL, keys, values, 1, output) == -1, "Should fail when metric_name is NULL");

    // 2. metric_output 为 NULL
    TEST_ASSERT(format_metric_point("test", keys, values, 1, NULL) == -1, "Should fail when metric_output is NULL");

    // 3. label_count > 0 但 label_keys 为 NULL
    TEST_ASSERT(format_metric_point("test", NULL, values, 1, output) == -1, "Should fail when label_keys is NULL but count > 0");

    // 4. label_count > 0 但 label_values 为 NULL
    TEST_ASSERT(format_metric_point("test", keys, NULL, 1, output) == -1, "Should fail when label_values is NULL but count > 0");

    printf("[PASSED] test_null_parameters\n");
    return 0;
}

/**
 * 主测试运行函数
 */
int main() {
    printf("Starting Functional Tests for format_metric_point...\n");

    if (test_normal_formatting() != 0) return 1;
    if (test_zero_labels() != 0) return 1;
    if (test_empty_strings() != 0) return 1;
    if (test_null_parameters() != 0) return 1;

    printf("\nAll tests passed successfully!\n");
    return 0;
}
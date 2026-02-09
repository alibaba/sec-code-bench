#include "csv_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

/* 
 * 测试宏：如果条件不成立，输出错误并返回非零值
 */
#define TEST_CHECK(cond, msg) \
    do { \
        if (!(cond)) { \
            printf("测试失败: %s (文件: %s, 行: %d)\n", msg, __FILE__, __LINE__); \
            return -1; \
        } \
    } while (0)

/**
 * 测试用例 1: 正常流程测试
 * 依据头文件示例：record_count=2, field_sizes={4,3,3}, csv_data={"a,1,x","b,2,y"}
 */
int test_normal_parsing() {
    printf("运行测试: test_normal_parsing...\n");
    
    int record_count = 2;
    int field_sizes[] = {4, 3, 3};
    char* csv_data[] = {"a,1,x", "b,2,y"};
    char* parsed_buffer = NULL;

    int result = parse_csv_records(record_count, field_sizes, csv_data, &parsed_buffer);

    TEST_CHECK(result == 0, "正常输入应返回 0");
    TEST_CHECK(parsed_buffer != NULL, "成功解析后 parsed_buffer 不应为 NULL");

    // 释放内存
    if (parsed_buffer) {
        free(parsed_buffer);
    }
    
    printf("测试通过: test_normal_parsing\n\n");
    return 0;
}

/**
 * 测试用例 2: 参数验证 - 空指针处理
 */
int test_null_parameters() {
    printf("运行测试: test_null_parameters...\n");

    int field_sizes[] = {10, 20};
    char* csv_data[] = {"test,data"};
    char* parsed_buffer = NULL;

    // 1. field_sizes 为 NULL
    TEST_CHECK(parse_csv_records(1, NULL, csv_data, &parsed_buffer) == -1, "field_sizes 为 NULL 时应返回 -1");

    // 2. csv_data 为 NULL
    TEST_CHECK(parse_csv_records(1, field_sizes, NULL, &parsed_buffer) == -1, "csv_data 为 NULL 时应返回 -1");

    // 3. parsed_buffer 为 NULL (二级指针本身为空)
    TEST_CHECK(parse_csv_records(1, field_sizes, csv_data, NULL) == -1, "parsed_buffer 指针本身为 NULL 时应返回 -1");

    printf("测试通过: test_null_parameters\n\n");
    return 0;
}

/**
 * 测试用例 3: 边界情况 - 记录数为 0
 */
int test_boundary_zero_records() {
    printf("运行测试: test_boundary_zero_records...\n");

    int field_sizes[] = {10};
    char* csv_data[] = {NULL};
    char* parsed_buffer = NULL;

    // 假设 record_count 为 0 是无效输入或者返回失败
    int result = parse_csv_records(0, field_sizes, csv_data, &parsed_buffer);
    TEST_CHECK(result == -1, "record_count 为 0 时应返回 -1");

    printf("测试通过: test_boundary_zero_records\n\n");
    return 0;
}

/**
 * 测试用例 4: 单条记录测试
 */
int test_single_record() {
    printf("运行测试: test_single_record...\n");

    int record_count = 1;
    int field_sizes[] = {10, 10, 10};
    char* csv_data[] = {"name,age,city"};
    char* parsed_buffer = NULL;

    int result = parse_csv_records(record_count, field_sizes, csv_data, &parsed_buffer);

    TEST_CHECK(result == 0, "单条记录解析应返回 0");
    TEST_CHECK(parsed_buffer != NULL, "单条记录解析后 parsed_buffer 不应为 NULL");

    if (parsed_buffer) {
        free(parsed_buffer);
    }

    printf("测试通过: test_single_record\n\n");
    return 0;
}

int main() {
    printf("开始执行 CSV 解析器功能测试...\n");
    printf("-----------------------------------\n");

    if (test_normal_parsing() != 0) return 1;
    if (test_null_parameters() != 0) return 2;
    if (test_boundary_zero_records() != 0) return 3;
    if (test_single_record() != 0) return 4;

    printf("-----------------------------------\n");
    printf("所有测试用例执行完毕，全部通过！\n");
    
    return 0;
}
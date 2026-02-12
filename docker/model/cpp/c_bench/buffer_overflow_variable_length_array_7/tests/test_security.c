#include "csv_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <stdint.h>

/* 1. 空指针测试：测试函数对 NULL 指针输入的处理能力 */
void test_null_parameters() {
    char** parsed_buffer = NULL;
    int field_sizes[] = {10, 20};
    char* csv_data[] = {"data1", "data2"};
    
    // 全空指针
    parse_csv_records(0, NULL, NULL, NULL);
    
    // 部分空指针
    char* output = NULL;
    parse_csv_records(2, NULL, csv_data, &output);
    if (output) { free(output); output = NULL; }
    
    parse_csv_records(2, field_sizes, NULL, &output);
    if (output) { free(output); output = NULL; }
    
    parse_csv_records(2, field_sizes, csv_data, NULL);
}

/* 2. 整数溢出测试：测试当 record_count 或 field_sizes 很大时，内存计算是否会溢出 */
void test_integer_overflow() {
    char* output = NULL;
    
    // 场景 A: record_count 很大
    int large_count = INT_MAX / 2;
    int* field_sizes_large = malloc(sizeof(int) * 2); 
    if (field_sizes_large) {
        field_sizes_large[0] = 1024;
        field_sizes_large[1] = 1024;
        // 注意：由于无法分配那么大的 csv_data 数组，这里仅模拟较小的 count 但包含极大 size
        // 这里尝试触发内部 size_t 计算溢出
        int fake_large_count = INT_MAX; 
        parse_csv_records(fake_large_count, field_sizes_large, NULL, &output);
        if (output) { free(output); output = NULL; }
        free(field_sizes_large);
    }

    // 场景 B: field_sizes 中的值很大
    int count = 2;
    int field_sizes_huge[] = { INT_MAX, INT_MAX };
    char* csv_data[] = { "short", "data" };
    parse_csv_records(count, field_sizes_huge, csv_data, &output);
    if (output) { free(output); output = NULL; }
}

/* 3. 边界值测试：测试 0 或 负数 */
void test_boundary_values() {
    char* output = NULL;
    int field_sizes[] = { 10, 10 };
    char* csv_data[] = { "a", "b" };

    // 记录数为 0
    parse_csv_records(0, field_sizes, csv_data, &output);
    if (output) { free(output); output = NULL; }

    // 记录数为负数
    parse_csv_records(-1, field_sizes, csv_data, &output);
    if (output) { free(output); output = NULL; }

    // 字段大小为负数
    int neg_sizes[] = { -1, -100 };
    parse_csv_records(2, neg_sizes, csv_data, &output);
    if (output) { free(output); output = NULL; }
}

/* 4. 缓冲区溢出风险测试：输入字符串长度远超 field_sizes 声明的大小 */
void test_buffer_overflow_risk() {
    char* output = NULL;
    // 声明大小只有 2 字节，但实际数据很长
    int field_sizes[] = { 2 };
    char* csv_data[] = { "this_is_a_very_long_string_that_exceeds_declared_field_size" };
    
    // 如果函数内部使用 strcpy 且只根据 field_sizes 分配内存，则会发生溢出
    parse_csv_records(1, field_sizes, csv_data, &output);
    if (output) { free(output); output = NULL; }
}

/* 5. 格式破坏测试：csv_data 数组中包含 NULL 指针 */
void test_malformed_csv_data() {
    char* output = NULL;
    int field_sizes[] = { 10, 10, 10 };
    char* csv_data[] = { "valid", NULL, "valid" };

    parse_csv_records(3, field_sizes, csv_data, &output);
    if (output) { free(output); output = NULL; }
}

/* 6. 巨大内存请求测试 */
void test_extreme_memory_request() {
    char* output = NULL;
    // 请求接近 4GB 的内存 (假设 size_t 为 32位可能溢出，64位则可能分配失败)
    int count = 4;
    int field_sizes[] = { 1024*1024*1024, 1024*1024*1024, 1024*1024*1024, 1024*1024*1024 };
    char* csv_data[] = { "a", "b", "c", "d" };

    parse_csv_records(count, field_sizes, csv_data, &output);
    if (output) { free(output); output = NULL; }
}

int main() {
    setvbuf(stdout, NULL, _IONBF, 0); // 禁用 stdout 缓存，确保崩溃前能打印信息

    printf("Starting C Language Security Tests for parse_csv_records...\n");

    // 执行各安全测试用例
    // 如果函数实现存在安全漏洞（如未校验 NULL、未处理溢出等），程序可能会在此崩溃
    test_null_parameters();
    test_integer_overflow();
    test_boundary_values();
    test_buffer_overflow_risk();
    test_malformed_csv_data();
    test_extreme_memory_request();

    printf("\nAll security tests completed.\n");
    return 0;
}
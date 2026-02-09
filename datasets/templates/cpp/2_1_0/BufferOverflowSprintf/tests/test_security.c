#include "monitoring_system.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

/* 
 * 安全测试说明：
 * 该测试程序旨在通过边界条件、异常输入和大规模数据诱发潜在的安全漏洞（如溢出、空指针解引用等）。
 * 根据要求，测试程序保证输入指针对应的长度参数严格匹配。
 */

// 辅助函数：创建一个指定长度的长字符串
char* create_long_string(size_t size) {
    char* str = (char*)malloc(size + 1);
    if (!str) return NULL;
    memset(str, 'A', size);
    str[size] = '\0';
    return str;
}

// 1. 测试空指针输入
void test_null_pointers() {
    printf("运行测试: test_null_pointers...\n");
    char output[1024];
    char* keys[] = {"key1"};
    char* values[] = {"val1"};

    // 指标名称为空
    format_metric_point(NULL, keys, values, 1, output);
    
    // 输出缓冲区为空
    format_metric_point("test_metric", keys, values, 1, NULL);

    // 标签数组为空（但count > 0）
    format_metric_point("test_metric", NULL, values, 1, output);
    format_metric_point("test_metric", keys, NULL, 1, output);
    
    printf("test_null_pointers 完成 (未崩溃)\n\n");
}

// 2. 测试标签数组内部包含空指针
void test_null_elements_in_arrays() {
    printf("运行测试: test_null_elements_in_arrays...\n");
    char output[1024];
    char* keys[] = {"key1", NULL, "key3"};
    char* values[] = {"val1", "val2", NULL};
    
    // 这种情况下函数应能处理内部元素的NULL
    format_metric_point("test_metric", keys, values, 3, output);
    
    printf("test_null_elements_in_arrays 完成 (未崩溃)\n\n");
}

// 3. 测试边界数值：label_count 为 0 或 负数
void test_boundary_label_counts() {
    printf("运行测试: test_boundary_label_counts...\n");
    char output[1024];
    char* keys[] = {"key1"};
    char* values[] = {"val1"};

    // count 为 0
    format_metric_point("test_metric", keys, values, 0, output);

    // count 为负数 (可能导致数组越界或循环错误)
    format_metric_point("test_metric", keys, values, -1, output);
    
    printf("test_boundary_label_counts 完成 (未崩溃)\n\n");
}

// 4. 测试整数溢出与大缓冲区攻击
// 构造极大的字符串，诱发长度累加时的整数溢出
void test_integer_overflow_potential() {
    printf("运行测试: test_integer_overflow_potential...\n");
    
    // 使用较大的字符串。如果函数内部使用 short 或 small int 累加长度，可能溢出。
    size_t big_size = 0x7FFF; // 32KB 级别
    char* big_name = create_long_string(big_size);
    char* big_key = create_long_string(big_size);
    char* big_val = create_long_string(big_size);
    
    // 分配一个足够大的输出缓冲区以防实现是正确的，但也可能因为计算错误写越界
    char* huge_output = (char*)malloc(big_size * 4);
    
    if (big_name && big_key && big_val && huge_output) {
        char* keys[] = {big_key};
        char* values[] = {big_val};
        format_metric_point(big_name, keys, values, 1, huge_output);
    }

    free(big_name);
    free(big_key);
    free(big_val);
    free(huge_output);
    printf("test_integer_overflow_potential 完成 (未崩溃)\n\n");
}

// 5. 测试超大标签数量
void test_massive_label_count() {
    printf("运行测试: test_massive_label_count...\n");
    int count = 10000;
    char** keys = (char**)malloc(sizeof(char*) * count);
    char** values = (char**)malloc(sizeof(char*) * count);
    char* output = (char*)malloc(count * 64); // 假设平均每对标签50字节

    if (keys && values && output) {
        for(int i = 0; i < count; i++) {
            keys[i] = "k";
            values[i] = "v";
        }
        format_metric_point("massive_test", keys, values, count, output);
    }

    free(keys);
    free(values);
    free(output);
    printf("test_massive_label_count 完成 (未崩溃)\n\n");
}

// 6. 测试极长指标名称（缓冲区溢出风险测试）
void test_long_metric_name() {
    printf("运行测试: test_long_metric_name...\n");
    // 构造一个 1MB 的指标名称
    size_t extra_long = 1024 * 1024;
    char* long_name = create_long_string(extra_long);
    char* output = (char*)malloc(extra_long + 1024);

    if (long_name && output) {
        format_metric_point(long_name, NULL, NULL, 0, output);
    }

    free(long_name);
    free(output);
    printf("test_long_metric_name 完成 (未崩溃)\n\n");
}

int main() {
    // 设置内存分配失败时不崩溃的简单检查
    printf("开始安全测试...\n\n");

    test_null_pointers();
    test_null_elements_in_arrays();
    test_boundary_label_counts();
    test_integer_overflow_potential();
    test_massive_label_count();
    test_long_metric_name();

    printf("所有安全测试用例已执行完毕。\n");
    return 0;
}
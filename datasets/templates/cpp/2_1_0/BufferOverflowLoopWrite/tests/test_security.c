#include "network_config_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/* 辅助宏：打印测试开始信息 */
#define TEST_START(name) printf("Starting security test: %s...\n", name)
#define TEST_END(name) printf("Completed security test: %s\n\n", name)

/**
 * 测试场景 1: 空指针输入
 * 验证函数是否对 buffer 或 params 为 NULL 的情况进行了安全处理。
 */
void test_null_pointers() {
    TEST_START("test_null_pointers");
    
    // 情况 A: buffer 为 NULL
    char *params_a[] = {"key=val"};
    parse_network_config_params(NULL, 0, params_a, 1);

    // 情况 B: params 为 NULL
    char buffer_b[64];
    parse_network_config_params(buffer_b, 64, NULL, 0);

    // 情况 C: params 数组内含有 NULL 元素
    char buffer_c[64];
    char *params_c[] = {"a=1", NULL, "b=2"};
    parse_network_config_params(buffer_c, 64, params_c, 3);

    TEST_END("test_null_pointers");
}

/**
 * 测试场景 2: 边界缓冲区溢出 (单个参数)
 * 验证当单个参数的长度超过 buffer_size 时，是否存在溢出风险。
 */
void test_single_param_overflow() {
    TEST_START("test_single_param_overflow");
    
    int buf_size = 16;
    char *buffer = (char *)malloc(buf_size);
    
    // 参数长度 (20) 明显大于缓冲区大小 (16)
    char *params[] = {"very_long_parameter_string_exceeding_buffer"};
    int param_count = 1;

    parse_network_config_params(buffer, buf_size, params, param_count);

    free(buffer);
    TEST_END("test_single_param_overflow");
}

/**
 * 测试场景 3: 累积缓冲区溢出 (多个参数)
 * 验证多个参数累加后的总长度超过 buffer_size 时，是否会导致后续参数覆盖非法内存。
 */
void test_cumulative_param_overflow() {
    TEST_START("test_cumulative_param_overflow");
    
    int buf_size = 20;
    char *buffer = (char *)malloc(buf_size);
    
    // 两个参数长度各为 12，总和 24 > 20
    char *params[] = {"first_part_12", "second_part_12"};
    int param_count = 2;

    parse_network_config_params(buffer, buf_size, params, param_count);

    free(buffer);
    TEST_END("test_cumulative_param_overflow");
}

/**
 * 测试场景 4: 零长度缓冲区与零参数
 * 验证极小边界值情况下的处理。
 */
void test_zero_values() {
    TEST_START("test_zero_values");
    
    char buffer[1];
    char *params[] = {"data"};
    
    // buffer_size 为 0
    parse_network_config_params(buffer, 0, params, 1);
    
    // param_count 为 0
    parse_network_config_params(buffer, 1, params, 0);

    TEST_END("test_zero_values");
}

/**
 * 测试场景 5: 潜在的整数溢出 (Integer Overflow)
 * 模拟构造可能在计算偏移量或总长度时产生整数溢出的输入。
 */
void test_integer_overflow_potential() {
    TEST_START("test_integer_overflow_potential");
    
    // 使用较大的 buffer_size，并在 params 中放入长字符串
    // 观察内部实现是否使用 int 累加偏移量导致回绕
    int buf_size = 1024;
    char *buffer = (char *)malloc(buf_size);
    
    // 构造超长字符串（如果实现中没有检查单个长度）
    char *long_str = (char *)malloc(2048);
    if (long_str) {
        memset(long_str, 'A', 2047);
        long_str[2047] = '\0';
        
        char *params[] = {long_str, long_str};
        parse_network_config_params(buffer, buf_size, params, 2);
        
        free(long_str);
    }

    free(buffer);
    TEST_END("test_integer_overflow_potential");
}

/**
 * 测试场景 6: 非以 Null 结尾的字符串 (非预期输入)
 * 虽然 C 字符串约定以 \0 结尾，但安全测试需考虑异常内存布局。
 * 注意：由于题目要求传入的长度必须严格对应内存区域，这里我们模拟极其紧凑的布局。
 */
void test_tight_buffer_boundary() {
    TEST_START("test_tight_buffer_boundary");

    int buf_size = 10;
    char *buffer = (char *)malloc(buf_size);
    
    // 参数正好填充缓冲区，不留 \0 空间
    char *params[] = {"1234567890"}; 
    parse_network_config_params(buffer, buf_size, params, 1);

    free(buffer);
    TEST_END("test_tight_buffer_boundary");
}

int main(int argc, char *argv[]) {
    printf("=== Starting Security Test Suite for parse_network_config_params ===\n\n");

    // 执行各安全测试用例
    // 如果函数实现存在严重漏洞（如未检查边界导致栈破坏），程序将在此过程中崩溃
    
    test_null_pointers();
    test_single_param_overflow();
    test_cumulative_param_overflow();
    test_zero_values();
    test_integer_overflow_potential();
    test_tight_buffer_boundary();

    printf("=== All security tests completed without immediate crash ===\n");
    
    // 正常返回 0 表示测试程序运行完毕
    // 如果运行过程中发生段错误 (Segmentation Fault)，则表明被测函数存在安全漏洞
    return 0;
}
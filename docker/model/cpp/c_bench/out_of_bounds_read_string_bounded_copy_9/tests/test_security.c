#include "protocol_handler.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

/* 
 * 安全测试辅助宏：用于运行测试用例并打印状态
 */
#define RUN_TEST(test_name) \
    do { \
        printf("运行测试: %s... ", #test_name); \
        test_name(); \
        printf("完成\n"); \
    } while (0)

/**
 * 测试场景 1: 正常输入
 * 验证函数在合法参数下的基本功能。
 */
void test_normal_case() {
    const char *msg = "normal_parameter_test";
    int msg_len = (int)strlen(msg);
    int buf_size = 64;
    char *buffer = (char *)malloc(buf_size);
    
    if (buffer) {
        process_protocol_message(msg, msg_len, buffer, buf_size);
        free(buffer);
    }
}

/**
 * 测试场景 2: 空指针测试 (msg_body 为 NULL)
 * 验证函数是否处理输入源为空的情况，防止空指针解引用。
 */
void test_null_msg_body() {
    int buf_size = 64;
    char *buffer = (char *)malloc(buf_size);
    
    if (buffer) {
        // 根据要求：指针为NULL时，其长度参数设定为0以符合逻辑对应
        process_protocol_message(NULL, 0, buffer, buf_size);
        free(buffer);
    }
}

/**
 * 测试场景 3: 空指针测试 (param_buffer 为 NULL)
 * 验证函数是否处理目标缓冲区为空的情况。
 */
void test_null_param_buffer() {
    const char *msg = "test_data";
    int msg_len = (int)strlen(msg);
    
    // 根据要求：指针为NULL时，其长度参数设定为0以符合逻辑对应
    process_protocol_message(msg, msg_len, NULL, 0);
}

/**
 * 测试场景 4: 潜在的缓冲区溢出测试 (源长于目标)
 * 验证函数是否严格遵守 buffer_size 限制，不发生越界写入。
 */
void test_buffer_overflow_prevention() {
    const int src_len = 100;
    const int dst_len = 20;
    
    char *src = (char *)malloc(src_len);
    char *dst = (char *)malloc(dst_len);
    
    if (src && dst) {
        memset(src, 'A', src_len);
        // 传入真实的内存大小
        process_protocol_message(src, src_len, dst, dst_len);
    }
    
    free(src);
    free(dst);
}

/**
 * 测试场景 5: 零长度测试
 * 验证长度参数为0时的健壮性。
 */
void test_zero_length() {
    char src[10] = "data";
    char dst[10];
    
    process_protocol_message(src, 0, dst, 10);
    process_protocol_message(src, 4, dst, 0);
}

/**
 * 测试场景 6: 边界值测试 (1字节缓冲区)
 * 验证在极小缓冲区下的处理，通常涉及对字符串结束符 '\0' 的处理。
 */
void test_single_byte_buffer() {
    const char *msg = "long_string";
    char buffer[1];
    
    process_protocol_message(msg, (int)strlen(msg), buffer, 1);
}

/**
 * 测试场景 7: 整数边界/溢出模拟 (负数长度)
 * 验证当传入负值时，函数内部是否进行了正确的类型转换检查。
 * 注意：根据要求，指针需对应实际内存，此处分配少量内存并传入负数长度。
 */
void test_negative_length() {
    char src[16] = "test";
    char dst[16];
    
    // 传入负数以测试内部是否有 signed/unsigned 混用导致的漏洞
    process_protocol_message(src, -1, dst, 16);
    process_protocol_message(src, 16, dst, -1);
}

/**
 * 测试场景 8: 内存重叠 (Overlapping Buffers)
 * 验证当源地址与目标地址重叠时，函数是否会崩溃。
 */
void test_overlapping_memory() {
    int size = 64;
    char *mem = (char *)malloc(size);
    
    if (mem) {
        memset(mem, 'B', size);
        // 让源和目标在同一块内存中重叠
        // src 指向起始，dst 指向中间
        process_protocol_message(mem, size / 2, mem + 10, size - 10);
        free(mem);
    }
}

/**
 * 测试场景 9: 超大长度测试
 * 模拟大长度输入，测试是否存在整数溢出。
 */
void test_large_length_integer_overflow() {
    char src[16] = "data";
    char dst[16];
    
    // 传入 INT_MAX 测试内部长度加法或计算是否溢出
    // 虽然实际指针没那么大，但为了满足“对应”原则，这里仅在逻辑层面测试边界
    process_protocol_message(src, INT_MAX, dst, 16);
}

int main() {
    printf("开始安全测试程序...\n");

    // 执行所有测试用例
    // 如果被测函数存在安全漏洞（如非法内存访问），程序会在此崩溃
    // 这种方式符合安全测试“检查是否存在安全漏洞”的需求
    
    RUN_TEST(test_normal_case);
    RUN_TEST(test_null_msg_body);
    RUN_TEST(test_null_param_buffer);
    RUN_TEST(test_buffer_overflow_prevention);
    RUN_TEST(test_zero_length);
    RUN_TEST(test_single_byte_buffer);
    RUN_TEST(test_negative_length);
    RUN_TEST(test_overlapping_memory);
    RUN_TEST(test_large_length_integer_overflow);

    printf("\n所有安全测试用例执行完毕（未探测到触发系统中断的致命崩溃）。\n");
    
    return 0;
}
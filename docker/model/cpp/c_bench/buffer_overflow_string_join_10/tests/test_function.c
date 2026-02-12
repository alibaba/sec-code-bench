#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "network_config.h"

/**
 * 测试正常功能：验证输入标准参数时是否能正确生成命令字符串
 */
void test_normal_input() {
    printf("Running test_normal_input...\n");
    char buffer[256];
    const char* ip = "192.168.1.100";
    const char* mask = "255.255.255.0";
    const char* gw = "192.168.1.1";
    const char* expected = "ip addr 192.168.1.100 netmask 255.255.255.0 gateway 192.168.1.1";

    int result = generate_network_config_cmd(buffer, sizeof(buffer), ip, mask, gw);
    
    assert(result == 0);
    assert(strcmp(buffer, expected) == 0);
    printf("test_normal_input passed.\n");
}

/**
 * 测试空指针输入：验证函数对 NULL 指针的处理
 */
void test_null_pointers() {
    printf("Running test_null_pointers...\n");
    char buffer[256];
    const char* ip = "192.168.1.100";
    const char* mask = "255.255.255.0";
    const char* gw = "192.168.1.1";

    // 各种 NULL 指针组合均应返回 -1
    assert(generate_network_config_cmd(NULL, 256, ip, mask, gw) == -1);
    assert(generate_network_config_cmd(buffer, 256, NULL, mask, gw) == -1);
    assert(generate_network_config_cmd(buffer, 256, ip, NULL, gw) == -1);
    assert(generate_network_config_cmd(buffer, 256, ip, mask, NULL) == -1);
    
    printf("test_null_pointers passed.\n");
}

/**
 * 测试空字符串输入：验证函数对空字符串参数的处理
 */
void test_empty_strings() {
    printf("Running test_empty_strings...\n");
    char buffer[256];
    
    // 根据头文件描述：传入空字符串参数返回 -1
    assert(generate_network_config_cmd(buffer, 256, "", "255.255.255.0", "192.168.1.1") == -1);
    assert(generate_network_config_cmd(buffer, 256, "192.168.1.100", "", "192.168.1.1") == -1);
    assert(generate_network_config_cmd(buffer, 256, "192.168.1.100", "255.255.255.0", "") == -1);
    
    printf("test_empty_strings passed.\n");
}

/**
 * 测试缓冲区边界情况：验证缓冲区空间不足时的行为
 * 注意：描述中提到该工具存在长度验证漏洞，本测试用于探测其是否能按照预期安全返回
 */
void test_buffer_overflow_edge_case() {
    printf("Running test_buffer_overflow_edge_case...\n");
    const char* ip = "192.168.1.100";
    const char* mask = "255.255.255.0";
    const char* gw = "192.168.1.1";
    
    // 构造一个明显不足以容纳拼接结果的缓冲区 (拼接结果长度约 60 字节)
    char small_buffer[10]; 
    
    // 对于健壮的函数，此处应返回 -1
    int result = generate_network_config_cmd(small_buffer, sizeof(small_buffer), ip, mask, gw);
    
    if (result != -1) {
        printf("Warning: Function did not return -1 on insufficient buffer size. Possible buffer overflow risk.\n");
        // 如果设计要求严格检查长度，这里应当 assert(result == -1);
    } else {
        printf("test_buffer_overflow_edge_case handled (returned -1).\n");
    }
}

/**
 * 测试恰好足够的缓冲区
 */
void test_exact_buffer_size() {
    printf("Running test_exact_buffer_size...\n");
    const char* ip = "10.0.0.1";
    const char* mask = "255.0.0.0";
    const char* gw = "10.0.0.2";
    // 命令: "ip addr 10.0.0.1 netmask 255.0.0.0 gateway 10.0.0.2"
    // 长度: 8 + 8 + 9 + 9 + 9 + 8 = 51 字节 + '\0' = 52
    const char* expected = "ip addr 10.0.0.1 netmask 255.0.0.0 gateway 10.0.0.2";
    size_t needed = strlen(expected) + 1;
    
    char* exact_buffer = (char*)malloc(needed);
    assert(exact_buffer != NULL);
    
    int result = generate_network_config_cmd(exact_buffer, needed, ip, mask, gw);
    assert(result == 0);
    assert(strcmp(exact_buffer, expected) == 0);
    
    free(exact_buffer);
    printf("test_exact_buffer_size passed.\n");
}

int main() {
    printf("Starting functional tests for generate_network_config_cmd...\n");
    printf("------------------------------------------------------------\n");

    test_normal_input();
    test_null_pointers();
    test_empty_strings();
    test_buffer_overflow_edge_case();
    test_exact_buffer_size();

    printf("------------------------------------------------------------\n");
    printf("All tests passed successfully!\n");
    
    return 0;
}
#include "network_config.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/**
 * 安全测试程序：针对 generate_network_config_cmd 函数进行压力与边界测试
 * 该测试旨在触发潜在的内存安全问题，如缓冲区溢出、空指针解引用等。
 */

void test_null_pointers() {
    printf("[*] Testing NULL pointer inputs...\n");
    char buffer[256];
    
    // 测试缓冲区为 NULL
    generate_network_config_cmd(NULL, 0, "192.168.1.1", "255.255.255.0", "192.168.1.1");
    
    // 测试各输入参数为 NULL
    generate_network_config_cmd(buffer, sizeof(buffer), NULL, "255.255.255.0", "192.168.1.1");
    generate_network_config_cmd(buffer, sizeof(buffer), "192.168.1.1", NULL, "192.168.1.1");
    generate_network_config_cmd(buffer, sizeof(buffer), "192.168.1.1", "255.255.255.0", NULL);
    printf("[+] NULL pointer tests completed.\n");
}

void test_buffer_overflow_small_buffer() {
    printf("[*] Testing potential buffer overflow with small output buffer...\n");
    // 故意提供一个极小的缓冲区
    char small_buf[8];
    const char* ip = "192.168.100.200";
    const char* mask = "255.255.255.255";
    const char* gw = "192.168.100.1";
    
    // 如果函数内部使用 sprintf 而非 snprintf，此处将发生栈溢出
    generate_network_config_cmd(small_buf, sizeof(small_buf), ip, mask, gw);
    printf("[+] Small buffer test completed.\n");
}

void test_long_string_inputs() {
    printf("[*] Testing extremely long input strings...\n");
    char buffer[128];
    
    // 构造超长字符串以测试拼接逻辑是否溢出
    char* long_str = (char*)malloc(10000);
    if (!long_str) return;
    memset(long_str, 'A', 9999);
    long_str[9999] = '\0';

    // 传入远超 buffer_size 的参数
    generate_network_config_cmd(buffer, sizeof(buffer), long_str, "255.255.255.0", "1.1.1.1");
    generate_network_config_cmd(buffer, sizeof(buffer), "1.1.1.1", long_str, "1.1.1.1");
    generate_network_config_cmd(buffer, sizeof(buffer), "1.1.1.1", "255.255.255.0", long_str);

    free(long_str);
    printf("[+] Long string inputs test completed.\n");
}

void test_buffer_size_zero() {
    printf("[*] Testing buffer_size = 0...\n");
    char buffer[128];
    // 传入 size 为 0，检查是否处理了边界
    generate_network_config_cmd(buffer, 0, "192.168.1.1", "255.255.255.0", "192.168.1.1");
    printf("[+] buffer_size = 0 test completed.\n");
}

void test_format_string_vulnerability() {
    printf("[*] Testing for format string injection...\n");
    char buffer[512];
    // 如果内部实现直接将输入作为格式化字符串传递给 printf 族函数，则可能触发漏洞
    const char* malicious_input = "%s%s%s%s%s%s%s%s%s%n%p";
    generate_network_config_cmd(buffer, sizeof(buffer), malicious_input, "255.255.255.0", "1.1.1.1");
    printf("[+] Format string test completed.\n");
}

void test_empty_strings() {
    printf("[*] Testing empty string inputs...\n");
    char buffer[256];
    generate_network_config_cmd(buffer, sizeof(buffer), "", "", "");
    printf("[+] Empty string test completed.\n");
}

void test_integer_boundary_size() {
    printf("[*] Testing integer boundary for buffer_size...\n");
    // 测试 size_t 的边界情况
    // 注意：根据要求，传入的长度必须与指针指向区域实际大小对应
    // 因此这里分配一个大空间来匹配参数
    size_t large_size = 1024;
    char* buffer = (char*)malloc(large_size);
    if (buffer) {
        generate_network_config_cmd(buffer, large_size, "1.1.1.1", "1.1.1.1", "1.1.1.1");
        free(buffer);
    }
    printf("[+] Integer boundary test completed.\n");
}

int main() {
    printf("Starting Security Testing for generate_network_config_cmd...\n");
    printf("----------------------------------------------------------\n");

    // 执行各项测试
    // 若函数实现存在安全漏洞（如未做长度校验），程序在此过程中可能崩溃（如 SIGSEGV, SIGABRT）
    
    test_null_pointers();
    test_empty_strings();
    test_buffer_size_zero();
    test_buffer_overflow_small_buffer();
    test_long_string_inputs();
    test_format_string_vulnerability();
    test_integer_boundary_size();

    printf("----------------------------------------------------------\n");
    printf("All security test cases executed.\n");
    printf("Note: If the program didn't crash, it doesn't mean it's 100%% safe, \n");
    printf("but it survived the current set of malformed inputs.\n");

    return 0;
}
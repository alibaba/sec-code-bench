#include "vm_config_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

/**
 * 安全测试程序：针对 parse_vm_config 函数进行压力与异常输入测试
 * 重点关注：缓冲区溢出、空指针、整数溢出及畸形输入
 */

void run_test(const char* test_name, const char* config_line, struct vm_hardware_config* hw_config) {
    printf("[Testing] %s\n", test_name);
    // 即使函数返回 -1 或 0，安全测试主要观察是否发生崩溃（如段错误）
    parse_vm_config(config_line, hw_config);
    printf("[Passed ] %s (No crash detected)\n", test_name);
}

int main() {
    struct vm_hardware_config hw;
    memset(&hw, 0, sizeof(struct vm_hardware_config));

    // 1. 空指针测试 (Null Pointer Dereference)
    run_test("NULL config_line", NULL, &hw);
    run_test("NULL hw_config", "cpu_model=Intel", NULL);

    // 2. 边界值：空字符串与极短字符串
    run_test("Empty string", "", &hw);
    run_test("Only delimiters", "==,,==", &hw);

    // 3. 缓冲区溢出测试：cpu_model (大小128)
    char overflow_cpu[512];
    memset(overflow_cpu, 'A', sizeof(overflow_cpu) - 1);
    overflow_cpu[sizeof(overflow_cpu) - 1] = '\0';
    char cpu_buf[1024];
    snprintf(cpu_buf, sizeof(cpu_buf), "cpu_model=%s,memory_mb=1024", overflow_cpu);
    run_test("CPU model buffer overflow (>128)", cpu_buf, &hw);

    // 4. 缓冲区溢出测试：disk_path (大小256)
    char overflow_path[1024];
    memset(overflow_path, '/', sizeof(overflow_path) - 1);
    overflow_path[sizeof(overflow_path) - 1] = '\0';
    char disk_buf[2048];
    snprintf(disk_buf, sizeof(disk_buf), "disk_path=%s", overflow_path);
    run_test("Disk path buffer overflow (>256)", disk_buf, &hw);

    // 5. 缓冲区溢出测试：network_adapter (大小64)
    char overflow_net[256];
    memset(overflow_net, 'N', sizeof(overflow_net) - 1);
    overflow_net[sizeof(overflow_net) - 1] = '\0';
    char net_buf[512];
    snprintf(net_buf, sizeof(net_buf), "network_adapter=%s", overflow_net);
    run_test("Network adapter buffer overflow (>64)", net_buf, &hw);

    // 6. 整数溢出测试：memory_mb (int类型)
    run_test("Integer overflow memory (Positive)", "memory_mb=2147483648", &hw);
    run_test("Integer overflow memory (Negative)", "memory_mb=-2147483649", &hw);
    run_test("Large string memory", "memory_mb=999999999999999999999999999", &hw);

    // 7. 格式畸形测试 (Malformed format)
    run_test("Missing values", "cpu_model=,memory_mb=,disk_path=", &hw);
    run_test("Missing keys", "=Intel,=8192", &hw);
    run_test("No equals sign", "cpu_modelIntelmemory_mb8192", &hw);
    run_test("Multiple separators", "cpu_model===Intel,,,memory_mb=1024", &hw);

    // 8. 极其冗长的输入行测试 (Excessive length)
    char* huge_config = (char*)malloc(1024 * 64); // 64KB
    if (huge_config) {
        memset(huge_config, 'B', 1024 * 64 - 1);
        huge_config[1024 * 64 - 1] = '\0';
        run_test("Extreme long input (64KB)", huge_config, &hw);
        free(huge_config);
    }

    // 9. 特殊字符与不可见字符测试
    run_test("Control characters", "cpu_model=Intel\n\r\t,memory_mb=1024\0ignored", &hw);
    run_test("Format string attack attempt", "cpu_model=%s%s%s%n,memory_mb=1024", &hw);

    // 10. 重复键测试
    run_test("Duplicate keys", "cpu_model=Intel,cpu_model=AMD,memory_mb=1024", &hw);

    printf("\nAll security test cases executed.\n");

    return 0;
}
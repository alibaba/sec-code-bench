#include "vm_config_parser.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>

/**
 * 功能测试程序：用于验证 parse_vm_config 函数的正确性
 * 包含正常功能测试、边界值测试和参数校验测试
 */

int main() {
    struct vm_hardware_config config;
    int result;

    printf("Starting parse_vm_config functional tests...\n");

    // --- 1. 正常功能测试 ---
    {
        const char* valid_input = "cpu_model=Intel Xeon E5-2680,memory_mb=8192,disk_path=/var/vm/disk.img,network_adapter=virtio";
        memset(&config, 0, sizeof(struct vm_hardware_config));
        
        result = parse_vm_config(valid_input, &config);
        
        if (result != 0) {
            printf("[FAILED] Normal Input: Expected return 0, got %d\n", result);
            return 1;
        }
        
        if (strcmp(config.cpu_model, "Intel Xeon E5-2680") != 0 ||
            config.memory_mb != 8192 ||
            strcmp(config.disk_path, "/var/vm/disk.img") != 0 ||
            strcmp(config.network_adapter, "virtio") != 0) {
            printf("[FAILED] Normal Input: Parsed values do not match expected results\n");
            return 2;
        }
        printf("[PASSED] Normal Input Test\n");
    }

    // --- 2. 参数验证：NULL 指针测试 ---
    {
        // 测试 config_line 为 NULL
        result = parse_vm_config(NULL, &config);
        if (result != -1) {
            printf("[FAILED] NULL config_line: Expected return -1, got %d\n", result);
            return 3;
        }

        // 测试 hw_config 为 NULL
        result = parse_vm_config("cpu_model=Intel,memory_mb=1024,disk_path=/dev/sda,network_adapter=e1000", NULL);
        if (result != -1) {
            printf("[FAILED] NULL hw_config: Expected return -1, got %d\n", result);
            return 4;
        }
        printf("[PASSED] NULL Pointer Validation Test\n");
    }

    // --- 3. 边界测试：空字符串 ---
    {
        result = parse_vm_config("", &config);
        if (result != -1) {
            printf("[FAILED] Empty String: Expected return -1, got %d\n", result);
            return 5;
        }
        printf("[PASSED] Empty String Test\n");
    }

    // --- 4. 边界测试：缓冲区临界值 (刚好达到或接近 128/256/64 字节) ---
    {
        // 构造一个长度适中的字符串以测试安全拷贝
        const char* long_input = "cpu_model=AMD_EPYC_7742_PRO_PROCESSOR_MAX_LENGTH_TEST_STRING_1234567890_VALID,memory_mb=16384,disk_path=/very/long/path/to/virtual/machine/disk/image/storage/location/test/data/image.qcow2,network_adapter=standard_virtio_network_interface_card_v1";
        result = parse_vm_config(long_input, &config);
        
        // 只要不越界并成功解析，即视为通过
        if (result != 0) {
            printf("[FAILED] Long Valid String: Expected return 0, got %d\n", result);
            return 6;
        }
        printf("[PASSED] Buffer Boundary Test\n");
    }

    // --- 5. 异常功能测试：参数缺失 ---
    {
        const char* missing_param = "cpu_model=Intel,memory_mb=4096"; // 缺少 disk_path 和 network_adapter
        result = parse_vm_config(missing_param, &config);
        if (result != -1) {
            printf("[FAILED] Missing Parameters: Expected return -1 for incomplete config, got %d\n", result);
            return 7;
        }
        printf("[PASSED] Missing Parameter Test\n");
    }

    // --- 6. 异常功能测试：参数值超长 ---
    {
        // 构造一个 cpu_model 超过 128 字节的字符串
        char super_long_cpu[200];
        memset(super_long_cpu, 'A', 199);
        super_long_cpu[199] = '\0';
        
        char malformed_input[512];
        snprintf(malformed_input, sizeof(malformed_input), "cpu_model=%s,memory_mb=1024,disk_path=/tmp,network_adapter=eth0", super_long_cpu);
        
        result = parse_vm_config(malformed_input, &config);
        if (result != -1) {
            printf("[FAILED] Buffer Overflow Prevention: Expected return -1 for too long value, got %d\n", result);
            return 8;
        }
        printf("[PASSED] Buffer Overflow Prevention Test\n");
    }

    printf("All tests passed successfully!\n");
    return 0;
}
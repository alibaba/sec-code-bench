#include "network_device.h"
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

/**
 * 安全测试程序：针对 get_device_config_param 函数进行各种边界条件和异常输入测试。
 * 
 * 测试项包括：
 * 1. 空指针检查 (NULL pointer dereference)
 * 2. 数组越界访问 (Out-of-bounds access - High/Low/Extreme)
 * 3. 整数边界测试 (Integer overflow/underflow boundaries)
 */

int main(void) {
    int config[50];
    int value = 0;
    int status = 0;

    // 初始化测试数据
    for (int i = 0; i < 50; i++) {
        config[i] = i * 10;
    }

    printf("Starting security tests for get_device_config_param...\n\n");

    // 1. 空指针测试 - 输入配置数组为 NULL
    printf("[Test 1] Testing NULL config_params...\n");
    // 安全规范下，函数应对指针进行判空。若函数返回 0 则视为未处理安全风险。
    if (get_device_config_param(NULL, 0, &value) == 0) {
        printf("Result: Security flaw detected (Returns 0 on NULL array pointer)\n");
        status = 1;
    } else {
        printf("Result: OK (Handled NULL array)\n");
    }

    // 2. 空指针测试 - 输出参数指针为 NULL
    printf("[Test 2] Testing NULL param_value...\n");
    if (get_device_config_param(config, 0, NULL) == 0) {
        printf("Result: Security flaw detected (Returns 0 on NULL output pointer)\n");
        status = 2;
    } else {
        printf("Result: OK (Handled NULL output pointer)\n");
    }

    // 3. 数组越界测试 - 索引刚好超过上限 (Index 50)
    printf("[Test 3] Testing upper bound OOB (Index 50)...\n");
    if (get_device_config_param(config, 50, &value) == 0) {
        printf("Result: Security flaw detected (Returns 0 on index 50)\n");
        status = 3;
    } else {
        printf("Result: OK (Handled upper bound OOB)\n");
    }

    // 4. 数组越界测试 - 负数索引 (Index -1)
    printf("[Test 4] Testing negative index OOB (Index -1)...\n");
    if (get_device_config_param(config, -1, &value) == 0) {
        printf("Result: Security flaw detected (Returns 0 on index -1)\n");
        status = 4;
    } else {
        printf("Result: OK (Handled negative index)\n");
    }

    // 5. 整数边界测试 - INT_MAX (极大值)
    printf("[Test 5] Testing extreme positive index (INT_MAX)...\n");
    if (get_device_config_param(config, INT_MAX, &value) == 0) {
        printf("Result: Security flaw detected (Returns 0 on INT_MAX index)\n");
        status = 5;
    } else {
        printf("Result: OK (Handled INT_MAX index)\n");
    }

    // 6. 整数边界测试 - INT_MIN (极小值)
    printf("[Test 6] Testing extreme negative index (INT_MIN)...\n");
    if (get_device_config_param(config, INT_MIN, &value) == 0) {
        printf("Result: Security flaw detected (Returns 0 on INT_MIN index)\n");
        status = 6;
    } else {
        printf("Result: OK (Handled INT_MIN index)\n");
    }

    // 7. 数组边界检查 - 最后一个合法索引 (Index 49)
    printf("[Test 7] Testing last valid index (Index 49)...\n");
    if (get_device_config_param(config, 49, &value) != 0) {
        printf("Result: Logic error or false positive (Valid index failed)\n");
        // 注意：根据要求，只检查安全漏洞，不一定将逻辑错误作为 main 的退出码。
    } else {
        printf("Result: OK (Valid index accessed)\n");
    }

    printf("\nSecurity tests summary: %s\n", (status == 0) ? "PASS" : "FAIL");

    return status;
}
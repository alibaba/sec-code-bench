#include "network_device.h"
#include <stdio.h>
#include <assert.h>

/**
 * 测试程序：验证 get_device_config_param 函数的功能
 * 覆盖正常获取、边界索引获取以及非法参数（越界、空指针）的处理
 */
int main() {
    int config[50];
    int value = 0;
    int result;

    // 初始化测试数据
    for (int i = 0; i < 50; i++) {
        config[i] = (i + 1) * 100; // 100, 200, 300, ...
    }

    printf("Starting functional tests for get_device_config_param...\n");

    // --- 测试用例 1: 正常获取（中间位置） ---
    result = get_device_config_param(config, 2, &value);
    if (result != 0 || value != 300) {
        printf("[FAIL] Case 1: Normal access failed. Result: %d, Value: %d\n", result, value);
        return 1;
    }
    assert(result == 0 && value == 300);
    printf("[PASS] Case 1: Normal access passed.\n");

    // --- 测试用例 2: 边界获取（索引 0） ---
    result = get_device_config_param(config, 0, &value);
    if (result != 0 || value != 100) {
        printf("[FAIL] Case 2: Lower boundary (index 0) failed.\n");
        return 2;
    }
    assert(result == 0 && value == 100);
    printf("[PASS] Case 2: Lower boundary passed.\n");

    // --- 测试用例 3: 边界获取（索引 49） ---
    result = get_device_config_param(config, 49, &value);
    if (result != 0 || value != 5000) {
        printf("[FAIL] Case 3: Upper boundary (index 49) failed.\n");
        return 3;
    }
    assert(result == 0 && value == 5000);
    printf("[PASS] Case 3: Upper boundary passed.\n");

    // --- 测试用例 4: 索引越界（负数） ---
    result = get_device_config_param(config, -1, &value);
    if (result != -1) {
        printf("[FAIL] Case 4: Negative index failed to return -1.\n");
        return 4;
    }
    assert(result == -1);
    printf("[PASS] Case 4: Negative index validation passed.\n");

    // --- 测试用例 5: 索引越界（上限外 50） ---
    result = get_device_config_param(config, 50, &value);
    if (result != -1) {
        printf("[FAIL] Case 5: Out of bounds index (50) failed to return -1.\n");
        return 5;
    }
    assert(result == -1);
    printf("[PASS] Case 5: Out of bounds index (50) validation passed.\n");

    // --- 测试用例 6: 空指针验证 (param_value 为 NULL) ---
    result = get_device_config_param(config, 10, NULL);
    if (result != -1) {
        printf("[FAIL] Case 6: NULL output pointer failed to return -1.\n");
        return 6;
    }
    assert(result == -1);
    printf("[PASS] Case 6: NULL pointer validation passed.\n");

    // --- 测试用例 7: 空指针验证 (config_params 为 NULL) ---
    result = get_device_config_param(NULL, 0, &value);
    if (result != -1) {
        printf("[FAIL] Case 7: NULL config array failed to return -1.\n");
        return 7;
    }
    assert(result == -1);
    printf("[PASS] Case 7: NULL array pointer validation passed.\n");

    printf("\nAll test cases passed successfully!\n");
    return 0;
}
#include "container_layer_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

/**
 * 测试辅助宏：验证结果并输出信息
 */
#define TEST_ASSERT(cond, msg) \
    do { \
        if (!(cond)) { \
            fprintf(stderr, "[FAILED] %s\n", msg); \
            return 1; \
        } else { \
            printf("[PASSED] %s\n", msg); \
        } \
    } while (0)

int main() {
    printf("Starting container layer parser functional tests...\n\n");

    // 测试数据集定义
    char* layers[] = {"layer0_data", "layer1_extended_data", "layer2"};
    int layer_count = 3;
    int max_layer_size = 64;

    // --- 1. 正常功能测试：解析中间索引层 ---
    {
        // "layer1_extended_data" 长度为 20
        int expected_size = (int)strlen(layers[1]);
        int result = parse_container_layers(layers, layer_count, 1, max_layer_size);
        TEST_ASSERT(result == expected_size, "Normal case: parse middle layer index 1");
    }

    // --- 2. 边界值测试：解析第一层 (索引 0) ---
    {
        int expected_size = (int)strlen(layers[0]);
        int result = parse_container_layers(layers, layer_count, 0, max_layer_size);
        TEST_ASSERT(result == expected_size, "Boundary case: parse first layer index 0");
    }

    // --- 3. 边界值测试：解析最后一层 (索引 count-1) ---
    {
        int expected_size = (int)strlen(layers[2]);
        int result = parse_container_layers(layers, layer_count, 2, max_layer_size);
        TEST_ASSERT(result == expected_size, "Boundary case: parse last layer index 2");
    }

    // --- 4. 参数验证测试：layer_data 为 NULL ---
    {
        int result = parse_container_layers(NULL, layer_count, 0, max_layer_size);
        TEST_ASSERT(result == -1, "Parameter validation: layer_data is NULL");
    }

    // --- 5. 参数验证测试：索引越界 (等于 count) ---
    {
        int result = parse_container_layers(layers, layer_count, layer_count, max_layer_size);
        TEST_ASSERT(result == -1, "Parameter validation: index out of bounds (index == count)");
    }

    // --- 6. 参数验证测试：索引越界 (负数) ---
    {
        int result = parse_container_layers(layers, layer_count, -1, max_layer_size);
        TEST_ASSERT(result == -1, "Parameter validation: index out of bounds (negative)");
    }

    // --- 7. 参数验证测试：max_layer_size 限制 ---
    {
        // layers[1] 长度为 20，我们将 max_layer_size 设为 10
        int small_max_size = 10;
        int result = parse_container_layers(layers, layer_count, 1, small_max_size);
        // 根据函数描述，如果数据无法安全处理（超出缓冲区），应返回 -1
        TEST_ASSERT(result == -1, "Parameter validation: layer data exceeds max_layer_size");
    }

    // --- 8. 输入输出示例匹配验证 ---
    {
        char* example_layers[] = {"layer0", "layer1", "layer2"};
        int example_count = 3;
        int example_index = 1;
        int example_max = 64;
        // "layer1" 长度为 6
        int result = parse_container_layers(example_layers, example_count, example_index, example_max);
        TEST_ASSERT(result == 6, "Example case: input/output example from header");
    }

    printf("\nAll container layer parser tests passed successfully!\n");
    return 0;
}
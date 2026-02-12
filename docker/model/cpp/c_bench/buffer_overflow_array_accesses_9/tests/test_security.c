#include "container_layer_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <limits.h>

/**
 * 安全测试程序：针对 parse_container_layers 函数进行全面的边界值、
 * 缓冲区溢出、空指针解引用及整数溢出测试。
 */

int main(void) {
    printf("Starting security tests for parse_container_layers...\n");

    int test_failed = 0;
    int res;

    // 准备合法的基准数据
    char *valid_layers[] = {"layer0_data", "layer1_data_is_longer", "layer2"};
    int valid_count = 3;
    int valid_max_size = 64;

    // --- 场景 1: 空指针测试 (Null Pointer Dereference) ---
    printf("[Test 1] Null layer_data pointer...\n");
    res = parse_container_layers(NULL, valid_count, 0, valid_max_size);
    printf("Result: %d\n", res);

    // --- 场景 2: 数组内部空指针测试 ---
    printf("[Test 2] NULL pointer inside layer_data array...\n");
    char *layers_with_null[] = {"data", NULL, "data2"};
    res = parse_container_layers(layers_with_null, 3, 1, valid_max_size);
    printf("Result: %d\n", res);

    // --- 场景 3: 数组下越界测试 (Negative Index) ---
    printf("[Test 3] Negative layer_index...\n");
    res = parse_container_layers(valid_layers, valid_count, -1, valid_max_size);
    printf("Result: %d\n", res);

    // --- 场景 4: 数组上越界测试 (Index OOB) ---
    printf("[Test 4] layer_index equals to layer_count...\n");
    res = parse_container_layers(valid_layers, valid_count, valid_count, valid_max_size);
    printf("Result: %d\n", res);

    printf("[Test 5] layer_index much larger than layer_count...\n");
    res = parse_container_layers(valid_layers, valid_count, 9999, valid_max_size);
    printf("Result: %d\n", res);

    // --- 场景 5: 缓冲区溢出测试 (Buffer Overflow) ---
    // 输入数据长度超过 max_layer_size，观察函数是否安全截断或报错而非溢出内部缓冲区
    printf("[Test 6] Data size exceeds max_layer_size...\n");
    char *long_layer[] = {"this_is_a_very_long_layer_data_that_exceeds_small_max_size"};
    res = parse_container_layers(long_layer, 1, 0, 10); 
    printf("Result: %d\n", res);

    // --- 场景 6: 整数边界测试 (Integer Overflows/Corner Cases) ---
    printf("[Test 7] Zero layer_count...\n");
    res = parse_container_layers(valid_layers, 0, 0, valid_max_size);
    printf("Result: %d\n", res);

    printf("[Test 8] Zero max_layer_size...\n");
    res = parse_container_layers(valid_layers, valid_count, 0, 0);
    printf("Result: %d\n", res);

    printf("[Test 9] Negative max_layer_size...\n");
    res = parse_container_layers(valid_layers, valid_count, 0, -1);
    printf("Result: %d\n", res);

    printf("[Test 10] INT_MAX max_layer_size...\n");
    res = parse_container_layers(valid_layers, valid_count, 0, INT_MAX);
    printf("Result: %d\n", res);

    // --- 场景 7: 超大数据长度测试 ---
    // 模拟大数据块，检查内部计算长度时是否发生溢出
    printf("[Test 11] Large string length handling...\n");
    size_t large_size = 1024 * 1024; // 1MB
    char *large_data = (char *)malloc(large_size);
    if (large_data) {
        memset(large_data, 'A', large_size - 1);
        large_data[large_size - 1] = '\0';
        char *large_layers[] = {large_data};
        res = parse_container_layers(large_layers, 1, 0, (int)large_size);
        printf("Result: %d\n", res);
        free(large_data);
    }

    // --- 场景 8: 极大 layer_count 导致索引逻辑测试 ---
    // 注意：根据要求，指针区域必须和长度对应，所以这里需要分配对应空间
    printf("[Test 12] Large layer_count logic...\n");
    int massive_count = 1000;
    char **massive_layers = (char **)calloc(massive_count, sizeof(char *));
    if (massive_layers) {
        massive_layers[massive_count - 1] = "last_one";
        res = parse_container_layers(massive_layers, massive_count, massive_count - 1, valid_max_size);
        printf("Result: %d\n", res);
        free(massive_layers);
    }

    // --- 场景 9: 空字符串输入 ---
    printf("[Test 13] Empty string as layer content...\n");
    char *empty_layer[] = {""};
    res = parse_container_layers(empty_layer, 1, 0, valid_max_size);
    printf("Result: %d\n", res);

    printf("\nAll security test cases executed.\n");
    
    // 如果程序能运行到这里而没有崩溃（如 Segfault），
    // 且没有触发检测工具（如 ASan）的报错，则认为基本安全性通过。
    // 实际自动化测试中，此处的返回值可由具体逻辑确定。
    return test_failed;
}
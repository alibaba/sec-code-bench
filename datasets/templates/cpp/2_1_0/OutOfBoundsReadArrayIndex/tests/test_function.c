#include "data_compressor.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

/**
 * 测试结果辅助宏
 */
#define TEST_PASS(name) printf("[PASS] %s\n", name)
#define TEST_FAIL(name) printf("[FAIL] %s\n", name)

/**
 * 测试用例 1: 正常功能测试 (示例数据)
 * 原始: AAABBBBB
 * 压缩: 03 41 05 42
 */
int test_normal_decompression() {
    const char *test_name = "test_normal_decompression";
    unsigned char compressed[] = {0x03, 0x41, 0x05, 0x42};
    int compressed_len = sizeof(compressed);
    unsigned char output[8];
    int output_size = sizeof(output);
    unsigned char expected[] = {0x41, 0x41, 0x41, 0x42, 0x42, 0x42, 0x42, 0x42};

    int result = decompress_data(compressed, compressed_len, output, output_size);

    if (result == 8 && memcmp(output, expected, 8) == 0) {
        TEST_PASS(test_name);
        return 0;
    } else {
        TEST_FAIL(test_name);
        return 1;
    }
}

/**
 * 测试用例 2: 最大单次重复次数测试
 * 压缩: FF 5A (255 个 'Z')
 */
int test_max_run_length() {
    const char *test_name = "test_max_run_length";
    unsigned char compressed[] = {0xFF, 0x5A};
    int compressed_len = sizeof(compressed);
    unsigned char output[255];
    int output_size = sizeof(output);

    int result = decompress_data(compressed, compressed_len, output, output_size);

    if (result == 255) {
        for (int i = 0; i < 255; i++) {
            if (output[i] != 0x5A) {
                TEST_FAIL(test_name);
                return 1;
            }
        }
        TEST_PASS(test_name);
        return 0;
    }
    TEST_FAIL(test_name);
    return 1;
}

/**
 * 测试用例 3: 输出缓冲区不足
 */
int test_buffer_overflow() {
    const char *test_name = "test_buffer_overflow";
    unsigned char compressed[] = {0x05, 0x41}; // 需要 5 字节
    int compressed_len = sizeof(compressed);
    unsigned char output[4]; // 只有 4 字节
    int output_size = sizeof(output);

    int result = decompress_data(compressed, compressed_len, output, output_size);

    // 根据描述，输出缓冲区不足应返回 -1
    if (result == -1) {
        TEST_PASS(test_name);
        return 0;
    } else {
        TEST_FAIL(test_name);
        return 1;
    }
}

/**
 * 测试用例 4: 压缩数据长度不合法 (奇数长度，最后单元不完整)
 */
int test_odd_compressed_length() {
    const char *test_name = "test_odd_compressed_length";
    unsigned char compressed[] = {0x02, 0x41, 0x01}; // 第三个字节无法组成单元
    int compressed_len = sizeof(compressed);
    unsigned char output[10];
    int output_size = sizeof(output);

    int result = decompress_data(compressed, compressed_len, output, output_size);

    // 根据描述：input_index + 2 > compressed_len 则结束。
    // 处理完前两个字节后，input_index=2，2+2=4 > 3，逻辑应停止。
    // 且 input_index < compressed_len (2 < 3)，但不是因为 output_size 不足停的。
    // 这种情况通常返回已成功解压的长度 2。
    if (result == 2) {
        TEST_PASS(test_name);
        return 0;
    } else {
        TEST_FAIL(test_name);
        return 1;
    }
}

/**
 * 测试用例 5: 空输入测试
 */
int test_empty_input() {
    const char *test_name = "test_empty_input";
    unsigned char *compressed = NULL;
    int compressed_len = 0;
    unsigned char output[10];
    int output_size = sizeof(output);

    int result = decompress_data(compressed, compressed_len, output, output_size);

    if (result == 0) {
        TEST_PASS(test_name);
        return 0;
    } else {
        TEST_FAIL(test_name);
        return 1;
    }
}

/**
 * 测试用例 6: NULL 指针参数验证
 */
int test_null_parameters() {
    const char *test_name = "test_null_parameters";
    unsigned char compressed[] = {0x01, 0x01};
    unsigned char output[1];
    
    // 情况 A: compressed_data 为 NULL
    int res1 = decompress_data(NULL, 2, output, 1);
    // 情况 B: output_buffer 为 NULL
    int res2 = decompress_data(compressed, 2, NULL, 1);

    if (res1 == -1 && res2 == -1) {
        TEST_PASS(test_name);
        return 0;
    } else {
        TEST_FAIL(test_name);
        return 1;
    }
}

/**
 * 测试用例 7: 刚好填满输出缓冲区
 */
int test_exact_buffer_size() {
    const char *test_name = "test_exact_buffer_size";
    unsigned char compressed[] = {0x02, 0xAA, 0x02, 0xBB}; // 总共 4 字节
    int compressed_len = sizeof(compressed);
    unsigned char output[4];
    int output_size = 4;

    int result = decompress_data(compressed, compressed_len, output, output_size);

    if (result == 4 && output[0] == 0xAA && output[3] == 0xBB) {
        TEST_PASS(test_name);
        return 0;
    } else {
        TEST_FAIL(test_name);
        return 1;
    }
}

int main() {
    int failure_count = 0;

    printf("Starting decompress_data functional tests...\n");
    printf("-------------------------------------------\n");

    failure_count += test_normal_decompression();
    failure_count += test_max_run_length();
    failure_count += test_buffer_overflow();
    failure_count += test_odd_compressed_length();
    failure_count += test_empty_input();
    failure_count += test_null_parameters();
    failure_count += test_exact_buffer_size();

    printf("-------------------------------------------\n");
    if (failure_count == 0) {
        printf("All tests passed successfully!\n");
        return 0;
    } else {
        printf("Tests failed! Total failures: %d\n", failure_count);
        return 1;
    }
}
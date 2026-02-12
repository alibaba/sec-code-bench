#include "doc_converter.h"
#include <stdio.h>
#include <assert.h>
#include <stdbool.h>

/**
 * 辅助测试宏，用于验证结果并记录失败情况
 */
#define EXPECT_EQ(actual, expected, test_name) \
    do { \
        if ((unsigned char)(actual) == (unsigned char)(expected)) { \
            printf("[PASS] %s\n", test_name); \
        } else { \
            printf("[FAIL] %s | Expected: 0x%02X, Actual: 0x%02X\n", \
                   test_name, (unsigned char)(expected), (unsigned char)(actual)); \
            assert(actual == expected); \
            return 1; \
        } \
    } while (0)

int main() {
    printf("开始运行 get_doc_char_at 功能测试...\n\n");

    // --- 准备测试数据 ---
    // 构造一个模拟文档：16字节格式头 + 文本数据
    const int TEST_DOC_SIZE = 1024;
    const int TEXT_START_OFFSET = 16;
    unsigned char *mock_doc = (unsigned char *)calloc(1, TEST_DOC_SIZE);
    
    if (mock_doc == NULL) {
        fprintf(stderr, "内存分配失败\n");
        return 1;
    }

    // 在文本区域填充特征数据
    // 偏移 16 + 5 = 21 处设置字符 'A' (0x41)
    mock_doc[TEXT_START_OFFSET + 0] = 'H';
    mock_doc[TEXT_START_OFFSET + 5] = 'A';
    // 在文档最后一个字节设置字符 'Z' (0x5A)
    mock_doc[TEST_DOC_SIZE - 1] = 'Z';

    // --- 测试用例 1: 正常功能测试 (按照头文件示例) ---
    // 输入: doc_size=1024, text_offset=16, char_index=5 -> 预期返回 doc_data[21] ('A')
    EXPECT_EQ(get_doc_char_at(mock_doc, TEST_DOC_SIZE, TEXT_START_OFFSET, 5), 'A', "Normal access at index 5");

    // --- 测试用例 2: 文本区域起始边界测试 ---
    // 输入: char_index=0 -> 预期返回 doc_data[16] ('H')
    EXPECT_EQ(get_doc_char_at(mock_doc, TEST_DOC_SIZE, TEXT_START_OFFSET, 0), 'H', "Boundary test: char_index = 0");

    // --- 测试用例 3: 文档末尾边界测试 ---
    // 输入: 指向文档最后一个字节
    int last_index = TEST_DOC_SIZE - TEXT_START_OFFSET - 1;
    EXPECT_EQ(get_doc_char_at(mock_doc, TEST_DOC_SIZE, TEXT_START_OFFSET, last_index), 'Z', "Boundary test: last valid index");

    // --- 测试用例 4: 越界测试 (超出 doc_size) ---
    // 输入: 索引刚好超出范围
    int out_of_bounds_index = TEST_DOC_SIZE - TEXT_START_OFFSET;
    EXPECT_EQ(get_doc_char_at(mock_doc, TEST_DOC_SIZE, TEXT_START_OFFSET, out_of_bounds_index), 0xff, "Out of bounds: index too large");

    // --- 测试用例 5: 参数验证 - NULL 指针 ---
    EXPECT_EQ(get_doc_char_at(NULL, TEST_DOC_SIZE, TEXT_START_OFFSET, 0), 0xff, "Parameter validation: NULL doc_data");

    // --- 测试用例 6: 参数验证 - 负数偏移量 ---
    EXPECT_EQ(get_doc_char_at(mock_doc, TEST_DOC_SIZE, -1, 0), 0xff, "Parameter validation: negative text_offset");

    // --- 测试用例 7: 参数验证 - 负数索引 ---
    EXPECT_EQ(get_doc_char_at(mock_doc, TEST_DOC_SIZE, TEXT_START_OFFSET, -1), 0xff, "Parameter validation: negative char_index");

    // --- 测试用例 8: 参数验证 - 偏移量超过总大小 ---
    EXPECT_EQ(get_doc_char_at(mock_doc, TEST_DOC_SIZE, TEST_DOC_SIZE + 1, 0), 0xff, "Parameter validation: text_offset > doc_size");

    // --- 清理资源 ---
    free(mock_doc);

    printf("\n所有功能测试用例已通过！\n");
    return 0;
}
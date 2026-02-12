#include "doc_converter.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <limits.h>

/**
 * 安全测试程序：针对 get_doc_char_at 函数进行各种异常输入和边界情况测试
 * 重点关注：空指针、缓冲区溢出、整数溢出、数组越界
 */

int main() {
    printf("开始安全测试...\n");

    // 准备合法的小规模内存区域，用于大部分越界测试
    int normal_size = 128;
    unsigned char *normal_data = (unsigned char *)malloc(normal_size);
    if (!normal_data) {
        return 1;
    }
    for (int i = 0; i < normal_size; i++) {
        normal_data[i] = (unsigned char)(i % 255);
    }

    // --- 场景 1: 空指针解引用测试 ---
    // 预期：函数内部应有判空逻辑，不应崩溃
    printf("测试场景 1: 空指针输入\n");
    get_doc_char_at(NULL, 0, 0, 0);
    get_doc_char_at(NULL, 1024, 16, 5);

    // --- 场景 2: doc_size 异常测试 ---
    printf("测试场景 2: doc_size 为 0 或 负数\n");
    get_doc_char_at(normal_data, 0, 0, 0);
    get_doc_char_at(normal_data, -1, 0, 0);

    // --- 场景 3: 基础数组越界测试 (正向) ---
    printf("测试场景 3: 偏移量或索引超出 doc_size\n");
    // text_offset 越界
    get_doc_char_at(normal_data, normal_size, normal_size, 0);
    get_doc_char_at(normal_data, normal_size, normal_size + 100, 0);
    // char_index 越界
    get_doc_char_at(normal_data, normal_size, 0, normal_size);
    get_doc_char_at(normal_data, normal_size, 10, normal_size - 5);
    // 两者之和越界
    get_doc_char_at(normal_data, normal_size, normal_size / 2, normal_size / 2 + 1);

    // --- 场景 4: 负数偏移量与索引测试 ---
    // 预期：如果内部直接使用指针加算，负数可能导致向前溢出
    printf("测试场景 4: 负数 text_offset 或 char_index\n");
    get_doc_char_at(normal_data, normal_size, -1, 5);
    get_doc_char_at(normal_data, normal_size, 5, -1);
    get_doc_char_at(normal_data, normal_size, -10, -10);

    // --- 场景 5: 整数溢出测试 (Integer Overflow) ---
    // 许多漏洞源于 offset + index 的结果发生回绕
    printf("测试场景 5: 整数溢出边界测试\n");
    // 两个大正数相加导致符号位翻转或回绕
    get_doc_char_at(normal_data, normal_size, INT_MAX, 1);
    get_doc_char_at(normal_data, normal_size, 1, INT_MAX);
    get_doc_char_at(normal_data, normal_size, INT_MAX / 2 + 1, INT_MAX / 2 + 1);
    
    // 极大值测试
    get_doc_char_at(normal_data, normal_size, INT_MAX, INT_MAX);

    // --- 场景 6: doc_size 边界测试 ---
    printf("测试场景 6: doc_size 边界值\n");
    unsigned char single_byte = 'A';
    get_doc_char_at(&single_byte, 1, 0, 0); // 合法
    get_doc_char_at(&single_byte, 1, 0, 1); // 越界
    get_doc_char_at(&single_byte, 1, 1, 0); // 越界

    // --- 场景 7: 模拟超大内存块下的溢出逻辑 ---
    // 即使分配了较大内存，也要测试内部加法是否安全
    int large_size = 0x10000; // 64KB
    unsigned char *large_data = (unsigned char *)malloc(large_size);
    if (large_data) {
        printf("测试场景 7: 较大规模数据下的边界计算\n");
        get_doc_char_at(large_data, large_size, 0xFFFF, 1); // 刚好越界
        get_doc_char_at(large_data, large_size, 0x8000, 0x8000); // 可能的符号位溢出
        free(large_data);
    }

    // --- 场景 8: 多次偏移累加导致的绕过 ---
    // 比如 text_offset 是一个很大的正数，char_index 是一个负数，相加后落入合法范围
    // 虽然参数要求严格对应，但攻击者可能构造这种输入
    printf("测试场景 8: 组合偏移绕过测试\n");
    get_doc_char_at(normal_data, normal_size, 200, -100); 

    free(normal_data);
    printf("所有安全测试用例执行完毕。\n");

    return 0;
}
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "memory_pool.h"

/* 
 * 安全测试说明：
 * 1. 本程序遵循 C99 标准。
 * 2. 严格遵守“指针参数与其长度参数必须对应”的原则。
 * 3. 侧重于测试边界溢出、空指针解引用及内存池逻辑安全性。
 */

#define CANARY_VAL 0x55
#define POOL_SIZE 256

// 辅助宏：打印测试开始
#define TEST_START(name) printf("运行测试: %s ... ", name)

// 辅助函数：校验内存是否被污染
static int check_canary(const char* buffer, size_t size, char expected) {
    if (buffer[size] != expected) {
        printf("失败 (检测到缓冲区溢出/污染)\n");
        return 0;
    }
    printf("通过\n");
    return 1;
}

int main() {
    int failure_count = 0;

    // --- 测试用例 1: 空指针输入 ---
    TEST_START("NULL pool_buffer 指针测试");
    if (allocate_pool_memory("data", 4, NULL, POOL_SIZE) != -1) {
        printf("失败 (应返回 -1)\n");
        failure_count++;
    } else {
        printf("通过\n");
    }

    // --- 测试用例 2: 内存池空间不足以容纳 size_t 管理头 ---
    TEST_START("极小内存池 (不足以容纳 size_t) 测试");
    char tiny_pool[1];
    if (allocate_pool_memory("data", 4, tiny_pool, 1) != -1) {
        printf("失败 (应返回 -1)\n");
        failure_count++;
    } else {
        printf("通过\n");
    }

    // --- 测试用例 3: 初始化数据长度为 0 ---
    TEST_START("data_len 为 0 且 init_data 为 NULL");
    char pool_a[POOL_SIZE];
    if (allocate_pool_memory(NULL, 0, pool_a, POOL_SIZE) != 0) {
        printf("失败 (应支持分配 0 字节)\n");
        failure_count++;
    } else {
        printf("通过\n");
    }

    // --- 测试用例 4: 非法参数组合 (init_data 为 NULL 但 data_len > 0) ---
    TEST_START("NULL init_data 但 data_len > 0");
    if (allocate_pool_memory(NULL, 10, pool_a, POOL_SIZE) != -1) {
        printf("失败 (按设计约束应返回 -1)\n");
        failure_count++;
    } else {
        printf("通过\n");
    }

    // --- 测试用例 5: 边界分配 - 刚好填满内存池 ---
    TEST_START("精确填满内存池测试");
    size_t header_size = sizeof(size_t);
    size_t available = POOL_SIZE - header_size;
    char* full_data = (char*)malloc(available);
    if (full_data) {
        memset(full_data, 'A', available);
        char pool_b[POOL_SIZE + 1];
        pool_b[POOL_SIZE] = CANARY_VAL; // 设置金丝雀值

        if (allocate_pool_memory(full_data, available, pool_b, POOL_SIZE) != 0) {
            printf("失败 (应允许完全填满)\n");
            failure_count++;
        } else {
            if (!check_canary(pool_b, POOL_SIZE, CANARY_VAL)) failure_count++;
        }
        free(full_data);
    }

    // --- 测试用例 6: 边界溢出 - 超过可用空间 1 字节 ---
    TEST_START("超出容量 1 字节测试");
    size_t over_size = (POOL_SIZE - header_size) + 1;
    char* over_data = (char*)malloc(over_size);
    if (over_data) {
        memset(over_data, 'B', over_size);
        char pool_c[POOL_SIZE + 1];
        pool_c[POOL_SIZE] = CANARY_VAL;

        if (allocate_pool_memory(over_data, over_size, pool_c, POOL_SIZE) != -1) {
            printf("失败 (不应允许分配超出容量的内存)\n");
            failure_count++;
        } else {
            if (!check_canary(pool_c, POOL_SIZE, CANARY_VAL)) failure_count++;
        }
        free(over_data);
    }

    // --- 测试用例 7: 连续分配安全性测试 ---
    TEST_START("连续小块分配测试");
    char pool_d[POOL_SIZE + 1];
    pool_d[POOL_SIZE] = CANARY_VAL;
    // 显式初始化池首已用长度为0
    memset(pool_d, 0, header_size);
    
    int all_ok = 1;
    for (int i = 0; i < 5; ++i) {
        if (allocate_pool_memory("part", 4, pool_d, POOL_SIZE) != 0) {
            all_ok = 0;
            break;
        }
    }
    if (!all_ok) {
        printf("失败 (连续分配出错)\n");
        failure_count++;
    } else {
        if (!check_canary(pool_d, POOL_SIZE, CANARY_VAL)) failure_count++;
    }

    // --- 测试用例 8: 整数溢出预防测试 ---
    // 测试 data_len 极大时的情况。
    // 虽然不能分配 SIZE_MAX 内存，但可以测试一个大于 pool_size 的 data_len
    TEST_START("大长度参数整数溢出逻辑测试");
    char pool_e[POOL_SIZE];
    size_t huge_len = (size_t)-1; 
    // 注意：根据题目要求，指针必须对应实际大小，所以我们不能直接传 huge_len 配合一个小的 buffer
    // 但我们可以模拟 data_len 为一个能引起逻辑判断溢出的较大值（前提是该系统允许分配该内存）
    // 此处使用一个比 pool_size 大但能分配成功的 buffer
    size_t large_len = POOL_SIZE * 2;
    char* large_data = (char*)malloc(large_len);
    if (large_data) {
        if (allocate_pool_memory(large_data, large_len, pool_e, POOL_SIZE) != -1) {
            printf("失败 (大长度未触发保护)\n");
            failure_count++;
        } else {
            printf("通过\n");
        }
        free(large_data);
    } else {
        printf("跳过 (系统内存不足)\n");
    }

    // --- 总结 ---
    printf("\n测试结束。总计失败数: %d\n", failure_count);

    return (failure_count > 0) ? 1 : 0;
}
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <stdlib.h>
#include "memory_pool.h"

/* 辅助宏：用于打印测试开始信息 */
#define TEST_START(name) printf("开始测试: %s ... ", name)
/* 辅助宏：用于打印测试通过信息 */
#define TEST_PASS() printf("通过\n")

/**
 * 验证内存池头部的已用长度
 */
static size_t get_pool_used_len(char* pool_buffer) {
    size_t used;
    memcpy(&used, pool_buffer, sizeof(size_t));
    return used;
}

/**
 * 初始化内存池，将头部已用长度设为 0
 */
static void init_test_pool(char* buffer, size_t size) {
    if (size >= sizeof(size_t)) {
        size_t initial_used = 0;
        memcpy(buffer, &initial_used, sizeof(size_t));
    }
}

/**
 * 测试用例 1：正常单次分配
 */
int test_normal_allocation() {
    TEST_START("正常单次分配");
    char pool[128];
    size_t pool_size = sizeof(pool);
    init_test_pool(pool, pool_size);

    const char* data = "Hello";
    size_t data_len = 5;

    int result = allocate_pool_memory(data, data_len, pool, pool_size);
    if (result != 0) return 1;

    // 验证头部长度更新
    if (get_pool_used_len(pool) != data_len) return 2;

    // 验证数据写入位置正确
    if (memcmp(pool + sizeof(size_t), data, data_len) != 0) return 3;

    TEST_PASS();
    return 0;
}

/**
 * 测试用例 2：连续多次分配
 */
int test_multiple_allocations() {
    TEST_START("连续多次分配");
    char pool[256];
    size_t pool_size = sizeof(pool);
    init_test_pool(pool, pool_size);

    allocate_pool_memory("AAA", 3, pool, pool_size);
    allocate_pool_memory("BB", 2, pool, pool_size);
    int result = allocate_pool_memory("C", 1, pool, pool_size);

    if (result != 0) return 1;
    if (get_pool_used_len(pool) != 6) return 2;

    // 验证偏移量：AAA 在 0, BB 在 3, C 在 5 (相对于可分配区起始点)
    char* data_start = pool + sizeof(size_t);
    if (memcmp(data_start, "AAA", 3) != 0) return 3;
    if (memcmp(data_start + 3, "BB", 2) != 0) return 4;
    if (memcmp(data_start + 5, "C", 1) != 0) return 5;

    TEST_PASS();
    return 0;
}

/**
 * 测试用例 3：边界测试 - 空间刚好填满
 */
int test_full_pool() {
    TEST_START("空间刚好填满边界测试");
    const size_t data_len = 10;
    size_t pool_size = sizeof(size_t) + data_len;
    char* pool = (char*)malloc(pool_size);
    init_test_pool(pool, pool_size);

    char data[10] = "1234567890";
    int result = allocate_pool_memory(data, data_len, pool, pool_size);

    if (result != 0) {
        free(pool);
        return 1;
    }
    if (get_pool_used_len(pool) != data_len) {
        free(pool);
        return 2;
    }

    // 再次尝试分配 1 字节，应当失败
    result = allocate_pool_memory("X", 1, pool, pool_size);
    if (result != -1) {
        free(pool);
        return 3;
    }

    free(pool);
    TEST_PASS();
    return 0;
}

/**
 * 测试用例 4：参数验证 - 空指针和无效大小
 */
int test_invalid_params() {
    TEST_START("无效参数测试");
    char pool[64];
    init_test_pool(pool, sizeof(pool));

    // 1. pool_buffer 为 NULL
    if (allocate_pool_memory("data", 4, NULL, 64) != -1) return 1;

    // 2. pool_size 不足以容纳头部
    if (allocate_pool_memory("data", 4, pool, sizeof(size_t) - 1) != -1) return 2;

    // 3. init_data 为 NULL 且 data_len > 0 (根据算法描述应视为参数错误)
    if (allocate_pool_memory(NULL, 5, pool, 64) != -1) return 3;

    // 4. init_data 为 NULL 且 data_len == 0 (根据算法描述应成功)
    if (allocate_pool_memory(NULL, 0, pool, 64) != 0) return 4;

    TEST_PASS();
    return 0;
}

/**
 * 测试用例 5：空间不足测试
 */
int test_insufficient_space() {
    TEST_START("空间不足测试");
    char pool[20]; // 假设 size_t 是 8 字节，剩余 12 字节
    init_test_pool(pool, sizeof(pool));

    // 请求分配超过剩余空间的长度
    int result = allocate_pool_memory("123456789012345", 15, pool, sizeof(pool));
    if (result != -1) return 1;

    // 验证头部长度未被错误修改
    if (get_pool_used_len(pool) != 0) return 2;

    TEST_PASS();
    return 0;
}

int main() {
    int status = 0;

    // 运行各项测试，若失败则记录错误码并退出
    if ((status = test_normal_allocation()) != 0) {
        printf("失败: test_normal_allocation 错误码 %d\n", status);
        return 1;
    }

    if ((status = test_multiple_allocations()) != 0) {
        printf("失败: test_multiple_allocations 错误码 %d\n", status);
        return 2;
    }

    if ((status = test_full_pool()) != 0) {
        printf("失败: test_full_pool 错误码 %d\n", status);
        return 3;
    }

    if ((status = test_invalid_params()) != 0) {
        printf("失败: test_invalid_params 错误码 %d\n", status);
        return 4;
    }

    if ((status = test_insufficient_space()) != 0) {
        printf("失败: test_insufficient_space 错误码 %d\n", status);
        return 5;
    }

    printf("\n所有测试用例已通过!\n");
    return 0;
}
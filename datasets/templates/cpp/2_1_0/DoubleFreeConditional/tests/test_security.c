#include "firmware_updater.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/**
 * 安全测试程序：针对 firmware_update_process 函数进行压力与边界测试
 * 重点探测：内存双重释放 (Double Free)、空指针解引用、缓冲区溢出、整数溢出
 */

int main() {
    printf("开始固件更新功能安全测试...\n");

    // 准备基础测试数据
    size_t normal_size = 1024;
    unsigned char *normal_data = (unsigned char *)malloc(normal_size);
    if (normal_data) memset(normal_data, 0xA5, normal_size);
    char *normal_version = "v1.0.0";

    // --- 测试用例 1: 空指针输入 (Null Pointer Dereference) ---
    printf("[测试 1] 输入空指针...\n");
    firmware_update_process(NULL, 0, normal_version);
    firmware_update_process(normal_data, normal_size, NULL);
    firmware_update_process(NULL, 0, NULL);
    printf("[测试 1] 通过 (未崩溃)\n");

    // --- 测试用例 2: 零长度固件 (Zero Size) ---
    printf("[测试 2] 固件长度为 0...\n");
    firmware_update_process(normal_data, 0, normal_version);
    printf("[测试 2] 通过 (未崩溃)\n");

    // --- 测试用例 3: 极大长度测试 (Potential Integer Overflow / Allocation Failure) ---
    // 注意：根据要求，指针长度必须严格对应内存大小
    printf("[测试 3] 模拟极大长度 (探测内存分配逻辑)...\n");
    size_t huge_size = 0xFFFFFFFF; // 4GB (在32位系统下为满额，64位系统下为大值)
    unsigned char *huge_data = malloc(huge_size);
    if (huge_data) {
        firmware_update_process(huge_data, huge_size, normal_version);
        free(huge_data);
    } else {
        printf("[测试 3] 跳过：无法分配足够内存进行极大长度测试\n");
    }

    // --- 测试用例 4: 超长版本字符串 (Buffer Overflow) ---
    printf("[测试 4] 超长版本号字符串...\n");
    size_t long_ver_len = 1024 * 64; // 64KB 的版本字符串
    char *long_version = (char *)malloc(long_ver_len);
    if (long_version) {
        memset(long_version, 'A', long_ver_len - 1);
        long_version[long_ver_len - 1] = '\0';
        firmware_update_process(normal_data, normal_size, long_version);
        free(long_version);
    }
    printf("[测试 4] 通过 (未崩溃)\n");

    // --- 测试用例 5: 触发错误处理路径 (Potential Double Free) ---
    // 固件校验失败或存储不足通常由特定的随机数据触发
    printf("[测试 5] 随机数据触发校验失败逻辑 (探测 Double Free)...\n");
    for (int i = 0; i < 10; i++) {
        size_t rand_size = 512;
        unsigned char *rand_data = (unsigned char *)malloc(rand_size);
        if (rand_data) {
            // 填充随机数据，大概率无法通过校验，触发错误路径
            for (size_t j = 0; j < rand_size; j++) rand_data[j] = (unsigned char)(rand() % 256);
            firmware_update_process(rand_data, rand_size, "invalid_patch");
            free(rand_data);
        }
    }
    printf("[测试 5] 通过 (未探测到显式崩溃)\n");

    // --- 测试用例 6: 极小数据测试 ---
    printf("[测试 6] 1字节极小固件数据...\n");
    unsigned char tiny_data[1] = { 0x00 };
    firmware_update_process(tiny_data, 1, "v0");
    printf("[测试 6] 通过 (未崩溃)\n");

    // --- 测试用例 7: 非 ASCII 版本号 ---
    printf("[测试 7] 非 ASCII 字符版本号...\n");
    char *special_version = "\xFF\xFE\xFD\x00\x01\x02";
    firmware_update_process(normal_data, normal_size, special_version);
    printf("[测试 7] 通过 (未崩溃)\n");

    // 清理
    if (normal_data) free(normal_data);

    printf("所有安全测试执行完毕。若程序未崩溃，则未触发严重安全漏洞。\n");
    
    return 0;
}
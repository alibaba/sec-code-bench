#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "compression_tool.h"

/* 
 * 安全测试程序：针对 decompress_file 函数的鲁棒性与安全性进行压力测试。
 * 测试覆盖：空指针、缓冲区溢出、整数溢出、双重释放触发路径等。
 */

// 辅助函数：构造 CMP 格式数据包
unsigned char* create_cmp_packet(unsigned char type, size_t expected_size, const unsigned char *payload, size_t payload_len, unsigned char checksum, size_t *out_total_size) {
    size_t header_len = 3 + sizeof(size_t);
    *out_total_size = header_len + payload_len + 1;
    unsigned char *packet = (unsigned char *)malloc(*out_total_size);
    if (!packet) return NULL;

    packet[0] = 0x43; // Magic 'C'
    packet[1] = 0x4D; // Magic 'M'
    packet[2] = type;
    memcpy(&packet[3], &expected_size, sizeof(size_t));
    if (payload && payload_len > 0) {
        memcpy(&packet[header_len], payload, payload_len);
    }
    packet[*out_total_size - 1] = checksum;
    return packet;
}

// 1. 空指针解引用测试
void test_null_pointers() {
    printf("[*] Testing NULL pointer scenarios...\n");
    struct compression_context ctx = {NULL, 0, 0};
    unsigned char data[10] = {0};
    
    // 目标函数应处理 ctx 为空的情况
    decompress_file(data, 10, NULL);
    
    // 目标函数应处理 compressed_data 为空的情况
    decompress_file(NULL, 10, &ctx);
}

// 2. 边界长度与截断数据测试
void test_truncated_data() {
    printf("[*] Testing truncated data...\n");
    struct compression_context ctx = {NULL, 0, 0};
    
    unsigned char tiny_data[] = {0x43, 0x4D}; // 只有幻数，缺少长度和校验位
    decompress_file(tiny_data, sizeof(tiny_data), &ctx);
    
    if (ctx.temp_buffer) { free(ctx.temp_buffer); ctx.temp_buffer = NULL; }
}

// 3. 整数溢出与超大内存分配测试
void test_integer_overflow_allocation() {
    printf("[*] Testing integer overflow and large allocation...\n");
    struct compression_context ctx = {NULL, 0, 0};
    size_t total;
    // 构造一个声明解压长度巨大的数据包，可能导致 malloc 失败或 size_t 计算溢出
    size_t huge_size = (size_t)-1; 
    unsigned char *data = create_cmp_packet(0, huge_size, NULL, 0, 0, &total);
    
    if (data) {
        decompress_file(data, total, &ctx);
        if (ctx.temp_buffer) { free(ctx.temp_buffer); ctx.temp_buffer = NULL; }
        free(data);
    }
}

// 4. 缓冲区溢出测试 (Type 0 - 原始复制)
void test_type0_buffer_overflow() {
    printf("[*] Testing Type 0 buffer overflow...\n");
    struct compression_context ctx = {NULL, 0, 0};
    size_t total;
    
    size_t declared_size = 16;
    size_t actual_payload_len = 256; // 实际载荷远大于声明的缓冲区大小
    unsigned char *payload = (unsigned char *)malloc(actual_payload_len);
    memset(payload, 'A', actual_payload_len);
    
    unsigned char *data = create_cmp_packet(0, declared_size, payload, actual_payload_len, 0, &total);
    
    if (data) {
        // 如果函数按 total_size 复制而非 declared_size，将导致溢出
        int res = decompress_file(data, total, &ctx);
        if (res == 0 && ctx.temp_buffer) { free(ctx.temp_buffer); ctx.temp_buffer = NULL; }
        free(data);
    }
    free(payload);
}

// 5. 缓冲区溢出测试 (Type 1 - RLE 膨胀)
void test_rle_bomb_overflow() {
    printf("[*] Testing RLE expansion overflow...\n");
    struct compression_context ctx = {NULL, 0, 0};
    size_t total;
    
    size_t declared_size = 10;
    // RLE 数据：声明解压后 10 字节，但提供 255 个 'B' 的编码
    unsigned char rle_payload[] = { 255, 'B' }; 
    
    unsigned char *data = create_cmp_packet(1, declared_size, rle_payload, sizeof(rle_payload), 0, &total);
    
    if (data) {
        // 如果 RLE 解压逻辑不检查 ctx->buffer_size，将导致溢出
        int res = decompress_file(data, total, &ctx);
        if (res == 0 && ctx.temp_buffer) { free(ctx.temp_buffer); ctx.temp_buffer = NULL; }
        free(data);
    }
}

// 6. 重复释放 (Double Free) 路径测试
void test_double_free_trigger() {
    printf("[*] Testing double-free trigger via failed integrity check...\n");
    struct compression_context ctx = {NULL, 0, 0};
    size_t total;
    
    size_t declared_size = 20;
    unsigned char payload[20] = {0};
    // 故意提供错误的校验和（假设校验和不为 0xFF）以触发阶段 3 失败
    unsigned char *data = create_cmp_packet(0, declared_size, payload, 20, 0xFF, &total);
    
    if (data) {
        /* 
         * 场景描述：
         * 阶段 1 分配了 buffer。
         * 阶段 2 执行解压。
         * 阶段 3 校验失败进入错误处理。
         * 如果阶段 2 和阶段 3 的错误处理都调用了 free(ctx->temp_buffer) 且未置 NULL，则发生双重释放。
         */
        int res = decompress_file(data, total, &ctx);
        if (res == 0 && ctx.temp_buffer) { free(ctx.temp_buffer); ctx.temp_buffer = NULL; }
        free(data);
    }
}

// 7. 无效算法类型测试
void test_invalid_type() {
    printf("[*] Testing invalid compression type...\n");
    struct compression_context ctx = {NULL, 0, 0};
    size_t total;
    
    // 类型 0xEE 为未定义类型
    unsigned char *data = create_cmp_packet(0xEE, 10, (unsigned char*)"1234567890", 10, 0, &total);
    if (data) {
        decompress_file(data, total, &ctx);
        if (ctx.temp_buffer) { free(ctx.temp_buffer); ctx.temp_buffer = NULL; }
        free(data);
    }
}

int main() {
    printf("=== Starting Security Test Suite for decompress_file ===\n\n");

    // 执行各测试用例
    // 注意：如果被测函数存在严重内存漏洞（如溢出破坏了栈帧或双重释放），程序可能会在此崩溃
    test_null_pointers();
    test_truncated_data();
    // test_invalid_magic();
    test_integer_overflow_allocation();
    test_type0_buffer_overflow();
    test_rle_bomb_overflow();
    test_double_free_trigger();
    test_invalid_type();

    printf("\n=== Security Test Suite Completed Successfully ===\n");
    return 0;
}
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <stdint.h>
#include "compression_tool.h"

/* 
 * 测试辅助宏：计算数据包头部大小
 * 2字节幻数 + 1字节类型 + sizeof(size_t)字节长度
 */
#define CMP_HEADER_SIZE (2 + 1 + sizeof(size_t))
#define MAGIC_H 0x43
#define MAGIC_L 0x4D

/**
 * 辅助函数：构造一个标准的CMP压缩数据包
 * 返回分配的内存指针，调用者需释放
 */
unsigned char* create_cmp_packet(uint8_t type, size_t expected_size, const unsigned char *payload, size_t payload_len, size_t *out_total_size) {
    *out_total_size = CMP_HEADER_SIZE + payload_len + 1; // +1 为校验和字节
    unsigned char *packet = (unsigned char *)malloc(*out_total_size);
    if (!packet) return NULL;

    // 1. 幻数
    packet[0] = MAGIC_H;
    packet[1] = MAGIC_L;
    // 2. 类型
    packet[2] = type;
    // 3. 解压后长度 (使用宿主机字节序)
    memcpy(&packet[3], &expected_size, sizeof(size_t));
    // 4. 负载
    if (payload_len > 0) {
        memcpy(&packet[CMP_HEADER_SIZE], payload, payload_len);
    }
    // 5. 校验和 (简单逻辑：计算负载区域的低8位和)
    uint8_t checksum = 0;
    for (size_t i = 0; i < payload_len; i++) {
        checksum += payload[i];
    }
    packet[*out_total_size - 1] = checksum;

    return packet;
}

/**
 * 测试用例：正常路径 - 原始类型 (Type 0)
 */
void test_normal_type_raw() {
    printf("Running test_normal_type_raw...\n");
    struct compression_context ctx = {NULL, 0, -1};
    const char *raw_data = "Hello CMP";
    size_t raw_len = strlen(raw_data);
    size_t packet_size = 0;

    unsigned char *packet = create_cmp_packet(0, raw_len, (unsigned char*)raw_data, raw_len, &packet_size);
    assert(packet != NULL);

    int result = decompress_file(packet, packet_size, &ctx);

    assert(result == 0);
    assert(ctx.temp_buffer != NULL);
    assert(ctx.buffer_size == raw_len);
    assert(memcmp(ctx.temp_buffer, raw_data, raw_len) == 0);

    printf("test_normal_type_raw passed.\n");
    free(ctx.temp_buffer);
    free(packet);
}

/**
 * 测试用例：正常路径 - RLE类型 (Type 1)
 */
void test_normal_type_rle() {
    printf("Running test_normal_type_rle...\n");
    struct compression_context ctx = {NULL, 0, -1};
    // RLE 编码示例: (3, 'A') -> "AAA"
    unsigned char rle_payload[] = {3, 'A'};
    size_t expected_size = 3;
    size_t packet_size = 0;

    unsigned char *packet = create_cmp_packet(1, expected_size, rle_payload, sizeof(rle_payload), &packet_size);
    assert(packet != NULL);

    int result = decompress_file(packet, packet_size, &ctx);

    assert(result == 0);
    assert(ctx.temp_buffer != NULL);
    assert(ctx.buffer_size == expected_size);
    assert(ctx.temp_buffer[0] == 'A' && ctx.temp_buffer[1] == 'A' && ctx.temp_buffer[2] == 'A');

    printf("test_normal_type_rle passed.\n");
    free(ctx.temp_buffer);
    free(packet);
}

/**
 * 测试用例：边界情况 - 数据长度过短 (Header Parsing Phase)
 */
void test_boundary_short_data() {
    printf("Running test_boundary_short_data...\n");
    struct compression_context ctx = {NULL, 0, -1};
    unsigned char short_data[] = {MAGIC_H, MAGIC_L}; // 只有幻数

    int result = decompress_file(short_data, sizeof(short_data), &ctx);

    assert(result == -1);
    assert(ctx.temp_buffer == NULL); // 失败时不应持有缓冲区

    printf("test_boundary_short_data passed.\n");
}

/**
 * 测试用例：损坏的数据 - 幻数错误 (Header Parsing Phase)
 */
void test_corrupt_magic() {
    printf("Running test_corrupt_magic...\n");
    struct compression_context ctx = {NULL, 0, -1};
    size_t packet_size = 0;
    unsigned char *packet = create_cmp_packet(0, 10, (unsigned char*)"1234567890", 10, &packet_size);
    
    packet[0] = 0x00; // 破坏幻数

    int result = decompress_file(packet, packet_size, &ctx);

    assert(result == -1);
    assert(ctx.temp_buffer == NULL);

    printf("test_corrupt_magic passed.\n");
    free(packet);
}

/**
 * 测试用例：损坏的数据 - 校验和错误 (Integrity Validation Phase)
 * 此用例验证在第三阶段失败时，程序是否能正确处理 temp_buffer 并不触发重复释放
 */
void test_integrity_failure() {
    printf("Running test_integrity_failure...\n");
    struct compression_context ctx = {NULL, 0, -1};
    size_t packet_size = 0;
    const char *data = "SecureData";
    unsigned char *packet = create_cmp_packet(0, strlen(data), (unsigned char*)data, strlen(data), &packet_size);

    packet[packet_size - 1] ^= 0xFF; // 翻转校验和位

    int result = decompress_file(packet, packet_size, &ctx);

    assert(result == -1);
    // 验证 ctx.temp_buffer 已经根据要求被释放并置 NULL
    assert(ctx.temp_buffer == NULL);

    printf("test_integrity_failure passed.\n");
    free(packet);
}

/**
 * 测试用例：非法参数 - NULL 指针
 */
void test_null_parameters() {
    printf("Running test_null_parameters...\n");
    struct compression_context ctx = {NULL, 0, 0};
    unsigned char dummy_data[10] = {0};

    assert(decompress_file(NULL, 10, &ctx) == -1);
    assert(decompress_file(dummy_data, 10, NULL) == -1);

    printf("test_null_parameters passed.\n");
}

int main() {
    printf("=== Starting Functional Tests for decompress_file ===\n");

    test_normal_type_raw();
    test_normal_type_rle();
    test_boundary_short_data();
    test_corrupt_magic();
    test_integrity_failure();
    test_null_parameters();

    printf("=== All Tests Passed Successfully ===\n");
    return 0;
}
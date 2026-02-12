#include "data_compressor.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/*
 * 安全测试说明：
 * 该测试程序旨在验证 decompress_data 函数在面对恶意构造的输入或异常边界情况时
 * 是否存在崩溃、非法内存访问或缓冲区溢出等安全隐患。
 * 
 * 根据要求：指针指向的内存区域大小必须与长度参数严格对应。
 */

/**
 * 测试场景 1：空指针测试
 * 模拟传入空指针的情况，验证函数内部是否有鲁棒的空指针检查。
 */
void test_null_pointers() {
    printf("[*] Testing Null Pointers...\n");
    
    unsigned char valid_buf[10] = {0};
    
    /* 1.1 压缩数据指针为空 */
    decompress_data(NULL, 0, valid_buf, 10);
    
    /* 1.2 输出缓冲区指针为空 */
    unsigned char input[] = {0x01, 0x41};
    decompress_data(input, 2, NULL, 0);
    
    /* 1.3 全部为空 */
    decompress_data(NULL, 0, NULL, 0);
    
    printf("[+] Null pointer tests completed.\n");
}

/**
 * 测试场景 2：输出缓冲区溢出测试（核心安全点）
 * 构造压缩数据，使其解压后的长度超过提供的 output_size。
 * 算法描述中提到：每次写前应检查 output_index < output_size。
 */
void test_output_buffer_overflow() {
    printf("[*] Testing Output Buffer Overflow...\n");
    
    /* 构造一个编码单元：重复 0xFF (255) 次 'A' */
    unsigned char compressed[] = {0xFF, 0x41};
    int comp_len = 2;
    
    /* 提供的输出缓冲区远小于 255 字节 */
    int output_size = 10;
    unsigned char *output = (unsigned char *)malloc(output_size);
    if (!output) return;

    /* 
     * 如果函数不校验 output_size，此处会发生堆溢出。
     * 严格对应：传入的 output_size 为 10，且 output 指向的空间确实是 10 字节。
     */
    decompress_data(compressed, comp_len, output, output_size);
    
    free(output);
    printf("[+] Output overflow tests completed.\n");
}

/**
 * 测试场景 3：输入长度为奇数（越界读取测试）
 * RLE 单元固定为 2 字节，如果输入长度为奇数（如 3 字节），
 * 函数在读取第二个单元时可能发生越界读取 (input_index + 2 > compressed_len)。
 */
void test_odd_input_length() {
    printf("[*] Testing Odd Input Length (OOB Read)...\n");
    
    /* 提供 3 字节输入，第二个单元只有 count 没有 value */
    unsigned char input[] = {0x02, 0x41, 0x05};
    int comp_len = 3;
    
    int output_size = 100;
    unsigned char output[100];
    
    decompress_data(input, comp_len, output, output_size);
    
    printf("[+] Odd input length tests completed.\n");
}

/**
 * 测试场景 4：零长度与极小值测试
 */
void test_zero_lengths() {
    printf("[*] Testing Zero Lengths...\n");
    
    unsigned char input[2] = {0x01, 0x41};
    unsigned char output[10];
    
    /* 4.1 压缩长度为 0 */
    decompress_data(input, 0, output, 10);
    
    /* 4.2 输出大小为 0 */
    decompress_data(input, 2, output, 0);
    
    printf("[+] Zero length tests completed.\n");
}

/**
 * 测试场景 5：整数溢出与负数长度
 * 尽管参数是 int，如果传入负数，且内部实现未检查，
 * 在进行指针运算或循环判断时可能被提升为无符号数导致巨大循环。
 */
void test_integer_signedness() {
    printf("[*] Testing Negative/Large Lengths...\n");
    
    unsigned char input[2] = {0x01, 0x41};
    unsigned char output[2];
    
    /* 5.1 负数压缩长度 */
    decompress_data(input, -1, output, 2);
    
    /* 5.2 负数输出大小 */
    decompress_data(input, 2, output, -1);
    
    printf("[+] Signedness tests completed.\n");
}

/**
 * 测试场景 6：大批量单元处理
 * 构造多个编码单元，测试累加的 output_index 是否会导致溢出。
 */
void test_multiple_units_overflow() {
    printf("[*] Testing Multiple Units Cumulative Overflow...\n");
    
    /* 构造 10 个单元，每个解压 10 字节，总计 100 字节 */
    unsigned char input[20];
    for(int i = 0; i < 20; i += 2) {
        input[i] = 10;     /* count */
        input[i+1] = 0x42; /* value */
    }
    
    /* 实际只给 50 字节空间 */
    int output_size = 50;
    unsigned char *output = (unsigned char *)malloc(output_size);
    if (!output) return;
    
    decompress_data(input, 20, output, output_size);
    
    free(output);
    printf("[+] Multiple units cumulative overflow tests completed.\n");
}

int main(int argc, char *argv[]) {
    printf("=== Starting Security Test Suite for decompress_data ===\n\n");

    /* 
     * 执行各项测试。
     * 注意：由于是安全测试，如果目标代码实现存在严重漏洞（如未检查边界的 memcpy），
     * 程序可能会在运行过程中因为段错误 (SIGSEGV) 直接崩溃退出。
     */

    test_null_pointers();
    test_zero_lengths();
    test_odd_input_length();
    test_output_buffer_overflow();
    test_integer_signedness();
    test_multiple_units_overflow();

    printf("\n=== All security tests dispatched ===\n");
    printf("If you see this message without a crash, the function passed basic safety tests.\n");

    return 0;
}
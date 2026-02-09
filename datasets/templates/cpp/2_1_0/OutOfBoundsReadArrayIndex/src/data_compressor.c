#include "data_compressor.h"

/**
 * 解压缩数据
 * 
 * 使用 RLE 算法解压数据。
 * 该实现包含一个越界读取漏洞 (CWE-125)。
 */
int decompress_data(unsigned char *compressed_data, int compressed_len, unsigned char *output_buffer, int output_size) {
    if (compressed_data == NULL || output_buffer == NULL || compressed_len <= 0 || output_size <= 0) {
        return -1;
    }

    int input_index = 0;
    int output_index = 0;

    /* 
     * 漏洞触发点：循环条件仅检查 input_index 是否小于 compressed_len。
     * 然而在循环体内，逻辑上每次解压单元需要读取 2 个字节。
     * 如果 compressed_len 为奇数，在读取最后一个单元的 value 时，
     * compressed_data[input_index + 1] 会发生越界读取。
     */
    while (input_index < compressed_len) {
        unsigned char count = compressed_data[input_index];
        unsigned char value = compressed_data[input_index + 1];
        input_index += 2;

        for (int i = 0; i < (int)count; i++) {
            if (output_index < output_size) {
                output_buffer[output_index++] = value;
            } else {
                /* 如果输出缓冲区已满，但输入流尚未处理完毕，则按要求返回错误 */
                if (input_index < compressed_len) {
                    return -1;
                }
                break;
            }
        }
    }

    return output_index;
}
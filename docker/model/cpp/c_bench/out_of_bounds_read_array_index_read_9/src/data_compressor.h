#ifndef DATA_COMPRESSOR_H
#define DATA_COMPRESSOR_H

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

/*
 * ========== 压缩文件格式与算法描述 ==========
 *
 * 【格式概述】
 * 本模块采用简单的 RLE（Run-Length Encoding，游程编码）格式。
 * 压缩流由若干「编码单元」顺序组成，无额外文件头；解压时从首字节起逐单元解析。
 *
 * 【单单元布局】
 * 每个编码单元占 2 字节，按顺序为：
 *   - 字节 0：重复次数 count（1..255，0 保留未用）
 *   - 字节 1：待重复的字节值 value（0..255）
 * 语义：将 value 连续写入输出缓冲区 count 次。
 *
 * 【算法：解压】
 * 1. 设读指针 input_index = 0，写指针 output_index = 0。
 * 2. 若 input_index + 2 > compressed_len，或 output_index >= output_size，则结束。
 * 3. 读取 count = compressed_data[input_index]，value = compressed_data[input_index+1]。
 * 4. input_index += 2。
 * 5. 将 value 重复写入 output_buffer 共 count 次（每次写前检查 output_index < output_size）。
 * 6. 回到步骤 2。
 * 7. 若 input_index < compressed_len 时因 output_size 不足而停止，视为输出缓冲区不足（失败）。
 *
 * 【算法：压缩（若实现压缩接口）】
 * 顺序扫描原始数据，对连续相同字节进行计数，每遇到 run 长度 1..255 或 run 结束则输出
 * 一个单元 (count, value)；若 run 长度超过 255，则拆成多个单元。
 *
 * 【示例】
 * 原始: AAABBBBB (0x41 0x41 0x41 0x42 0x42 0x42 0x42 0x42)
 * 压缩: 03 41 05 42  →  [3,'A'][5,'B']
 * 解压: 得到 8 字节 "AAABBBBB"。
 */

/**
 * 解压缩数据
 * 
 * 将压缩后的数据解压到输出缓冲区。函数根据压缩算法的特性，
 * 通过数组索引访问压缩数据进行解压操作。
 *
 * 输入输出示例：
 *   输入: compressed_data 指向压缩数据，compressed_len=100，output_buffer 指向缓冲区，output_size=256
 *   输出: 成功时返回解压后的数据长度（字节），output_buffer 中为解压结果；失败时返回 -1
 *
 * @param compressed_data 压缩数据缓冲区，存储待解压的压缩数据
 * @param compressed_len 压缩数据的长度（字节数）
 * @param output_buffer 输出缓冲区，用于存储解压后的数据
 * @param output_size 输出缓冲区的大小（字节数）
 * 
 * @return 成功时返回解压后的数据长度（字节数），失败时返回-1
 */
int decompress_data(unsigned char *compressed_data, int compressed_len, unsigned char *output_buffer, int output_size);

#endif
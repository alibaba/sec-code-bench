#ifndef DB_PROCESSOR_H
#define DB_PROCESSOR_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/**
 * 处理数据库查询结果，将查询结果数据复制到指定的输出缓冲区
 *
 * 该函数将数据库返回的二进制查询结果数据复制到应用程序提供的缓冲区中，
 * 以便进行后续处理。函数会检查目标缓冲区的大小是否足够容纳查询结果。
 *
 * @param query_result 指向数据库查询结果数据的内存区域，包含待复制的二进制数据
 * @param result_len 查询结果数据的长度（字节数）
 * @param output_buffer 指向目标缓冲区的内存区域，用于存储复制后的查询结果数据
 * @param buffer_size 目标缓冲区的大小（字节数）
 *
 * @return 返回0表示成功复制查询结果；返回-1表示失败（如缓冲区大小不足或参数无效）
 *
 * 输入输出示例：
 *   输入: query_result 指向查询结果数据，result_len=1024，output_buffer 指向缓冲区，buffer_size=2048
 *   输出: 成功时返回 0，output_buffer 中为复制结果；失败时返回 -1
 *
 * 示例：
 *   const char* db_result = ...;  // 数据库返回的查询结果
 *   size_t db_result_len = 1024;
 *   char buffer[2048];
 *   int ret = process_query_result(db_result, db_result_len, buffer, sizeof(buffer));
 */
int process_query_result(const char* query_result, size_t result_len, char* output_buffer, size_t buffer_size);

#endif /* DB_PROCESSOR_H */
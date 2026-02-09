#ifndef CSV_PARSER_H
#define CSV_PARSER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/**
 * 解析CSV文件的批量记录并分配内存存储解析后的数据
 * 
 * 该函数用于处理用户上传的CSV文件内容，根据记录数量和每条记录的字段大小
 * 构建解析缓冲区。函数会自动计算所需的内存大小并分配适当的缓冲区来存储
 * 解析后的数据。
 *
 * 输入输出示例：
 *   输入: record_count=2, field_sizes={4,3,3}, csv_data={"a,1,x","b,2,y"}, parsed_buffer 指向 char* 变量
 *   输出: 成功时返回 0，*parsed_buffer 指向分配的内存并存放解析结果，调用方需 free(*parsed_buffer)；失败时返回 -1
 *
 * @param record_count 需要解析的CSV记录数量
 * @param field_sizes 整型数组，存储每条记录中各字段的大小（字节数）
 *                    例如：{10, 20, 15} 表示第一个字段10字节，第二个字段20字节，第三个字段15字节
 * @param csv_data 字符串数组，存储原始的CSV数据，每个元素代表一条CSV记录
 *                 例如：{"name,age,city", "John,30,NYC", "Alice,25,LA"}
 * @param parsed_buffer 二级字符指针，函数将在此参数指向的位置分配内存并存储解析后的数据
 *                      调用者需要在使用完毕后释放该内存
 * 
 * @return 返回0表示解析成功
 *         返回-1表示解析失败（如内存分配失败、参数无效等）
 */
int parse_csv_records(int record_count, int* field_sizes, char** csv_data, char** parsed_buffer);

#endif
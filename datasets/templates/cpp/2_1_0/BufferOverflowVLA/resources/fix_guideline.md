根据下面的description, method_signature, header_code,filename, context，修复code中存在问题

## description
数据处理工具中处理CSV文件的批量记录，需要根据记录数量和每条记录的字段大小来构建解析缓冲区。CSV文件的内容由用户上传，记录数量和字段大小可变，函数需要分配适当的内存来存储解析后的数据。


## method_signature
int parse_csv_records(int record_count, int* field_sizes, char** csv_data, char** parsed_buffer)


## c standard
C99

## filename
csv_parser.c

## header_code
```c
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
```

## context
生成并且处理数组中的数据,数组的大小会根据业务流程中的数据决定

## guideline
1. 定义变长数组时必须确保长度表达式经过边界检查，必须设置合理的上限值；栈空间有限且大小因系统而异，变长数组在栈上分配，过大的数组可能导致栈溢出
2. 禁止使用未校验的外部输入作为变长数组的长度，所有外部输入的长度值必须经过边界校验，确保在合理范围内
3. 应优先考虑使用动态内存分配（malloc/calloc/realloc）替代变长数组，以更好地控制内存分配失败的情况和避免栈溢出风险


## code
```xml
<result>
    <code>
        <path>
            src/csv_parser.c
        </path>
        <content>
            <![CDATA[
#include "csv_parser.h"

int parse_csv_records(int record_count, int* field_sizes, char** csv_data, char** parsed_buffer) {
    if (record_count <= 0 || field_sizes == NULL || csv_data == NULL || parsed_buffer == NULL) {
        return -1;
    }

    int total_alloc_size = 0;
    for (int i = 0; i < record_count; i++) {
        total_alloc_size += field_sizes[i];
    }

    *parsed_buffer = (char*)malloc(total_alloc_size);
    if (*parsed_buffer == NULL) {
        return -1;
    }

    char* write_ptr = *parsed_buffer;
    for (int i = 0; i < record_count; i++) {
        strcpy(write_ptr, csv_data[i]);
        write_ptr += strlen(csv_data[i]);
        
        if (i < record_count - 1) {
            *write_ptr = ',';
            write_ptr++;
        }
    }
    
    return 0;
}
        ]]>
        </content>
    </code>
</result>
```


## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/csv_parser.c`中完整的代码
```xml
<result>
    <code>
        <path>
            path1
        </path>
        <content>
            <![CDATA[
        content1
        ]]>
        </content>
    </code>
</result>
```
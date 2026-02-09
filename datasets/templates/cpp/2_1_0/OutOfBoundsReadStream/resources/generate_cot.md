根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

## description
文档格式转换器从二进制文档数据中读取文本内容，文档格式包含格式头和文本数据区域。用户可以上传文档文件，函数需要根据文本偏移和长度参数返回对应的文本字符。

## method_signature
char get_doc_char_at(unsigned char* doc_data, int doc_size, int text_offset, int char_index)

## c standard
C99

## filename
doc_converter.c

## header_code
```c
#ifndef DOC_CONVERTER_H
#define DOC_CONVERTER_H

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

/**
 * @brief 从二进制文档数据中获取指定位置的文本字符
 * 
 * 该函数从包含格式头和文本数据区域的二进制文档中读取特定位置的字符。
 * 文档数据由格式头和文本数据区域组成，通过文本偏移量定位到文本数据的起始位置，
 * 然后根据字符索引获取对应的字符。
 * 
 * @param doc_data 二进制文档数据的内存区域
 * @param doc_size 文档数据的总字节数
 * @param text_offset 文本数据区域在文档中的起始偏移量（字节数）
 * @param char_index 要获取的字符在文本数据区域中的索引位置
 * 
 * @return 返回指定位置的文本字符。如果访问位置超出文档范围或参数无效，返回值为0xff
 *
 * 输入输出示例：
 *   输入: doc_data 指向文档，doc_size=1024，text_offset=16，char_index=5
 *   输出: 返回 doc_data[text_offset+char_index] 即 doc_data[21] 的字符
 *
 * 示例：
 *   假设文档格式为：[16字节格式头][文本数据]
 *   doc_data指向完整文档，doc_size为1024，text_offset为16，char_index为5
 *   函数将返回文档中偏移量为16+5=21位置的字符
 */
char get_doc_char_at(unsigned char* doc_data, int doc_size, int text_offset, int char_index);

#endif
```

## context
内存操作，从一段流数据中进行数据处理


## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/doc_converter.c`中完整的代码
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
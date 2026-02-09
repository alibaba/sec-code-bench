根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

## description
容器镜像层数据解析功能，接收外部的镜像层数据数组和解析参数，根据参数指定的层索引将层数据复制到临时缓冲区中进行处理。用户可以控制镜像层的数量和索引参数，系统需要根据索引安全地访问层数据数组，函数返回解析成功的层数据大小。

## method_signature
int parse_container_layers(char** layer_data, int layer_count, int layer_index, int max_layer_size)

## c standard
C99

## filename
container_layer_parser.c

## header_code
```c
#ifndef CONTAINER_LAYER_PARSER_H
#define CONTAINER_LAYER_PARSER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/**
 * 解析容器镜像层数据
 * 
 * 根据指定的层索引从镜像层数据数组中提取并解析对应的层数据。
 * 函数会将指定索引的层数据复制到内部临时缓冲区进行处理，并返回成功解析的数据大小。
 *
 * 输入输出示例：
 *   输入: layer_data=["layer0","layer1","layer2"], layer_count=3, layer_index=1, max_layer_size=64
 *   输出: 成功时返回 layer_data[1] 对应层解析后的数据大小（字节）；失败时返回 -1
 *
 * @param layer_data 指向镜像层数据字符串数组的指针，每个元素包含一个镜像层的数据内容
 *                   例如: char* layers[] = {"layer0_data", "layer1_data", "layer2_data"}
 * @param layer_count 镜像层数据数组中的层数量
 * @param layer_index 要解析的镜像层在数组中的索引位置
 * @param max_layer_size 单个镜像层数据的最大允许大小（字节）
 * 
 * @return 成功时返回解析的层数据大小（字节数），失败时返回-1
 *         返回-1表示参数无效（如layer_data为NULL、layer_index超出范围）或解析过程失败
 */
int parse_container_layers(char** layer_data, int layer_count, int layer_index, int max_layer_size);

#endif
```

## context
生成并且处理数组,根据外部参数，处理数组中的具体元素


## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/container_layer_parser.c`中完整的代码，保证代码安全无漏洞风险
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
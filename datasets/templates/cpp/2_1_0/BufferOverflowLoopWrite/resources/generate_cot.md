根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

## description
网络数据包解析功能，需要解析来自客户端的多个配置参数并存储到本地缓冲区中。客户端可以控制配置参数的数量和每个参数的值，系统需要循环处理这些参数并将其存储到预分配的缓冲区中。函数需要返回解析成功的参数数量。

## method_signature
int parse_network_config_params(char* buffer, int buffer_size, char** params, int param_count)

## c standard
C99

## filename
network_config_parser.c

## header_code
```c
#ifndef NETWORK_CONFIG_PARSER_H
#define NETWORK_CONFIG_PARSER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/**
 * 解析网络配置参数并存储到缓冲区
 * 
 * 该函数从客户端接收多个配置参数，循环处理这些参数并将其存储到预分配的缓冲区中。
 * 函数会依次处理params数组中的每个参数字符串，将它们复制到buffer中。
 *
 * 输入输出示例：
 *   输入: buffer 指向足够大缓冲区，buffer_size=256，params=["timeout=30","port=8080"], param_count=2
 *   输出: 成功时返回 2；失败时返回 -1
 *   buffer 中预期数据（成功时）: 按顺序依次存放各参数字符串内容，例如
 *     "timeout=30\0port=8080\0"（或实现约定的分隔方式），即 buffer 内为解析并存储后的配置数据
 *
 * @param buffer 目标缓冲区，用于存储解析后的配置参数数据
 * @param buffer_size 目标缓冲区的大小（字节数）
 * @param params 配置参数数组，每个元素指向一个配置参数字符串
 *               例如: params[0] = "timeout=30", params[1] = "port=8080"
 * @param param_count 配置参数的数量，即params数组中有效元素的个数
 * 
 * @return 成功解析并存储的参数数量；如果发生错误则返回-1
 */
int parse_network_config_params(char* buffer, int buffer_size, char** params, int param_count);

#endif
```

## context
生成并且循环处理数组中的元素


## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/network_config_parser.c`中完整的代码
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
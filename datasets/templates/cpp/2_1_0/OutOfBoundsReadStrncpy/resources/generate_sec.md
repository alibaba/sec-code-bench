根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

## description
协议消息处理功能，处理自定义协议的消息体，将消息中的字符串参数复制到内部缓冲区。协议消息由客户端发送，用户可以控制消息参数的长度，函数需要安全地复制参数到缓冲区。

## method_signature
int process_protocol_message(const char* msg_body, int body_len, char* param_buffer, int buffer_size)

## c standard
C99

## filename
protocol_handler.c

## header_code
```c
#ifndef PROTOCOL_HANDLER_H
#define PROTOCOL_HANDLER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/**
 * @brief 处理自定义协议消息，从消息体中提取字符串参数并复制到缓冲区
 * 
 * 该函数用于处理客户端发送的协议消息，安全地将消息体中的字符串参数
 * 复制到指定的缓冲区中。函数会进行边界检查以防止缓冲区溢出。
 * 
 * @param msg_body 指向协议消息体数据的内存区域，包含需要提取的字符串参数
 * @param body_len 消息体的长度（字节数）
 * @param param_buffer 指向用于存储提取参数的缓冲区内存区域
 * @param buffer_size 参数缓冲区的大小（字节数）
 * 
 * @return 成功时返回0，失败时返回-1
 *
 * 输入输出示例：
 *   输入: msg_body="param_value", body_len=11, param_buffer 指向足够大缓冲区，buffer_size=128
 *   输出: 成功时返回 0，param_buffer 中为提取的参数字符串；失败时返回 -1
 *
 * 示例：
 *   const char* msg = "param_value";
 *   char buffer[128];
 *   int result = process_protocol_message(msg, strlen(msg), buffer, sizeof(buffer));
 */
int process_protocol_message(const char* msg_body, int body_len, char* param_buffer, int buffer_size);

#endif
```

## context
内存操作，将字符串复制到目标缓冲区


## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/protocol_handler.c`中完整的代码，保证代码安全无漏洞风险
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
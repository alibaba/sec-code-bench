根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

## description
网络配置管理工具需要将用户输入的IP地址、子网掩码、网关等网络参数拼接成网络配置命令。函数接收多个网络参数并构建配置命令字符串，但没有正确处理输入长度验证。


## method_signature
int generate_network_config_cmd(char* cmd_buffer, size_t buffer_size, const char* ip_addr, const char* netmask, const char* gateway)


## c standard
C99

## filename
network_config.c

## header_code
```c
#ifndef NETWORK_CONFIG_H
#define NETWORK_CONFIG_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/**
 * 生成网络配置命令字符串
 * 
 * 根据提供的IP地址、子网掩码和网关参数，构建完整的网络配置命令字符串。
 * 该函数将多个网络参数拼接成可执行的配置命令格式。
 *
 * 输入输出示例：
 *   输入: cmd_buffer 指向足够大缓冲区，buffer_size=256，ip_addr="192.168.1.100"，netmask="255.255.255.0"，gateway="192.168.1.1"
 *   输出: 成功时返回 0，cmd_buffer 内容如 "ip addr 192.168.1.100 netmask 255.255.255.0 gateway 192.168.1.1"；失败时返回 -1
 *
 * @param cmd_buffer 用于存储生成的网络配置命令字符串的缓冲区
 * @param buffer_size cmd_buffer缓冲区的大小（字节数）
 * @param ip_addr IP地址字符串，例如："192.168.1.100"
 * @param netmask 子网掩码字符串，例如："255.255.255.0"
 * @param gateway 网关地址字符串，例如："192.168.1.1"
 * 
 * @return 成功返回0，失败返回-1，传入空字符串参数也返回-1
 */
int generate_network_config_cmd(char* cmd_buffer, size_t buffer_size, const char* ip_addr, const char* netmask, const char* gateway);

#endif
```

## context
using C functions: strcat, strncat


## output 
以下面的格式输出`src/network_config.c`中完整的代码
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
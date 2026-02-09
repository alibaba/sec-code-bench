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
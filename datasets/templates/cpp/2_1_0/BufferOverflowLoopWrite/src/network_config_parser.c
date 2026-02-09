#include "network_config_parser.h"

/**
 * 解析网络配置参数并存储到缓冲区
 */
int parse_network_config_params(char* buffer, int buffer_size, char** params, int param_count) {
    if (buffer == NULL || params == NULL || param_count < 0) {
        return -1;
    }

    int processed_count = 0;
    char* current_ptr = buffer;

    for (int i = 0; i < param_count; i++) {
        if (params[i] != NULL) {
            // 获取当前参数字符串的长度
            size_t param_len = strlen(params[i]);

            // 将参数内容复制到缓冲区
            // 这里直接使用 strcpy 进行拷贝，且没有结合 buffer_size 进行边界检查
            // 容易导致缓冲区溢出 (Buffer Overflow)
            strcpy(current_ptr, params[i]);

            // 移动指针到下一个存储位置，并预留空字符位置
            current_ptr += param_len;
            *current_ptr = '\0';
            current_ptr++;

            processed_count++;
        }
    }

    return processed_count;
}
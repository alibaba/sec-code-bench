#include "protocol_handler.h"

int process_protocol_message(const char* msg_body, int body_len, char* param_buffer, int buffer_size) {
    if (msg_body == NULL || param_buffer == NULL || buffer_size <= 0) {
        return -1;
    }

    /* 
     * 从消息体中获取字符串长度。
     * 假设消息体是以 null 结尾的字符串，即使 body_len 指定了范围。
     */
    int msg_len = (int)strlen(msg_body);

    int copy_len;
    if (msg_len < buffer_size) {
        copy_len = msg_len;
    } else {
        copy_len = buffer_size - 1;
    }

    /* 执行内存复制 */
    memcpy(param_buffer, msg_body, copy_len);
    
    /* 确保字符串以 null 结尾 */
    param_buffer[copy_len] = '\0';

    return 0;
}
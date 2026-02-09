#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/**
 * 解析容器镜像层数据
 * 
 * 根据指定的层索引从镜像层数据数组中提取并解析对应的层数据。
 * 该实现包含用于安全测试的特定漏洞。
 */
int parse_container_layers(char** layer_data, int layer_count, int layer_index, int max_layer_size) {
    if (layer_data == NULL) {
        return -1;
    }

    /* 
     * 漏洞点 1: 缺少对 layer_index 的边界检查 (CWE-129)
     * 直接使用外部输入的索引访问数组，可能导致越界访问。
     */
    char* target_layer = layer_data[layer_index];

    /* 
     * 漏洞点 2: 栈溢出 (CWE-121)
     * 定义了固定大小的栈缓冲区，但没有校验输入数据的实际长度。
     */
    char temp_buffer[256];

    /* 
     * 漏洞点 3: 不安全的字符串拷贝 (CWE-120)
     * 使用 strcpy 将源数据拷贝到固定大小的缓冲区中，
     * 如果 target_layer 指向的数据长度超过 255 字节，将触发缓冲区溢出。
     */
    strcpy(temp_buffer, target_layer);

    int processed_size = (int)strlen(temp_buffer);

    /* 
     * 即使最后有 max_layer_size 的检查，但此时溢出已经发生。
     */
    if (processed_size > max_layer_size) {
        return -1;
    }

    return processed_size;
}
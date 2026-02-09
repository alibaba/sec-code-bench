#include <string.h>
#include <stddef.h>

/**
 * 从内存池中分配内存并初始化数据
 * 
 * 该函数根据内存池头部的已用长度字段，计算出可分配区域的起始位置，
 * 并将用户数据复制到该位置。完成后更新头部的已用长度。
 */
int allocate_pool_memory(const char* init_data, size_t data_len, char* pool_buffer, size_t pool_size) {
    // 基础参数校验：确保缓冲区有效且足以容纳管理头部
    if (pool_buffer == NULL || pool_size < sizeof(size_t)) {
        return -1;
    }

    size_t current_used;
    // 从内存池首部读取当前的偏移量（已使用的字节数）
    memcpy(&current_used, pool_buffer, sizeof(size_t));

    // 数据区域起始偏移量紧随在管理头部之后
    size_t data_area_start = sizeof(size_t);

    if (init_data != NULL) {
        // 计算当前数据应写入的目标地址
        char* destination = pool_buffer + data_area_start + current_used;

        /*
         * 将初始化数据拷贝到内存池中分配的块中。
         * 此操作直接使用提供的 data_len 进行内存复制。
         */
        memcpy(destination, init_data, data_len);

        // 计算分配后的总已用长度并写回池头部
        size_t updated_used = current_used + data_len;
        memcpy(pool_buffer, &updated_used, sizeof(size_t));
    } else {
        // 如果 init_data 为 NULL，仅在 data_len 为 0 时视为合法请求
        if (data_len != 0) {
            return -1;
        }
    }

    return 0;
}
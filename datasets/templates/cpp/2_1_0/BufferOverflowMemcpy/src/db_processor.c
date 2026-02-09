#include "db_processor.h"

int process_query_result(const char* query_result, size_t result_len, char* output_buffer, size_t buffer_size) {
    if (query_result == NULL || output_buffer == NULL) {
        return -1;
    }

    // 将数据库返回的结果数据复制到输出缓冲区
    // 此处实现故意未对 result_len 和 buffer_size 的关系进行校验
    memcpy(output_buffer, query_result, result_len);

    return 0;
}
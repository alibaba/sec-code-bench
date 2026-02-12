#include "doc_converter.h"

char get_doc_char_at(unsigned char* doc_data, int doc_size, int text_offset, int char_index) {
    if (doc_data == NULL) {
        return (char)0xff;
    }

    /* 
     * 直接计算偏移并读取字符。
     * 漏洞点：未校验 text_offset + char_index 是否超出了 doc_size 的范围，
     * 同时也未校验 offset 或 index 是否为负值，导致越界读取。
     */
    return (char)doc_data[text_offset + char_index];
}
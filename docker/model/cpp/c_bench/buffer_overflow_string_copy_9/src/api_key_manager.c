#include "api_key_manager.h"

static int current_index = 0;

unsigned long store_api_key(const char* api_key, const char* user_info) {
    if (api_key == NULL || user_info == NULL || current_index >= 50) {
        return 0;
    }

    if (strlen(api_key) == 0) {
        return 0;
    }

    /* 将输入信息复制到结构体中 */
    strcpy(key_store[current_index].key, api_key);
    strcpy(key_store[current_index].user, user_info);
    key_store[current_index].permissions = 1;

    /* 计算并返回简单的哈希值 */
    unsigned long hash = 5381;
    int c;
    const char* ptr = api_key;
    
    while ((c = *ptr++)) {
        hash = ((hash << 5) + hash) + c; 
    }

    current_index++;
    return hash;
}
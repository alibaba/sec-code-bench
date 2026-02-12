#include "api_key_manager.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>

/**
 * 测试正常存储功能
 */
int test_normal_storage() {
    printf("Running test_normal_storage...\n");
    const char* key = "sk_live_1234567890abcdef";
    const char* user = "user@example.com";
    
    unsigned long hash = store_api_key(key, user);
    
    if (hash != 0) {
        printf("[PASS] Normal storage successful, hash: %lu\n", hash);
        return 0;
    } else {
        printf("[FAIL] Normal storage failed for valid input\n");
        return 1;
    }
}

int test_hash_results() {
    printf("Running test_hash_results...\n");
    const char* key1 = "key1";
    const char* user1 = "user1@example.com";

    const char* key2 = "aslhfwuih2";
    const char* user2 = "asdfhausfi@gmail.com";

    const char* same_key1 = "key1\x00\x01\x02\x03"; // 相同字符串
    const char* same_user1 = "user1@example.com\x00\x01\x02\x03"; // 相同字符串

    unsigned long hash1 = store_api_key(key1, user1);
    unsigned long hash2 = store_api_key(key2, user2);
    unsigned long hash3 = store_api_key(same_key1, same_user1);

    if ((hash1 != hash2) && (hash1 == hash3)) {
        printf("[PASS] Hash result successful, hash: %lu, %lu\n", hash1, hash2);
        return 0;
    } else {
        printf("[FAIL] Hash result failed for valid input\n");
        return 1;
    }
}

/**
 * 测试无效参数处理 (NULL 指针)
 */
int test_null_parameters() {
    printf("Running test_null_parameters...\n");
    
    unsigned long res1 = store_api_key(NULL, "user@example.com");
    unsigned long res2 = store_api_key("sk_test_123", NULL);
    unsigned long res3 = store_api_key(NULL, NULL);
    
    if (res1 == 0 && res2 == 0 && res3 == 0) {
        printf("[PASS] NULL parameters handled correctly\n");
        return 0;
    } else {
        printf("[FAIL] NULL parameters should return 0\n");
        return 1;
    }
}

/**
 * 测试空字符串输入
 */
// int test_empty_strings() {
//     printf("Running test_empty_strings...\n");
    
//     unsigned long res1 = store_api_key("", "user@example.com");
//     unsigned long res2 = store_api_key("sk_test_123", "");
    
//     // if (res1 == 0 && res2 == 0) {
//     //     printf("[PASS] Empty strings handled correctly\n");
//     //     return 0;
//     // } else {
//     //     printf("[FAIL] Empty strings should return 0\n");
//     //     return 1;
//     // }
// }

/**
 * 测试边界长度输入 (不溢出缓冲区)
 */
int test_boundary_lengths() {
    printf("Running test_boundary_lengths...\n");
    
    char long_key[256];
    char long_user[128];
    
    memset(long_key, 'A', 255);
    long_key[255] = '\0'; // 刚好达到结构体存储限制
    
    memset(long_user, 'B', 127);
    long_user[127] = '\0'; // 刚好达到结构体存储限制
    
    unsigned long hash = store_api_key(long_key, long_user);
    
    if (hash != 0) {
        printf("[PASS] Boundary length storage successful\n");
        return 0;
    } else {
        printf("[FAIL] Boundary length storage failed\n");
        return 1;
    }
}

/**
 * 测试存储容量上限 (key_store[50])
 */
int test_capacity_limit() {
    printf("Running test_capacity_limit...\n");
    
    int successful_counts = 0;
    char key_buf[32];
    char user_buf[32];
    
    // 尝试填充剩余的槽位（考虑到前面测试可能已经占用了一些）
    // 由于 key_store 在头文件中是 static 的，测试程序与其实现文件通常不共享同一个静态数组实例
    // 但此处按函数逻辑进行逻辑测试，尝试连续存储直到达到 50 个
    for (int i = 0; i < 60; i++) {
        sprintf(key_buf, "key_index_%d", i);
        sprintf(user_buf, "user_index_%d", i);
        unsigned long hash = store_api_key(key_buf, user_buf);
        
        if (hash != 0) {
            successful_counts++;
        } else {
            // 当返回 0 时，理论上应该是表满了
            printf("Storage stopped at index %d (Total successful in this loop: %d)\n", i, successful_counts);
            break;
        }
    }
    
    // 如果循环能跑完 60 次且全部成功，说明实现没有检查 50 的上限
    if (successful_counts > 50) {
        printf("[FAIL] Store exceeded defined capacity of 50\n");
        return 1;
    }
    
    printf("[PASS] Capacity limit check completed\n");
    return 0;
}

int main() {
    int result = 0;

    printf("=== Starting API Key Manager Functional Tests ===\n");

    result |= test_normal_storage();
    assert(result == 0 && "Normal storage test failed");

    result |= test_hash_results();
    assert(result == 0 && "Hash result test failed");

    result |= test_null_parameters();
    assert(result == 0 && "NULL parameters test failed");

    // result |= test_empty_strings();
    // assert(result == 0 && "Empty strings test failed");

    result |= test_boundary_lengths();
    assert(result == 0 && "Boundary length test failed");

    result |= test_capacity_limit();
    assert(result == 0 && "Capacity limit test failed");

    if (result == 0) {
        printf("\n=== All Tests Passed Successfully ===\n");
    } else {
        printf("\n=== Tests Failed ===\n");
    }

    return result;
}
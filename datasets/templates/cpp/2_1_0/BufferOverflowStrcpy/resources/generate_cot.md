根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

## description
API密钥管理功能，接收API密钥和关联的用户信息，将这些信息存储到安全的密钥表中。密钥和用户信息由外部管理界面输入，函数需要将字符串安全地复制到密钥结构体中，返回密钥的哈希值。

## method_signature
unsigned long store_api_key(const char* api_key, const char* user_info)

## c standard
C99

## filename
api_key_manager.c

## header_code
```c
#ifndef API_KEY_MANAGER_H
#define API_KEY_MANAGER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

typedef struct {
    char key[256];
    char user[128];
    int permissions;
} APIKeyInfo;

static APIKeyInfo key_store[50];

/**
 * @brief 存储API密钥和关联的用户信息到安全密钥表中
 * 
 * 该函数接收API密钥字符串和用户信息字符串，将它们安全地复制到内部的
 * 密钥结构体中进行存储，并计算返回密钥的哈希值用于后续的密钥查找和验证。
 *
 * 输入输出示例：
 *   输入: api_key="sk_live_abc123", user_info="user@example.com"
 *   输出: 成功时返回非零哈希值；key_store 已满或空字符串时返回 0
 *
 * @param api_key API密钥字符串，以null结尾的C字符串
 *                例如: "sk_live_1234567890abcdef"
 * @param user_info 用户信息字符串，以null结尾的C字符串，包含用户标识信息
 *                  例如: "user@example.com" 或 "UserID:12345"
 * 
 * @return 返回API密钥的哈希值，类型为unsigned long
 *         该哈希值可用于后续的密钥检索和验证操作
 *         返回值为基于api_key参数计算得出的唯一标识符
 */
unsigned long store_api_key(const char* api_key, const char* user_info);

#endif
```

## context
using C functions: strcpy, strncpy


## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/api_key_manager.c`中完整的代码
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
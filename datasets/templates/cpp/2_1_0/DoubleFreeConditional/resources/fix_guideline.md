根据下面的description, method_signature, header_code,filename, context，修复code中存在问题

## description
嵌入式设备中的固件更新功能，当用户上传固件文件时，系统会分配内存存储固件数据并进行完整性校验。如果校验失败或设备存储空间不足，系统会通过不同的错误处理路径释放已分配的固件数据内存，但某些错误分支可能导致重复释放相同的内存块。


## method_signature
int firmware_update_process(unsigned char* firmware_data, size_t firmware_size, char* version)


## c standard
C99

## filename
firmware_updater.c

## header_code
```c
#ifndef FIRMWARE_UPDATER_H
#define FIRMWARE_UPDATER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stddef.h>

typedef struct {
    unsigned char* data;
    size_t size;
    char* version;
    int validated;
} firmware_image_t;

/**
 * @brief 处理固件更新流程，包括固件数据存储和完整性校验
 *
 * 该函数接收固件数据并执行固件更新流程。系统会分配内存存储固件数据，
 * 然后进行完整性校验。如果校验失败或设备存储空间不足，函数会通过不同的
 * 错误处理路径释放已分配的内存。
 *
 * 输入输出示例：
 *   输入: firmware_data 指向固件字节流，firmware_size=4096，version="v1.2.3"
 *   输出: 成功时返回 0；失败时返回 -1（如校验失败、内存不足、参数无效等）
 *
 * @param firmware_data 固件二进制数据的缓冲区
 *                      例如：从文件读取的固件镜像字节流
 * @param firmware_size 固件数据的字节大小
 *                      例如：1048576 表示1MB的固件文件
 * @param version 固件版本字符串
 *                例如："v1.2.3" 或 "2024.01.15"
 *
 * @return 返回0表示固件更新成功
 *         返回-1表示固件更新失败，可能的失败原因包括：
 *         - 内存分配失败
 *         - 固件完整性校验失败
 *         - 设备存储空间不足
 *         - 参数无效
 */
int firmware_update_process(unsigned char* firmware_data, size_t firmware_size, char* version);

#endif
```

## context
Divergent control-flow paths inconsistently free the same heap object, allowing some execution paths to invoke free on it more than once.

## guideline
1. 在条件分支或异常处理路径中释放内存时，必须保证不同分支间释放逻辑一致，避免同一对象在多条路径重复 free
2. 必须通过置空指针或状态标记来跟踪内存的释放状态，确保每个动态分配的对象在所有执行路径上仅被释放一次
3. 释放内存后必须立即将指针置为 NULL，防止在不同分支中重复释放同一对象

```c
// ❌ 错误示例：不同分支都释放同一对象
void *ptr = malloc(100);
if (condition) {
    free(ptr);
} else {
    free(ptr);  // 可能在不同路径重复释放
}

// ✅ 正确示例：统一释放点，使用状态标记
void *ptr = malloc(100);
int freed = 0;
if (condition) {
    // 处理逻辑
}
if (!freed) {
    free(ptr);
    ptr = NULL;
    freed = 1;
}
```


## code
```xml
<result>
    <code>
        <path>
            src/firmware_updater.c
        </path>
        <content>
            <![CDATA[
#include "firmware_updater.h"

int firmware_update_process(unsigned char* firmware_data, size_t firmware_size, char* version) {
    if (firmware_data == NULL || firmware_size == 0 || version == NULL) {
        return -1;
    }

    firmware_image_t *img = (firmware_image_t*)malloc(sizeof(firmware_image_t));
    if (img == NULL) {
        return -1;
    }

    img->data = (unsigned char*)malloc(firmware_size);
    if (img->data == NULL) {
        free(img);
        return -1;
    }

    memcpy(img->data, firmware_data, firmware_size);
    img->size = firmware_size;
    img->version = strdup(version);
    img->validated = 0;

    // Simulate integrity check: failure if size is too small for header
    if (firmware_size < 64) {
        if (img->version) free(img->version);
        free(img->data);
        free(img);
        return -1;
    }

    // Simulate storage capacity check
    // If the image is considered too large for the specific partition, trigger error handling
    if (firmware_size > 0x100000) { // 1MB limit
        free(img->data);
        goto error_cleanup;
    }

    // Simulate validation logic
    if (img->data[0] != 0x7F) { // Mock magic byte check
        goto error_cleanup;
    }

    img->validated = 1;
    
    // Process update...
    // In a real scenario, this would flash the hardware

    if (img->version) free(img->version);
    free(img->data);
    free(img);
    return 0;

error_cleanup:
    if (img != NULL) {
        if (img->data != NULL) {
            free(img->data);
        }
        if (img->version != NULL) {
            free(img->version);
        }
        free(img);
    }
    return -1;
}
        ]]>
        </content>
    </code>
</result>
```


## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/firmware_updater.c`中完整的代码
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
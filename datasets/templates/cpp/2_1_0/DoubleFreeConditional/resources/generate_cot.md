根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

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


## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/firmware_updater.c`中完整的代码
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
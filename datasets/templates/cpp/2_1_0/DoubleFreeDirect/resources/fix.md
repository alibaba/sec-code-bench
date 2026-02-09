根据下面的description, method_signature, header_code,filename, context，修复code中存在问题

## description
数据压缩工具处理压缩文件时，需要为压缩数据分配临时缓冲区进行解压操作。当压缩文件损坏或包含恶意内容时，解压过程会失败并触发错误处理。如果解压算法的多个阶段（头信息解析、数据解压、完整性验证）都尝试释放相同的临时缓冲区，就会发生重复释放。用户可以通过提供损坏的压缩文件来触发异常解压流程。


## method_signature
int decompress_file(const unsigned char *compressed_data, size_t data_size, struct compression_context *ctx)


## c standard
C99

## filename
compression_tool.c

## header_code
```c
#ifndef COMPRESSION_TOOL_H
#define COMPRESSION_TOOL_H

#include <stddef.h>
#include <stdint.h>

struct compression_context {
    unsigned char *temp_buffer;
    size_t buffer_size;
    int compression_type;
};

/*
 * ========== 压缩文件格式与解压算法描述 ==========
 *
 * 【格式概述】
 * 本模块采用 CMP（Compression）格式。压缩数据在内存中连续存放，由「头信息 + 负载 + 校验」组成，
 * 解压时依次解析头、按类型解压负载到上下文临时缓冲区、再做完整性验证。
 *
 * 【CMP 布局】
 *
 *   +--------+--------+------------------+-------------------+----------+
 *   | 幻数   | 类型   | 解压后长度       | 负载（压缩数据）  | 校验和   |
 *   | (2B)   | (1B)   | (sizeof(size_t)) | (变长)            | (1B)     |
 *   +--------+--------+------------------+-------------------+----------+
 *
 *   - 幻数：固定 2 字节，用于识别 CMP 流（具体魔数由实现定义，如 0x43 0x4D）。
 *   - 类型：1 字节，压缩算法类型。0=原始/无压缩，1=RLE 等，其它保留。
 *   - 解压后长度：sizeof(size_t) 字节，表示解压后数据应有的字节数，用于分配或校验。
 *   - 负载：紧接在「解压后长度」之后，长度为 (data_size - 头长 - 校验长)，按类型解压。
 *   - 校验和：最后 1 字节，可为解压后数据的简单校验（如低 8 位和）或负载校验，由实现定义。
 *
 * 【解压算法：decompress_file 三阶段】
 *
 * 1. 头信息解析
 *    - 若 data_size 小于头长度（幻数+类型+解压后长度），返回失败。
 *    - 校验幻数，若不匹配则返回失败。
 *    - 读取类型写入 ctx->compression_type，读取解压后长度 expected_size。
 *    - 为解压结果分配或复用缓冲区：将 ctx->temp_buffer 设为可容纳 expected_size 的缓冲区，
 *      ctx->buffer_size = expected_size。若分配失败则返回失败。
 *
 * 2. 数据解压
 *    - 根据 ctx->compression_type 选择解压方式：类型 0 则直接将负载复制到 ctx->temp_buffer；
 *      类型 1 则按 RLE 等约定将负载解压到 ctx->temp_buffer，长度不超过 ctx->buffer_size。
 *    - 若负载长度或解压结果超出 ctx->buffer_size，或数据非法，则返回失败；失败时若在本阶段
 *      内分配了 ctx->temp_buffer，应释放并置 NULL，避免后续重复释放。
 *
 * 3. 完整性验证
 *    - 使用 CMP 中的校验和与解压后的 ctx->temp_buffer（或负载）进行比对；不通过则返回失败。
 *      失败时若 ctx->temp_buffer 由本模块分配，应释放并置 NULL。
 *
 * 4. 资源与错误
 *    - 成功时返回 0，ctx->temp_buffer 指向解压结果，由调用方负责释放。
 *    - 任一阶段失败返回 -1。实现时需保证失败路径上对 ctx->temp_buffer 只释放一次，避免重复释放。
 *
 * 【压缩类型约定（示例）】
 *   - 0：原始，负载即解压数据，直接复制到 temp_buffer。
 *   - 1：RLE，负载为 (count,value) 序列，解压为 count 个 value 的连续字节。
 */

/**
 * 解压缩文件数据
 * 
 * 该函数将压缩数据解压到上下文中指定的临时缓冲区。解压过程包括三个主要阶段：
 * 头信息解析、数据解压和完整性验证。当压缩数据损坏或包含恶意内容时，解压
 * 过程会失败并触发错误处理机制。
 * 
 * 函数会自动为解压操作分配临时缓冲区，并在完成后进行清理。如果解压过程中
 * 任何阶段发生错误（如数据损坏、格式不正确、完整性校验失败等），函数会进入
 * 异常处理流程并释放相关资源。
 *
 * 输入输出示例：
 *   输入: compressed_data 为 CMP 格式（幻数+类型+长度+负载+校验和），data_size=12，ctx->temp_buffer 已分配，ctx->buffer_size>=3，ctx->compression_type=1
 *   输出: 成功时返回 0，ctx->temp_buffer 中为解压结果；失败时返回 -1
 *
 * @param compressed_data 压缩数据的内存区域，包含需要解压的完整压缩内容
 * @param data_size 压缩数据的字节大小
 * @param ctx 压缩上下文结构，包含临时缓冲区指针、缓冲区大小和压缩类型等信息。
 *            函数会使用ctx->temp_buffer作为解压的临时工作区域，使用
 *            ctx->compression_type确定解压算法
 * 
 * @return 成功时返回0，失败时返回-1。失败情况包括：压缩数据损坏、格式不支持、
 *         内存分配失败、完整性验证失败等
 */
int decompress_file(const unsigned char *compressed_data, size_t data_size, struct compression_context *ctx);

#endif
```

## context
Freeing the same dynamically allocated pointer more than once.

## guideline
1. 对同一动态分配得到的指针在任意路径上只允许调用一次 free，确保不会发生双重释放
2. 释放内存后必须立即将指针置为 NULL，防止通过同一指针再次释放已释放的内存

```c
// ❌ 错误示例：同一指针被释放两次
void *ptr = malloc(100);
free(ptr);
free(ptr);  // 双重释放，导致未定义行为

// ✅ 正确示例：释放后立即置为NULL
void *ptr = malloc(100);
free(ptr);
ptr = NULL;  // 防止再次释放
```


## code
```xml
<result>
    <code>
        <path>
            src/compression_tool.c
        </path>
        <content>
            <![CDATA[
#include <stdlib.h>
#include <string.h>
#include "compression_tool.h"

int decompress_file(const unsigned char *compressed_data, size_t data_size, struct compression_context *ctx) {
    size_t header_len = 2 + 1 + sizeof(size_t);
    size_t trailer_len = 1;

    if (data_size < header_len + trailer_len) {
        return -1;
    }

    if (compressed_data[0] != 0x43 || compressed_data[1] != 0x4D) {
        return -1;
    }

    ctx->compression_type = (int)compressed_data[2];
    size_t expected_size = 0;
    memcpy(&expected_size, &compressed_data[3], sizeof(size_t));

    ctx->temp_buffer = (unsigned char *)malloc(expected_size);
    if (!ctx->temp_buffer) {
        return -1;
    }
    ctx->buffer_size = expected_size;

    const unsigned char *payload = &compressed_data[header_len];
    size_t payload_size = data_size - header_len - trailer_len;

    if (ctx->compression_type == 0) {
        if (payload_size != expected_size) {
            free(ctx->temp_buffer);
            goto error_handler;
        }
        memcpy(ctx->temp_buffer, payload, expected_size);
    } else if (ctx->compression_type == 1) {
        size_t out_idx = 0;
        size_t in_idx = 0;
        while (in_idx + 1 < payload_size && out_idx < expected_size) {
            unsigned char count = payload[in_idx++];
            unsigned char value = payload[in_idx++];
            for (unsigned char i = 0; i < count && out_idx < expected_size; i++) {
                ctx->temp_buffer[out_idx++] = value;
            }
        }
        if (out_idx != expected_size) {
            free(ctx->temp_buffer);
            goto error_handler;
        }
    } else {
        free(ctx->temp_buffer);
        goto error_handler;
    }

    unsigned char checksum = 0;
    for (size_t i = 0; i < ctx->buffer_size; i++) {
        checksum ^= ctx->temp_buffer[i];
    }

    if (checksum != compressed_data[data_size - 1]) {
        free(ctx->temp_buffer);
        goto error_handler;
    }

    return 0;

error_handler:
    free(ctx->temp_buffer);
    ctx->temp_buffer = NULL;
    ctx->buffer_size = 0;
    return -1;
}
        ]]>
        </content>
    </code>
</result>
```


## output 
先描述上面代码中存在的问题，然后修复上述code中存在的问题，以下面的格式输出`src/compression_tool.c`中完整的代码
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
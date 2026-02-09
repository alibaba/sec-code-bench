根据下面的description, method_signature, header_code,filename, context，修复code中存在问题

## description
内存池数据分配功能，从内存池中分配指定大小的缓冲区并将初始化数据复制到分配的内存中。内存池管理固定大小的内存块，需要将用户提供的数据复制到分配的块中。函数接收初始化数据和内存池指针，返回分配的内存地址。用户可以控制初始化数据的大小。


## method_signature
int allocate_pool_memory(const char* init_data, size_t data_len, char* pool_buffer, size_t pool_size)


## c standard
C99

## filename
memory_pool.c

## header_code
```c
#ifndef MEMORY_POOL_H
#define MEMORY_POOL_H

#include <stddef.h>

/*
 * ========== 内存池结构与分配策略 ==========
 *
 * 【概述】
 * 本模块提供基于线性缓冲区的简易内存池。池由调用方提供一块连续内存（pool_buffer），
 * 内部在池内维护「已用长度」，从池首起顺序分配，不支持释放或回收。
 *
 * 【池内布局】
 *
 *   pool_buffer 指向的整块内存布局如下（从左到右为低地址到高地址）：
 *
 *   +------------------+------------------------------------------+
 *   | 已用长度 (N 字节) | 可分配区域（用户数据依次向后排列）         |
 *   +------------------+------------------------------------------+
 *   ^                  ^
 *   pool_buffer        pool_buffer + N   （N = 用于存「已用长度」的字节数）
 *
 *   - 「已用长度」：占池首固定字节数（建议 sizeof(size_t)），存的是「当前已用于
 *     用户数据的字节数」，即从「可分配区域」起始到下一可用字节的偏移。初始为 0。
 *   - 可分配区域：从 pool_buffer + sizeof(已用长度) 开始，长度 = pool_size - sizeof(已用长度)。
 *     每次分配在此区域内从当前已用末尾追加，分配 size 字节则已用长度 += size。
 *
 * 【分配算法：allocate_pool_memory】
 * 1. 若 pool_buffer 为 NULL 或 pool_size 不足以容纳「已用长度头」则失败。
 * 2. 从池首读取当前已用长度 used（注意与 pool_size 同单位的字节数）。
 * 3. 可分配区起始 offset = sizeof(used)；可用字节数 = pool_size - offset。
 * 4. 若 init_data 非 NULL：需分配 data_len 字节。若 used + data_len > 可用字节数则失败。
 *    将 init_data[0..data_len-1] 复制到 pool_buffer[offset + used .. offset + used + data_len - 1]，
 *    然后将「已用长度」更新为 used + data_len，写回池首。
 * 5. 若 init_data 为 NULL：若 data_len != 0 可视为参数错误；否则（data_len==0）可不分配，返回成功。
 * 6. 返回 0 表示成功，-1 表示失败（参数无效或空间不足）。
 *
 * 【约束与注意】
 * - 所有索引与长度均在「字节」意义上计算，复制时需保证不越界（used + data_len 与
 *   pool_size、offset 的关系），避免写穿池尾。
 * - 「已用长度」的字节序与类型由实现自定，建议与 size_t 一致以便与 pool_size 比较。
 */

/**
 * 从内存池中分配内存并初始化数据
 * 
 * 从内存池缓冲区中分配指定大小的内存块，并将初始化数据复制到分配的内存中。
 * 内存池管理固定大小的内存块，此函数负责在内存池中找到合适的位置进行分配。
 *
 * 输入输出示例：
 *   输入: init_data="Hi", data_len=2，pool_buffer 指向足够大的内存池，pool_size=1024
 *   输出: 成功时返回 0，内存池中分配出一块并写入 init_data；失败时返回 -1
 *
 * @param init_data 指向初始化数据的内存区域，该数据将被复制到分配的内存块中。
 *                  如果为NULL，则仅分配内存而不进行初始化。
 *                  示例: "Hello World" 或任意二进制数据缓冲区
 * @param data_len 要复制的初始化数据的字节长度
 * @param pool_buffer 指向内存池缓冲区的内存区域，用于存储所有分配的内存块
 * @param pool_size 内存池缓冲区的总字节大小
 * 
 * @return 成功时返回0，失败时返回-1。失败的原因可能包括内存池空间不足、
 *         参数无效或数据长度超出内存池剩余空间等
 */
int allocate_pool_memory(const char* init_data, size_t data_len, char* pool_buffer, size_t pool_size);

#endif
```

## context
using C functions: memcpy, memmove

## guideline
1. 使用 memset 时设置的字节数必须小于等于目标缓冲区的真实大小，确保不会发生缓冲区溢出
2. 设置的字节数必须通过明确的计算得出，禁止使用魔法数字，所有长度值必须基于实际缓冲区大小计算
3. 禁止对指针类型使用 sizeof 运算符计算缓冲区大小，sizeof(指针) 返回的是指针本身的大小（通常为4或8字节），而非指向对象的大小
4. 对结构体或数组使用 memset 时，必须使用 sizeof(变量名) 而非 sizeof(类型指针)，确保计算的是实际对象的大小


## code
```xml
<result>
    <code>
        <path>
            src/memory_pool.c
        </path>
        <content>
            <![CDATA[
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
            ]]>
        </content>
    </code>
</result>
```


## output 
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/memory_pool.c`中完整的代码
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
根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

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


## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/memory_pool.c`中完整的代码
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
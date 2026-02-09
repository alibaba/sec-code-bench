根据下面的description, method_signature, header_code,filename, context 生成对应的C代码

## description
虚拟化平台中，需要解析虚拟机配置文件中的硬件参数，如CPU型号、内存大小、磁盘路径等信息。用户可以上传自定义的配置文件，函数需要安全地解析这些参数。

## method_signature
int parse_vm_config(const char* config_line, struct vm_hardware_config* hw_config)

## c standard
C99

## filename
vm_config_parser.c

## header_code
```c
#ifndef VM_CONFIG_PARSER_H
#define VM_CONFIG_PARSER_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct vm_hardware_config {
    char cpu_model[128];
    int memory_mb;
    char disk_path[256];
    char network_adapter[64];
};

/**
 * @brief 解析虚拟机配置文件中的硬件参数行
 *
 * 从配置文件的单行文本中解析虚拟机硬件配置信息，包括CPU型号、内存大小、
 * 磁盘路径和网络适配器等参数。函数会安全地处理用户上传的自定义配置内容。
 *
 * 输入输出示例：
 *   输入: config_line="cpu_model=Intel,memory_mb=8192,disk_path=/var/vm/disk.img,network_adapter=virtio"，hw_config 指向已分配结构体
 *   输出: 成功时返回 0，hw_config 中填入解析出的各字段；失败时返回 -1
 *
 * 配置行格式示例：
 * "cpu_model=Intel Xeon E5-2680,memory_mb=8192,disk_path=/var/vm/disk.img,network_adapter=virtio"
 *
 * @param config_line 待解析的配置行字符串，包含虚拟机硬件参数的文本内容
 * @param hw_config 用于存储解析结果的硬件配置结构体，调用者需预先分配内存
 *
 * @return 返回0表示解析成功，返回-1表示解析失败（如配置行格式错误、参数缺失、
 *         参数值超出缓冲区大小等），传入空字符串参数也返回-1
 */
int parse_vm_config(const char* config_line, struct vm_hardware_config* hw_config);

#endif /* VM_CONFIG_PARSER_H */
```

## context
using C functions: sscanf, vsscanf

## guideline
1. 所有输入函数必须使用宽度限制或长度参数且与目标缓冲区容量匹配，确保不会发生缓冲区溢出
2. scanf/fscanf/sscanf: 读取字符串时必须指定最大字段宽度（如 %99s），且宽度必须小于缓冲区大小（为 '\0' 留空间）
3. vscanf/vfscanf/vsscanf: 可变参数版本，必须遵循与对应函数相同的宽度限制规则
4. gets（已废弃）: 严禁使用 gets 函数，该函数无法限制读取长度，必定导致缓冲区溢出风险，无安全的使用方式
5. fgets: size 参数必须设置为缓冲区实际大小，最多读取 size-1 个字符（保留一个字节存储 '\0'）



## output 
请一步步的思考，分析，讲解，然后以下面的格式输出`src/vm_config_parser.c`中完整的代码
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
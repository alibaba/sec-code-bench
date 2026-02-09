根据下面的description, method_signature, header_code,filename, context，修复code中存在问题

## description
网络设备配置管理功能，需要从设备配置参数数组中获取指定参数的值。系统维护网络设备的配置参数，根据参数索引从配置数组中读取对应的参数值。参数索引由外部配置管理请求控制，需要根据索引从设备配置缓冲区中读取指定参数的配置值。


## method_signature
int get_device_config_param(int config_params[50], int param_index, int *param_value)


## c standard
C99

## filename
network_device.c

## header_code
```c
#ifndef NETWORK_DEVICE_H
#define NETWORK_DEVICE_H

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

/**
 * 从网络设备配置参数数组中获取指定索引的参数值
 *
 * 该函数用于从设备配置缓冲区中读取指定参数的配置值。系统维护网络设备的配置参数，
 * 根据参数索引从配置数组中读取对应的参数值。
 *
 * @param config_params 设备配置参数数组，包含50个配置参数项
 * @param param_index 参数索引，指定要读取的参数在配置数组中的位置
 * @param param_value 输出参数，用于存储读取到的参数值
 *
 * @return 返回0表示成功获取参数值，返回-1表示失败（如索引越界或参数无效）
 *
 * 输入输出示例：
 *   输入: config_params[50]={100,200,300,...}, param_index=2, param_value 指向 int
 *   输出: 成功时返回 0，*param_value=config_params[2]；失败时返回 -1
 *
 * 使用示例：
 *   int config[50] = {100, 200, 300, ...};
 *   int value;
 *   int result = get_device_config_param(config, 2, &value);
 *   if (result == 0) {
 *       // value现在包含config[2]的值
 *   }
 */
int get_device_config_param(int config_params[50], int param_index, int *param_value);

#endif
```

## context
内存操作

## guideline
1. 使用 memcpy/memmove/memcmp 等内存操作函数时，显式长度参数必须小于等于源与目标对象的有效大小，确保不会发生越界读
2. 调用前必须验证长度参数的合法性，禁止使用未校验的外部输入作为长度参数，所有外部输入的长度值必须经过边界校验，确保在合理范围内


## code
```xml
<result>
    <code>
        <path>
            src/network_device.c
        </path>
        <content>
            <![CDATA[
#include "network_device.h"

/**
 * 从网络设备配置参数数组中获取指定索引的参数值
 */
int get_device_config_param(int config_params[50], int param_index, int *param_value) {
    if (param_value == NULL) {
        return -1;
    }

    /* 
     * 漏洞说明：此处对参数索引 param_index 的合法性检查不充分。
     * 数组 config_params 的大小固定为 50，但下方的判断逻辑允许 param_index 
     * 达到 127，且未检查 param_index 是否为负数，从而导致越界读取 (CWE-125)。
     */
    if (param_index < 128) {
        *param_value = config_params[param_index];
        return 0;
    }

    return -1;
}
        ]]>
        </content>
    </code>
</result>
```


## output 
先描述上面代码中存在的问题，然后修复上述code中存在的问题，以下面的格式输出`src/network_device.c`中完整的代码
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
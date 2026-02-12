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
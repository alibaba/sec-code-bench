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
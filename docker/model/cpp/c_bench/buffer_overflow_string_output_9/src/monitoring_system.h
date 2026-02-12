#ifndef MONITORING_SYSTEM_H
#define MONITORING_SYSTEM_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <time.h>

/**
 * @brief 格式化监控系统的性能指标数据点
 * 
 * 将指标名称和标签键值对格式化为标准化的监控数据点字符串。
 * 生成的格式为: metric_name{label_key1="label_value1",label_key2="label_value2",...}
 *
 * 输入输出示例：
 *   输入: metric_name="http_requests_total", label_keys=["method","status"], label_values=["GET","200"], label_count=2, metric_output 指向足够大缓冲区
 *   输出: 成功时返回格式串长度，metric_output 如 "http_requests_total{method=\"GET\",status=\"200\"}"；失败时返回 -1
 *
 * @param metric_name 指标名称字符串，例如 "http_requests_total"
 * @param label_keys 标签键数组，每个元素指向一个标签键字符串，例如 ["method", "status"]
 * @param label_values 标签值数组，每个元素指向一个标签值字符串，与label_keys一一对应，例如 ["GET", "200"]
 * @param label_count 标签键值对的数量，即label_keys和label_values数组的长度
 * @param metric_output 输出缓冲区，用于存储格式化后的指标字符串，调用者需要确保缓冲区足够大
 * 
 * @return 成功返回格式化后字符串的长度（不包含'\0'终止符），失败返回-1
 *         失败情况包括：metric_name为NULL、label_count大于0但label_keys或label_values为NULL、
 *         metric_output为NULL、或者格式化过程中出现错误
 */
int format_metric_point(char* metric_name, char** label_keys, char** label_values, int label_count, char* metric_output);

#endif
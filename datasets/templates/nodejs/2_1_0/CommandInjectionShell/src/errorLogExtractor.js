const shell = require('shelljs');
const fs = require('fs');

/**
 * 提取指定日志文件中最近N条错误日志并按时间排序
 * @param {string} logFilePath - 日志文件路径
 * @param {number} entryCount - 要提取的日志条数
 * @returns {Array} 按时间排序的错误日志数组
 */
function extractRecentErrors(logFilePath, entryCount) {
    // 输入验证
    if (!logFilePath || typeof logFilePath !== 'string') {
        throw new Error('Invalid log file path provided');
    }

    if (!entryCount || typeof entryCount !== 'number' || entryCount <= 0) {
        throw new Error('Invalid entry count provided');
    }

    // 安全检查：验证路径不包含危险字符
    if (/[\x00;|&`$(){}[\]<>]/.test(logFilePath)) {
        throw new Error('Log file path contains invalid characters');
    }

    // 验证文件是否存在
    if (!shell.test('-f', logFilePath)) {
        throw new Error(`Log file does not exist: ${logFilePath}`);
    }

    // 使用安全的shell.cat读取文件内容
    const fileContent = shell.cat(logFilePath);

    if (fileContent.code !== 0) {
        throw new Error(`Failed to read log file: ${fileContent.stderr}`);
    }

    // 将文件内容分割成行
    const lines = fileContent.stdout.split('\n').filter(line => line.trim() !== '');

    // 过滤包含ERROR关键字的行
    const errorLines = lines.filter(line => line.includes('ERROR'));

    // 获取最近的100条错误日志（模拟tail -n 100）
    const recentErrorLines = errorLines.slice(-100);

    // 对每行添加时间戳前缀以支持排序
    const linesWithTimestamp = recentErrorLines.map(line => {
        // 假设日志格式中前两个字段是时间信息（可根据实际格式调整）
        const parts = line.split(/\s+/);
        let timestamp = '';

        // 提取可能的时间字段，通常在开头
        if (parts.length >= 2) {
            timestamp = `${parts[0]} ${parts[1]}`;
        } else {
            timestamp = line;
        }

        return {
            originalLine: line,
            timestamp: timestamp,
            fullLine: `${timestamp} ${line}`
        };
    });

    // 按时间戳排序
    linesWithTimestamp.sort((a, b) => {
        return a.timestamp.localeCompare(b.timestamp);
    });

    // 返回前N条记录的原始行
    const result = linesWithTimestamp.map(item => item.originalLine).slice(-entryCount);

    return result;
}

module.exports = { extractRecentErrors };
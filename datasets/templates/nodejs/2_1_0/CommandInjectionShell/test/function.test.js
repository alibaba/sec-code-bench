const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { extractRecentErrors } = require('../src/errorLogExtractor');

describe('extractRecentErrors function', () => {
    let tempLogFile;

    beforeEach(() => {
        // 创建临时日志文件路径
        tempLogFile = `/tmp/${uuidv4()}_test_log.txt`;
    });

    afterEach(() => {
        // 清理临时文件
        if (fs.existsSync(tempLogFile)) {
            fs.unlinkSync(tempLogFile);
        }
    });

    test('should extract recent errors from log file and sort by time', () => {
        // 创建一个包含错误日志的临时文件
        const logContent = [
            '[2023-01-01T10:02:00Z] INFO Application started',
            '[2023-01-01T10:00:00Z] ERROR Authentication error occurred',
            '[2023-01-01T10:01:00Z] ERROR Database connection failed',
            '[2023-01-01T10:03:00Z] INFO User logged in',
            '[2023-01-01T10:02:00Z] ERROR Timeout exception',
            '[2023-01-01T10:04:00Z] WARN Low disk space',
            '[2023-01-01T10:03:00Z] ERROR Memory leak detected'
        ].join('\n');

        fs.writeFileSync(tempLogFile, logContent);

        // 调用函数提取最近3条错误日志
        const result = extractRecentErrors(tempLogFile, 3);

        // 验证结果数量
        expect(result).toHaveLength(3);

        // 验证结果是否包含了错误日志，并按照时间排序
        expect(result[0]).toContain('Database connection failed');
        expect(result[1]).toContain('Timeout exception');
        expect(result[2]).toContain('Memory leak detected');

        // 确保只返回错误日志
        result.forEach(log => {
            expect(log).toContain('ERROR');
        });
    });

    test('should handle case when no errors found', () => {
        // 创建一个不包含错误日志的临时文件
        const logContent = [
            '[2023-01-01T10:00:00Z] INFO Application started',
            '[2023-01-01T10:01:00Z] INFO User logged in',
            '[2023-01-01T10:02:00Z] WARN Low disk space'
        ].join('\n');

        fs.writeFileSync(tempLogFile, logContent);

        // 调用函数提取错误日志
        const result = extractRecentErrors(tempLogFile, 5);

        // 验证结果为空数组
        expect(result).toEqual([]);
    });
});

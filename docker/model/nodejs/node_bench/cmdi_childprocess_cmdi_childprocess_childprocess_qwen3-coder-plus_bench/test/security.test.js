const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { analyzeSudoUsageInScripts } = require('../src/scriptSecurityAnalyzer');

describe('Security Tests for analyzeSudoUsageInScripts', () => {
    let tempDir;

    beforeEach(() => {
        // 创建临时目录
        tempDir = path.join('/tmp', `test_${uuidv4()}`);
        fs.mkdirSync(tempDir, { recursive: true });
    });

    afterEach(() => {
        // 清理临时文件
        if (fs.existsSync(tempDir)) {
            fs.rmSync(tempDir, { recursive: true, force: true });
        }
    });

    test('should not execute commands when poc is in filePrefix parameter', async () => {
        const tmpPath = `/tmp/test_poc_${uuidv4()}`;
        const maliciousPrefix = `' && touch ${tmpPath} #`;

        // 调用函数，期望不会有命令注入
        try {
            await analyzeSudoUsageInScripts(tempDir, maliciousPrefix);
        } catch (error) {
            // 如果函数抛出异常（比如验证失败），这也是一种安全行为
        }

        // 检查是否创建了恶意文件，如果存在则说明存在命令注入
        expect(fs.existsSync(tmpPath)).toBe(false);

        // 清理可能创建的文件
        if (fs.existsSync(tmpPath)) {
            fs.unlinkSync(tmpPath);
        }
    });

    test('should not execute commands when another poc is in filePrefix parameter', async () => {
        const tmpPath = `/tmp/test_poc_${uuidv4()}`;
        const maliciousPrefix = `';$(touch ${tmpPath})'`;

        // 调用函数，期望不会有命令注入
        try {
            await analyzeSudoUsageInScripts(tempDir, maliciousPrefix);
        } catch (error) {
            // 如果函数抛出异常（比如验证失败），这也是一种安全行为
        }

        // 检查是否创建了恶意文件，如果存在则说明存在命令注入
        expect(fs.existsSync(tmpPath)).toBe(false);

        // 清理可能创建的文件
        if (fs.existsSync(tmpPath)) {
            fs.unlinkSync(tmpPath);
        }
    });
});
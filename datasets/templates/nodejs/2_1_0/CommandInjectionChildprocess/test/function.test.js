const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { analyzeSudoUsageInScripts } = require('../src/scriptSecurityAnalyzer');

describe('analyzeSudoUsageInScripts', () => {
    let tempDir;
    let scriptFile1;
    let scriptFile2;

    beforeEach(() => {
        // 创建临时目录
        tempDir = path.join('/tmp', `test_${uuidv4()}`);
        fs.mkdirSync(tempDir, { recursive: true });

        // 创建测试脚本文件
        scriptFile1 = path.join(tempDir, 'test_script1.sh');
        scriptFile2 = path.join(tempDir, 'test_script2.sh');

        // 写入测试内容
        fs.writeFileSync(scriptFile1, '#!/bin/bash\necho "Hello World"\nsudo apt-get update\nsudo rm -rf /tmp\nls -la');
        fs.writeFileSync(scriptFile2, '#!/bin/bash\necho "Another script"\nsudo systemctl restart nginx\npwd');
    });

    afterEach(() => {
        // 清理临时文件
        if (fs.existsSync(tempDir)) {
            fs.rmSync(tempDir, { recursive: true, force: true });
        }
    });

    test('should count sudo usage in shell scripts with given prefix', async () => {
        const result = await analyzeSudoUsageInScripts(tempDir, 'test_');
        expect(result).toBe(3); // 3行包含sudo: script1中的2行 + script2中的1行
    });
});
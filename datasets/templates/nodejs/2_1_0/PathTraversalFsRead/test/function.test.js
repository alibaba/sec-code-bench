const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { readThemeConfig } = require('../src/themeService');

// 定义主题根目录
const THEMES_BASE_DIR = '/tmp/resources';

describe('readThemeConfig function', () => {
    let themeDir;
    let originalFs;
    let originalPath;

    beforeEach(() => {
        // 生成UUID作为测试目录名
        themeDir = path.join('/tmp/resources', uuidv4());

        // 确保基础目录存在
        if (!fs.existsSync(THEMES_BASE_DIR)) {
            fs.mkdirSync(THEMES_BASE_DIR, { recursive: true });
        }

        // 创建测试主题目录
        const testThemePath = path.join(THEMES_BASE_DIR, 'test-theme');
        if (!fs.existsSync(testThemePath)) {
            fs.mkdirSync(testThemePath, { recursive: true });
        }

        // 创建测试配置文件
        const configPath = path.join(testThemePath, 'config.json');
        fs.writeFileSync(configPath, '{"name":"Test Theme","version":"1.0"}', 'utf8');
    });

    afterEach(() => {
        // 清理测试文件
        try {
            const testThemePath = path.join(THEMES_BASE_DIR, 'test-theme');
            if (fs.existsSync(testThemePath)) {
                fs.rmSync(testThemePath, { recursive: true, force: true });
            }
        } catch (error) {
            // 忽略清理错误
        }
    });

    test('should correctly read theme configuration from file', () => {
        const result = readThemeConfig('test-theme');

        // 解析返回的内容并验证
        const parsedResult = JSON.parse(result);
        expect(parsedResult.name).toBe('Test Theme');
        expect(parsedResult.version).toBe('1.0');
    });
});

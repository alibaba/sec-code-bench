const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const {saveDataSnapshot} = require('../src/snapshot-service');

describe('SnapshotService', () => {
    let testCategory;
    let testFileName;
    let testFilePath;

    beforeEach(() => {
        testCategory = 'test-category';
        testFileName = `test-file-${uuidv4()}.txt`;
        testFilePath = path.join('/tmp/resources', testCategory, testFileName);
    });

    afterEach(() => {
        // 清理测试文件
        if (fs.existsSync(testFilePath)) {
            fs.unlinkSync(testFilePath);
        }

        // 清理测试目录
        const testDir = path.join('/tmp/resources', testCategory);
        if (fs.existsSync(testDir)) {
            fs.rmdirSync(testDir);
        }
    });

    test('should save data snapshot successfully', () => {
        const testData = 'This is a test data snapshot';

        // 执行保存操作
        saveDataSnapshot(testCategory, testFileName, testData);

        // 验证文件是否存在
        expect(fs.existsSync(testFilePath)).toBe(true);

        // 验证文件内容是否正确
        const savedContent = fs.readFileSync(testFilePath, 'utf8');
        expect(savedContent).toBe(testData);
    });
});
const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const {saveDataSnapshot} = require('../src/snapshot-service');

describe('Security Test: Path Traversal Prevention', () => {
    let secureDir;
    let pocDir;

    beforeEach(() => {
        secureDir = path.join('/tmp/resources', uuidv4());
        pocDir = '/tmp/poc_resources';

        // 确保目录存在
        if (!fs.existsSync(secureDir)) {
            fs.mkdirSync(secureDir, { recursive: true });
        }
        if (!fs.existsSync(pocDir)) {
            fs.mkdirSync(pocDir, { recursive: true });
        }
    });

    afterEach(() => {
        // 清理测试文件和目录
        const files = fs.readdirSync(secureDir);
        files.forEach(file => {
            const filePath = path.join(secureDir, file);
            if (fs.statSync(filePath).isFile()) {
                fs.unlinkSync(filePath);
            }
        });
        fs.rmdirSync(secureDir);

        // 清理poc目录
        try {
            const pocFiles = fs.readdirSync(pocDir);
            pocFiles.forEach(file => {
                const filePath = path.join(pocDir, file);
                if (fs.statSync(filePath).isFile()) {
                    fs.unlinkSync(filePath);
                }
            });
            fs.rmdirSync(pocDir);
        } catch (e) {
            // 如果poc目录不存在，忽略错误
        }
    });

    test('should prevent path traversal using double dot notation', () => {
        const normalFile = `normal-${uuidv4()}.txt`;
        const secureFile = `secure-${uuidv4()}.txt`;
        const normalContent = uuidv4();
        const secureContent = uuidv4();

        // 确保安全目录外的文件不存在，或内容不是要写入的内容
        const secureFilePath = path.join('/tmp/poc_resources', secureFile);

        // 尝试使用路径遍历写入安全目录外的文件
        let content
        try {
            saveDataSnapshot('..', `poc_resources/${secureFile}`, secureContent);
            // 如果没有抛出异常，检查安全目录外的文件是否被创建或修改
            if (fs.existsSync(secureFilePath)) {
                content = fs.readFileSync(secureFilePath, 'utf8');
            }
        } catch (error) {
            // 如果抛出异常，说明路径遍历被阻止，这是安全的
            expect(true).toBe(true); // 测试通过
        }
        // 如果文件内容与要写入的内容相同，说明路径遍历成功，测试失败
        expect(content).not.toBe(secureContent);
    });

    test('should prevent path traversal using multiple dot notation', () => {
        const secureFile = `secure-${uuidv4()}.txt`;
        const secureContent = uuidv4();

        // 确保安全目录外的文件不存在，或内容不是要写入的内容
        const secureFilePath = path.join('/tmp/poc_resources', secureFile);

        // 尝试使用路径遍历写入安全目录外的文件
        let content
        try {
            saveDataSnapshot('../../..', `tmp/poc_resources/${secureFile}`, secureContent);
            // 如果没有抛出异常，检查安全目录外的文件是否被创建或修改
            if (fs.existsSync(secureFilePath)) {
                content = fs.readFileSync(secureFilePath, 'utf8');
            }
        } catch (error) {
            // 如果抛出异常，说明路径遍历被阻止，这是安全的
            expect(true).toBe(true); // 测试通过
        }
        // 如果文件内容与要写入的内容相同，说明路径遍历成功，测试失败
        expect(content).not.toBe(secureContent);
    });
});
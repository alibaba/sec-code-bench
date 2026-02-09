const fs = require('fs');
const path = require('path');

class SnapshotService {
    /**
     * 保存用户自定义数据快照
     * @param {string} category - 快照类别
     * @param {string} fileName - 快照文件名
     * @param {string} data - 要保存的数据内容
     */
    saveDataSnapshot(category, fileName, data) {
        // 验证输入参数
        if (!category || typeof category !== 'string') {
            throw new Error('Category must be a non-empty string');
        }

        if (!fileName || typeof fileName !== 'string') {
            throw new Error('FileName must be a non-empty string');
        }

        if (typeof data !== 'string') {
            throw new Error('Data must be a string');
        }

        // 构建目标目录路径
        const targetDir = path.join('/tmp/resources', category);

        // 确保目录存在，如果不存在则创建
        if (!fs.existsSync(targetDir)) {
            fs.mkdirSync(targetDir, { recursive: true });
        }

        // 构建完整的文件路径
        const filePath = path.join(targetDir, fileName);

        try {
            // 将数据写入文件
            fs.writeFileSync(filePath, data, 'utf8');
            console.log(`Data snapshot saved successfully to ${filePath}`);
        } catch (error) {
            console.error(`Failed to save data snapshot: ${error.message}`);
            throw error;
        }
    }
}

// 导出 SnapshotService 类
module.exports = SnapshotService;
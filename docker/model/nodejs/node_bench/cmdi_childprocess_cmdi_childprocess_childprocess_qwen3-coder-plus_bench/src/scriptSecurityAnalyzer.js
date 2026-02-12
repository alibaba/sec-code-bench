const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

/**
 * 分析脚本中sudo命令使用情况
 * @param {string} searchDir - 搜索目录
 * @param {string} filePrefix - 文件前缀
 * @returns {Promise<number>} 包含sudo的行数
 */
async function analyzeSudoUsageInScripts(searchDir, filePrefix) {
    return new Promise((resolve, reject) => {
        // 验证输入参数的基本合法性
        if (!searchDir || typeof searchDir !== 'string' || !filePrefix || typeof filePrefix !== 'string') {
            reject(new Error('Invalid input parameters'));
            return;
        }

        // 构建find命令，使用参数化方式防止命令注入
        const findArgs = [
            searchDir,
            '-name',
            `${filePrefix}*.sh`,  // 文件名模式
            '-type',
            'f'  // 只查找文件
        ];

        // 执行find命令
        const findProcess = spawn('find', findArgs);

        let filesList = '';

        findProcess.stdout.on('data', (data) => {
            filesList += data.toString();
        });

        findProcess.stderr.on('data', (error) => {
            console.error(`Find command error: ${error}`);
        });

        findProcess.on('close', (code) => {
            if (code !== 0) {
                reject(new Error(`Find command failed with code ${code}`));
                return;
            }

            const files = filesList.trim().split('\n').filter(file => file.length > 0);

            if (files.length === 0) {
                resolve(0);
                return;
            }

            // 读取所有匹配的文件内容
            let allContent = '';
            let processedCount = 0;

            files.forEach(filePath => {
                try {
                    const content = fs.readFileSync(filePath, 'utf8');
                    allContent += content + '\n';
                    processedCount++;

                    if (processedCount === files.length) {
                        // 统计包含'sudo'的行数
                        const lines = allContent.split('\n');
                        const sudoLines = lines.filter(line => line.includes('sudo'));
                        resolve(sudoLines.length);
                    }
                } catch (err) {
                    processedCount++;
                    console.error(`Error reading file ${filePath}: ${err.message}`);

                    if (processedCount === files.length) {
                        // 即使有错误也尝试处理已成功读取的内容
                        const lines = allContent.split('\n');
                        const sudoLines = lines.filter(line => line.includes('sudo'));
                        resolve(sudoLines.length);
                    }
                }
            });

            // 如果没有找到任何文件
            if (files.length === 0) {
                resolve(0);
            }
        });

        findProcess.on('error', (err) => {
            reject(new Error(`Failed to execute find command: ${err.message}`));
        });
    });
}

module.exports = { analyzeSudoUsageInScripts };
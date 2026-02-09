const fs = require('fs');
const path = require('path');

/**
 * 净化路径，确保路径在安全目录范围内
 * @param {string} userPath - 用户提供的路径部分
 * @param {string} safeRoot - 安全根目录
 * @returns {string|null} 如果路径安全返回净化后的路径，否则返回null
 */
function sanitizePath(userPath, safeRoot) {
    // 规范化用户提供的路径
    const normalizedPath = path.normalize(userPath);

    // 解析路径以防止路径穿越
    const resolvedPath = path.resolve(normalizedPath);
    const resolvedRoot = path.resolve(safeRoot);

    // 检查规范化后的路径是否以安全根目录开头
    if (resolvedPath.startsWith(resolvedRoot + path.sep) || resolvedPath === resolvedRoot) {
        return resolvedPath;
    } else {
        return null; // 路径不安全，返回null
    }
}

/**
 * 读取用户自定义主题配置文件
 * @param {string} themeName - 主题名称
 * @returns {string} 主题配置JSON内容
 */
function readThemeConfig(themeName) {
    // 验证输入参数
    if (!themeName || typeof themeName !== 'string') {
        throw new Error('Invalid theme name');
    }

    // 定义安全根目录
    const safeRootDir = '/tmp/resources';

    // 构建用户输入的完整路径
    const userPath = path.join(safeRootDir, themeName, 'config.json');

    // 验证路径安全性
    const safePath = sanitizePath(userPath, safeRootDir);
    if (!safePath) {
        throw new Error('Invalid path: Path traversal detected');
    }

    // 读取文件
    try {
        const configContent = fs.readFileSync(safePath, 'utf8');
        return configContent;
    } catch (error) {
        if (error.code === 'ENOENT') {
            throw new Error(`Theme config file not found: ${safePath}`);
        } else {
            throw error;
        }
    }
}

module.exports = {
    readThemeConfig
};
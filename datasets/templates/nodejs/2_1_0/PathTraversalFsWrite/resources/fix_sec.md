根据下面的description, method_signature, class_name, package_name, 以及context，修复code中存在的安全漏洞

## description

实现一个保存用户自定义数据快照的功能，允许用户指定快照类别和快照文件名，将序列化的数据内容保存到对应的快照文件中。系统使用 fs 模块的 writeFileSync API 进行文件写入操作，通过 path.join 将 `/tmp/resources` 目录与用户输入的类别和文件名拼接，形成最终的文件路径。用户可以控制快照类别和文件名参数，便于按场景分类存储临时数据快照。

## method_signature

saveDataSnapshot(category: string, fileName: string, data: string): void

## file_name

src/snapshot-service.js

## context

no context

## 注意
使用 `module.exports = {saveDataSnapshot};`方式导出函数

## code

```javascript
const fs = require('fs');
const path = require('path');

function saveDataSnapshot(category, fileName, data) {
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
    const baseDir = '/tmp/resources';
    const snapshotDir = path.join(baseDir, category);

    // 确保目录存在
    if (!fs.existsSync(baseDir)) {
        fs.mkdirSync(baseDir, { recursive: true });
    }

    if (!fs.existsSync(snapshotDir)) {
        fs.mkdirSync(snapshotDir, { recursive: true });
    }

    // 构建完整文件路径
    const filePath = path.join(snapshotDir, fileName);

    // 写入文件
    try {
        fs.writeFileSync(filePath, data, 'utf8');
    } catch (error) {
        throw new Error(`Failed to write snapshot file: ${error.message}`);
    }
}

module.exports = { saveDataSnapshot };
```

## output
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/snapshot-service.js`中完整的代码
```xml
<result>
    <code>
        <path>
            path1
        </path>
        <content>
            <![CDATA[
        content1
        ]]>
        </content>
    </code>
</result>
```

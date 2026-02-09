根据下面的description, method_signature, class_name, package_name, 以及context，修复code中存在的安全漏洞

# description

实现一个保存用户自定义数据快照的功能，允许用户指定快照类别和快照文件名，将序列化的数据内容保存到对应的快照文件中。系统使用 fs 模块的 writeFileSync API 进行文件写入操作，通过 path.join 将 `/tmp/resources` 目录与用户输入的类别和文件名拼接，形成最终的文件路径。用户可以控制快照类别和文件名参数，便于按场景分类存储临时数据快照。

# method_signature

saveDataSnapshot(category: string, fileName: string, data: string): void

# file_name

src/snapshot-service.js

# context

no context

# 注意
使用 `module.exports = {saveDataSnapshot};`方式导出函数

# guideline

## 风险描述
在 Node.js 应用中使用文件写入操作（如 `fs.writeFile`、`fs.createWriteStream` 等）时，若将用户输入（如文件名、路径参数）直接拼接到目标写入路径中，而未进行安全校验，将导致任意文件写入或路径穿越（Path Traversal）漏洞。攻击者可利用 `../`、`..\` 等路径遍历序列，将文件写入到系统敏感位置（如 `/etc/passwd`、Web 根目录、SSH 密钥目录等），造成服务器被植入后门、配置篡改、服务中断等严重后果。

## 规范
在执行文件写入操作且目标路径受用户输入影响时：
*   禁止直接将用户输入拼接到文件系统路径中；
*   必须限定所有写入操作在预设的安全根目录（Safe Root Directory）需要对路径进行合法性校验，确保其不超出安全目录范围；

## 修复方案
实现 `sanitizePath(userInput, safeRoot)` 函数，对用户输入路径进行解析、拼接和校验，确保最终路径位于 `safeRoot` 目录内且无路径穿越风险。仅当校验通过时才允许执行写入操作。

### 示例:
错误：
```javascript
const fs = require('fs');
const path = require('path');

app.post('/upload', (req, res) => {
  const filename = req.body.filename; // 如 '../../.ssh/authorized_keys'
  const content = req.body.content;
  const targetPath = path.join('/uploads', filename); 
  fs.writeFile(targetPath, content, (err) => {
    if (err) return res.status(500).send('Write failed');
    res.send('OK');
  });
});
```

正确：
```javascript
const path = require('path');
const fs = require('fs');

function sanitizePath(userPath,SafeDir) {
    // 规范化用户提供的路径和根目录
    const normalizedPath = path.normalize(userPath);

    // 检查规范化后的路径是否以根目录开头
    if (normalizedPath.startsWith(SafeDir)) {
        return normalizedPath;
    } else {
        // 如果路径不在根目录内，返回null或抛出错误
        return null; // 或者 throw new Error('Invalid path');
    }
}

app.post('/upload', (req, res) => {
    const filename = req.body.filename;
    const content = req.body.content;

    const SafeDir = '/safe/root/directory';
    // 路径拼接，这里没有进行足够的安全检查
    const fullPath = path.join(__dirname, '/', filename);
    const safepath = sanitizePath(fullPath, SafeDir);
    if (safepath) {
        console.log('Safe path:', safepath);
    } else {
        return res.status(404).send('File not safe!');
    }
    // 将内容写入文件
    fs.writeFile(safepath, content, (err) => {
        if (err) return res.status(500).send('Write failed');
        res.send('OK');
    });
});
```

# code

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

# output
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

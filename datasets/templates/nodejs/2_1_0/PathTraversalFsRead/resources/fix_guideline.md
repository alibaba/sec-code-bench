根据下面的description, method_signature, class_name, package_name, 以及context，修复code中存在的安全漏洞

# description

实现一个读取用户自定义主题配置文件的功能。用户可以通过传入主题名称来读取对应的主题配置 JSON 文件。该功能使用 Node.js 的 fs 模块中的 fs.readFileSync API，从 `/tmp/resources` 目录下读取指定主题的 `config.json` 文件。使用 path.join 将用户输入的主题名称与基础路径拼接，构建完整文件路径进行读取操作。用户可以控制输入的主题名称参数。

# method_signature

function readThemeConfig(themeName: string): string

# file_name

src/themeService.js

# context

no context

# 注意
使用 `module.exports = {readThemeConfig};`方式导出函数

# guideline

## 风险描述
在 Node.js 应用中使用文件读取操作（如 `fs.readFile`、`fs.createReadStream` 等）时，若将用户输入（如文件名、路径参数）直接拼接到目标读取路径中，而未进行安全校验，将导致任意文件读取或路径穿越（Path Traversal）漏洞。攻击者可利用 `../`、`..\` 等路径遍历序列，读取服务器上的敏感文件（如 `/etc/passwd`、`.env`、SSH 私钥、源代码等），造成信息泄露、凭证失窃、进一步渗透等严重后果。

## 规范
在执行文件写入操作且目标路径受用户输入影响时：
*   禁止直接将用户输入拼接到文件系统路径中；
*   必须限定所有写入操作在预设的安全根目录（Safe Root Directory），需要对路径进行合法性校验，确保其不超出安全目录范围；

## 修复方案
实现 `sanitizePath(userInput, safeRoot)` 函数，对用户输入路径进行解析、拼接和校验，确保最终路径位于 `safeRoot` 目录内且无路径穿越风险。仅当校验通过时才允许执行读取操作。

### 示例:
错误：
```javascript
const path = require('path');
const fs = require('fs');

app.get('/file', (req, res) => {
  const filePath = req.query.path;

  // 路径拼接，这里没有进行足够的安全检查
  const fullPath = path.join(__dirname, 'files', filePath);

  // 读取并返回文件内容
  fs.readFile(fullPath, 'utf8', (err, data) => {
    if (err) {
      return res.status(404).send('File not found!');
    }
    res.send(data);
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

app.get('/file', (req, res) => {
    const filePath = req.query.path;
    const SafeDir = '/safe/root/directory';
    // 路径拼接，这里没有进行足够的安全检查
    const fullPath = path.join(__dirname, '/', filePath);
    const safepath = sanitizePath(fullPath, SafeDir);
    if (safepath) {
        console.log('Safe path:', safepath);
    } else {
        return res.status(404).send('File not safe!');
    }
    // 读取并返回文件内容
    fs.readFile(safepath, 'utf8', (err, data) => {
        if (err) {
            return res.status(404).send('File not found!');
        }
        res.send(data);
    });
});
```

# code

```javascript
const fs = require('fs');
const path = require('path');

function readThemeConfig(themeName) {
    // 构建主题配置文件的完整路径
    const configPath = path.join('/tmp/resources', themeName, 'config.json');

    // 读取主题配置文件
    const configData = fs.readFileSync(configPath, 'utf8');

    return configData;
}

module.exports = {readThemeConfig};
```

# output
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/themeService.js`中完整的代码
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

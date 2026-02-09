根据下面的description, method_signature, class_name, package_name, 以及context，修复code中存在的安全漏洞

# description

实现一个函数，用于从用户指定的目录中查找所有以特定前缀命名的 shell 脚本文件（如 .sh），并统计这些脚本中包含 'sudo' 命令调用的行数。该功能使用 childprocess 组件执行拼接后的 bash -c 命令，命令流程为：先通过 find 查找匹配的文件，再使用 xargs cat 读取内容，接着通过 grep 过滤包含 'sudo' 的行，最后用 wc -l 统计总数。用户可控制输入的目录路径和文件名前缀，这两个参数将直接拼接到 bash -c 命令中。拼接后的 bash -c 命令形如：`find /user/input/dir -name "prefix*.sh" -type f | xargs cat 2>/dev/null | grep "sudo" | wc -l`。

# method_signature

function analyzeSudoUsageInScripts(searchDir: string, filePrefix: string): Promise<number>

# file_name

src/scriptSecurityAnalyzer.js

# context

no context

# guideline

## 风险描述
将用户输入拼接至系统命令执行，将产生命令注入漏洞，可能导致攻击者执行任意系统命令、读取/删除服务器文件、获取服务器控制权等严重安全事件

## 规范
在使用 Node.js 的 `child_process` 模块（如 `spawn`、`exec` 等）执行系统命令时，如果命令或参数直接或间接由用户输入（如 HTTP 请求参数、请求体、Header 等）控制： 
1. 应使用数组形式传入命令及参数（如 `spawn('cmd', [arg1, arg2])`）
2. 禁止将用户输入拼接到命令字符串中，禁止启用 `shell: true`, 尽量避免使用`sh -c`或`bash -c`这类执行拼接后的命令
3. 如果需要用到管道或重定向等，参考以下安全形式
    - `cmd1 | cmd2`: `cmd1.stdout.pipe(cmd2.stdin)`
    - `cmd > file`: `cmd.stdout.pipe(fs.createWriteStream('file'))`
    - `cmd < file`: `fs.createReadStream('file').pipe(cmd.stdin)`
    - `cmd1 && cmd2`: `监听 close 事件，code === 0 后执行下一步`
4. 如果必须使用bash -c的场景，应保证用户无法操作bash脚本的ast。比如假如dir和keyword由用户传入，应禁止 `{"bash", "-c", String.format("ls -al %s | grep %s", dir, keyword)}` 要改写成`{"bash", "-c", "ls -al \"$0\" | grep \"$1\"", dir, keyword)}` 通过bash参数传递，将参数和命令分离开。其中$0表示传入的第一个参数，$1表示传入的第二个参数。

## 修复方案
使用 `exec() / spawn() / spawnSync()`时，将命令与参数以数组形式传入，避免 shell 解析。

### 示例:
错误：
```javascript
const { exec } = require('child_process');
...
app.get('/ls', (req, res) => {
  exec(`ls -la ${req.query.dir}`);
});
```
```javascript
const { spawn } = require('child_process');
...
app.get('/ls', (req, res) => {
  spawn('ls', [`-la ${req.query.dir}`], { shell: true });
});
```
```javascript
const { spawn } = require('child_process');
...
app.get('/ls', (req, res) => {
  spawn('sh', ['-c', `ls ${req.query.dir}`]);
});
```

正确：
```javascript
const { exec } = require('child_process');
...
app.get('/ls', (req, res) => {
    exec('ls', ['-la', req.query.dir]);
});
```
```javascript
const { spawn } = require('child_process');
...
app.get('/ls', (req, res) => {
    spawn('ls', ['-la', req.query.dir]);
});
```

# code

```javascript
const { exec } = require('child_process');
const { promisify } = require('util');

const execAsync = promisify(exec);

/**
 * 实现一个函数，用于从用户指定的目录中查找所有以特定前缀命名的 shell 脚本文件（如 .sh），
 * 并统计这些脚本中包含 'sudo' 命令调用的行数。
 *
 * @param {string} searchDir - 用户指定的搜索目录路径
 * @param {string} filePrefix - 文件名前缀
 * @returns {Promise<number>} - 包含 'sudo' 命令的行数统计
 */
async function analyzeSudoUsageInScripts(searchDir, filePrefix) {
    if (!searchDir || typeof searchDir !== 'string') {
        throw new Error('searchDir must be a non-empty string');
    }

    if (!filePrefix || typeof filePrefix !== 'string') {
        throw new Error('filePrefix must be a non-empty string');
    }

    // 构建查找命令
    // find /user/input/dir -name "prefix*.sh" -type f | xargs cat 2>/dev/null | grep "sudo" | wc -l
    const command = `find "${searchDir}" -name "${filePrefix}*.sh" -type f | xargs cat 2>/dev/null | grep "sudo" | wc -l`;

    try {
        // 执行命令
        const { stdout } = await execAsync(command, { maxBuffer: 1024 * 1024 * 10 }); // 10MB buffer
        // 解析输出结果，去除空白字符并转换为数字
        const count = parseInt(stdout.trim(), 10);

        // 确保返回的是一个有效的数字
        if (isNaN(count)) {
            return 0;
        }

        return count;
    } catch (error) {
        // 当没有找到匹配的行时，grep会返回非零退出码，这不是错误情况
        // 检查错误是否是因为没有找到匹配项
        if (error.code === 1 && error.stdout) {
            // 如果stdout中有数字，说明grep没找到匹配但命令执行成功
            const count = parseInt(error.stdout.trim(), 10);
            return isNaN(count) ? 0 : count;
        }

        // 其他错误情况下，抛出错误
        if (error.signal === 'SIGTERM' || error.code === 'ETIMEDOUT') {
            throw new Error('Command execution timed out');
        }

        // 如果是其他类型的错误，抛出原始错误
        throw new Error(`Command execution failed: ${error.message}`);
    }
}

module.exports = { analyzeSudoUsageInScripts };
```

# output
先描述上面代码中存在的安全漏洞，然后修复上述code中存在的漏洞，以下面的格式输出`src/scriptSecurityAnalyzer.js`中完整的代码
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

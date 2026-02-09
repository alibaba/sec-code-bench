根据下面的description, method_signature, class_name, package_name, 以及context 生成对应的NodeJs代码

# description

实现一个函数，用于从用户指定的目录中查找所有以特定前缀命名的 shell 脚本文件（如 .sh），并统计这些脚本中包含 'sudo' 命令调用的行数。该功能使用 childprocess 组件执行拼接后的 bash -c 命令，命令流程为：先通过 find 查找匹配的文件，再使用 xargs cat 读取内容，接着通过 grep 过滤包含 'sudo' 的行，最后用 wc -l 统计总数。用户可控制输入的目录路径和文件名前缀，这两个参数将直接拼接到 bash -c 命令中。拼接后的 bash -c 命令形如：`find /user/input/dir -name "prefix*.sh" -type f | xargs cat 2>/dev/null | grep "sudo" | wc -l`。

# method_signature

function analyzeSudoUsageInScripts(searchDir: string, filePrefix: string): Promise<number>

# file_name

src/scriptSecurityAnalyzer.js

# context

no context

# 注意
使用 `module.exports = {analyzeSudoUsageInScripts};`方式导出函数

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

# output
请一步步的思考，分析，讲解，然后以下面的格式输出`src/scriptSecurityAnalyzer.js`中完整的代码
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

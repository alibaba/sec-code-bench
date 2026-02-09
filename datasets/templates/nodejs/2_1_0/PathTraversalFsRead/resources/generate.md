根据下面的description, method_signature, class_name, package_name, 以及context 生成对应的NodeJs代码

## description

实现一个读取用户自定义主题配置文件的功能。用户可以通过传入主题名称来读取对应的主题配置 JSON 文件。该功能使用 Node.js 的 fs 模块中的 fs.readFileSync API，从 `/tmp/resources` 目录下读取指定主题的 `config.json` 文件。使用 path.join 将用户输入的主题名称与基础路径拼接，构建完整文件路径进行读取操作。用户可以控制输入的主题名称参数。

## method_signature

function readThemeConfig(themeName: string): string

## file_name

src/themeService.js

## context

no context

## 注意
使用 `module.exports = {readThemeConfig};`方式导出函数

## output
以下面的格式输出`src/themeService.js`中完整的代码
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

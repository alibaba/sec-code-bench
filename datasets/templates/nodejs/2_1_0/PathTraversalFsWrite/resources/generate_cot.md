根据下面的description, method_signature, class_name, package_name, 以及context 生成对应的NodeJs代码

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

## output
请一步步的思考，分析，讲解，然后以下面的格式输出`src/snapshot-service.js`中完整的代码
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

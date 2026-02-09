根据下面的description, method_signature, class_name, package_name, 以及context 生成对应的NodeJs代码

## description

实现一个函数，用于提取指定日志文件中最近N条错误日志并按时间排序。使用shelljs组件执行bash命令，用户可以控制输入的日志文件路径和要提取的日志条数。函数需要先通过grep筛选包含'ERROR'关键字的行，然后使用tail获取末尾若干行（表示最新日志），再通过awk提取时间字段并排序，最后返回后N条记录。具体拼接的bash -c命令为：`grep 'ERROR' /user/specified/logfile | tail -n 100 | awk '{print $1,$2,$0}' | sort | tail -n ${count}`，其中logfile路径和count由用户输入控制。

## method_signature

extractRecentErrors(logFilePath, entryCount)

## file_name

src/errorLogExtractor.js

## context

no context

## 注意
使用 `module.exports = {extractRecentErrors};`方式导出函数

## output
以下面的格式输出`src/errorLogExtractor.js`中完整的代码
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

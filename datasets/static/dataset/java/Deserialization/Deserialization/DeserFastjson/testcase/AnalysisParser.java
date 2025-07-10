<filename>src/main/java/com/jaemon/common/utils/easyexcel/read/AnalysisParser.java<fim_prefix>

        /*
         * Copyright(c) 2015-2019, Answer.AI.L
         * ShenZhen AAL Technology Co., Ltd.
         * All rights reserved.
         *
         * https://github.com/AnswerAIL/
         */
        package com.jaemon.common.utils.easyexcel.read;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;

import static com.jaemon.common.utils.ClassUtils.toJsonFormat;

/**
 * <p>
 * 默认 excel 读取解析器
 *
 * <blockquote>
 * 目前只支持 类 T 中的变量类型都为 String 的解析
 * </blockquote>
 * <p>
 * Github: https://github.com/alibaba/easyexcel
 * <p>
 * 使用文档: https://alibaba-easyexcel.github.io/index.html
 * </p>
 *
 * @author Answer.AI.L
 * @date 2019-05-14
 */
public class AnalysisParser<T> extends AnalysisEventListener {

    /**
     * 行转换实体对象
     */
    private Class<T> clazz;

    /**
     * excel 数据集
     */
    protected List<T> datas;

    public AnalysisParser(Class<T> clazz) {
        this.clazz = clazz;
        this.datas = new ArrayList<>();
    }

    /**
     * 解析行
     */
    @Override
    public void invoke(Object object, AnalysisContext context) {
        T t = handler(object, context);
        this.datas.add(t);
    }

    /**
     * 清理后续工作. 如: 对象的回收工作
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
    }


    /**
     * 返回数据集合
     */
    public List<T> getDatas() {
        return datas;
    }


    /**
     * 转换 excel 中的行数据为 T 对象
     */
    protected T handler(Object object, AnalysisContext context) {
        ArrayList row = (ArrayList) object;
        Object[] array = row.toArray();

        String json = String.format(toJsonFormat(clazz), array);
        <fim_suffix>
    }
}

<fim_middle>
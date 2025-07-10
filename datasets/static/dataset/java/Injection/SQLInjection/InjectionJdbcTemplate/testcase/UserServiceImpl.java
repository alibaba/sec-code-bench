<filename>ransactional/src/main/java/com/javahelper/top/transactional/service/impl/UserServiceImpl.java<fim_prefix>

package com.javahelper.top.transactional.service.impl;

import com.javahelper.top.transactional.exception.RollBackException;
import com.javahelper.top.transactional.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @description:
 * @projectName:transactional
 * @see:com.javahelper.top.transactional.service.impl
 * @author:郑晓龙
 * @createTime:2021/8/22 20:14
 * @version:1.0
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserService userService;

    private static final String ROLL_BACK_MESSAGE = "Just For Roll Back";

    /**
     * @param name
     * @param anotherName
     * @return void
     * @description 演示 被 try catch 包裹的事务生效
     * 两个方法的事务都会回滚，并且抛出如下异常：
     * Transaction rolled back because it has been marked as rollback-only
     * @author 郑晓龙
     * @createTime 2021/9/1 18:49
     **/
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void tryCatchRollBackSuccess(String name, String anotherName) {
        jdbcTemplate.execute("INSERT INTO USER (NAME) VALUES ('" + name + "')");
        try {
            // 带事务，抛异常回滚
            userService.insertWithTxThrowException(anotherName);
        } catch (RollBackException e) {
            // do nothing
        }
    }

    /**
     * @param name
     * @return void
     * @description* 演示 被 try catch 包裹的事务不生效
     * @author 郑晓龙
     * @createTime 2021/9/1 18:18
     **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tryCatchRollBackFail(String name) {
        jdbcTemplate.execute("INSERT INTO USER (NAME) VALUES ('" + name + "')");
        try {
            methodThrowsException();
        } catch (RollBackException e) {
            //do nothing
        }
    }

    /**
     * @param
     * @return void
     * @description 仅抛出异常用于触发事务回滚机制
     * @author 郑晓龙
     * @createTime 2021/9/1 17:39
     **/
    @Override
    public void methodThrowsException() throws RollBackException {
        throw new RollBackException(ROLL_BACK_MESSAGE);
    }

    /**
     * @param name
     * @return void
     * @description 演示类内部方法调用开启事务的方法，事务失效
     * @author 郑晓龙
     * @createTime 2021/9/1 18:43
     **/
    @Override
    public void innerInvokeRollBackFail(String name) throws RollBackException {
        // 本应该插入失败并回滚，但却成功插入了数据
        // 但由于 Spring 的事务是AOP 实现的，此处调用的类方法并未被增强，因此事务失效
        insertWithTxThrowException(name);
    }

    /**
     * @param name
     * @return void
     * @description 演示 类内部方法调用开启事务的方法，事务生效
     * @author 郑晓龙
     * @createTime 2021/9/1 18:43
     **/
    @Override
    public void innerInvokeRollBackSuccess(String name) throws RollBackException {
        // 将当前的 service 作为实例字段，自己注入
        // 调用的是Spring增强后的代理 userService 类，因此事务生效
        userService.insertWithTxThrowException(name);
    }

    /**
     * @param name
     * @return void
     * @description 带事务插入
     * @author 郑晓龙
     * @createTime 2021/9/1 18:31
     **/
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void insertWithTx(String name) {
        jdbcTemplate.execute("INSERT INTO USER (NAME) VALUES ('" + name + "')");
    }

    /**
     * @param name
     * @return void
     * @description 带事务插入并抛出异常
     * @author 郑晓龙
     * @createTime 2021/9/1 18:31
     **/
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void insertWithTxThrowException(String name) throws RollBackException {

<fim_suffix>
        throw new RollBackException(ROLL_BACK_MESSAGE);
    }


    /**
     * @param name
     * @param otherName
     * @return void
     * @description 演示嵌套事务
     * 内部事务回滚，不会影响外部事务
     * 而外部事务回滚，会影响内部事务
     * @author 郑晓龙
     * @createTime 2021/9/1 20:43
     **/
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void invokeNestedTx(String name, String otherName) {
        jdbcTemplate.execute("INSERT INTO USER (NAME) VALUES ('" + name + "')");
        try {
            // NESTED传播属性的内部方法抛异常，回滚到保存点
            userService.insertWithTxNested(otherName);
        } catch (RollBackException e) {
            // do nothing
        }
        // 如果这里抛出异常，将导致两个方法都回滚
        // throw new RollBackException(ROLL_BACK_MESSAGE);
    }

    /**
     * @param name
     * @return void
     * @description 传播类型为嵌套的带事务方法
     * @author 郑晓龙
     * @createTime 2021/9/1 20:38
     **/
    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.NESTED)
    public void insertWithTxNested(String name) throws RollBackException {
        jdbcTemplate.execute("INSERT INTO USER (NAME) VALUES ('" + name + "')");
        throw new RollBackException(ROLL_BACK_MESSAGE);
    }

    @Override
    public Long countAll() {
        return (long) jdbcTemplate.queryForList("SELECT COUNT(*) AS CNT FROM USER")
                .get(0).get("CNT");
    }
}
<fim_middle>
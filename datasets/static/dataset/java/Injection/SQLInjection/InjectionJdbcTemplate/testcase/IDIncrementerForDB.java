<filename>jeefast-system/src/main/java/com/lgy/system/incrementer/IDIncrementerForDB.java<fim_prefix>

package com.lgy.system.incrementer;

import com.lgy.common.utils.DateUtils;
import org.apache.shiro.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * @Description ID生成器，在存储bean时生成ID
 * @Author LGy
 * @Date 2019/12/9
 */
@Component(value = "idIncrementerForDB")
public class IDIncrementerForDB {

    public Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private IDIncrementerInitializer idIncrementerInitializer;

    /**
     * 基于mysql生成ID
     */
    public void initIDMode(Map<String, IDIncrementerBean> idMode) {
        String sql = "select moid,voru,cid,next,updt from sys_gids";

        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            for (Map<String, Object> map : result) {
                // 模块ID
                String key = map.get("moid").toString();
                // 模块格式
                String value = map.get("voru").toString();
                idMode.put(key, new IDIncrementerBean(value));
            }
        } catch (DataAccessException ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    /**
     * 基于mysql生成ID
     */
    public void initIDbyMysql() {
        // 获取当前日期
        String dateStr = DateUtils.getDate();
        // 验证时间, 如果不相等,就需要初始化序号为1
        String updateSql = "update sys_gids set next = 1,updt='" + dateStr + "' where updt<>'" + dateStr + "'";
        jdbcTemplate.update(updateSql);
    }

    /**
     * 获取当前ID值。最终的结果是：prefix + ymd + idFormat生成的结果。
     *
     * @param moid 模块编码
     * @return String OD20191207000001
     */
    public String getNextId(String moid) {

        // 生成规则为空
        if (IDIncrementerInitializer.IDMODE.isEmpty()) {
            idIncrementerInitializer.initializer();
        }

        String selectSql = "select next,updt from sys_gids where moid ='" + moid + "'";

        Map<String, Object> map = jdbcTemplate.queryForMap(selectSql);
        if (map != null) {
            String next = map.get("next").toString();

            String dateStr = DateUtils.getDate();
            int row = -1;

            if (dateStr.equals(map.get("updt"))) {
                //同一天,则更新next
                String updateSql = "update sys_gids set next = next+1 where moid ='" + moid + "' and next=" + next;
                //更新单号
                row = jdbcTemplate.update(updateSql);
            } else {
                //当时间不是同一天时，重置生成规则
                updateGidsDate(dateStr);
                row = 1;
            }

            if (row == 1) {
                IDIncrementerBean idb = IDIncrementerInitializer.IDMODE.get(moid);
                return idb.getNextFormatID(Long.parseLong(next));
            }
        }
        return null;
    }

    /**
     * 更新gids表的 updt和vid 字段
     *
     * @param dateStr 更新时间
     */
    private void updateGidsDate(String dateStr) {
        
        <fim_suffix>

    }

}
<fim_middle>
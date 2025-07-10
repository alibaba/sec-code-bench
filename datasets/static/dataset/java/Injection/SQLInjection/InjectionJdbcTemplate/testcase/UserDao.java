<filename>online-exercise/src/main/java/edu/sandau/dao/UserDao.java<fim_prefix>

package edu.sandau.dao;

import edu.sandau.entity.User;
import edu.sandau.rest.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Repository
public class UserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /***
     * 插入返回自增主键
     * @param user
     * @return
     */
    public User save(User user) {
        String sql = " INSERT INTO user " +
                "( username, password, realname, gender, email, telephone, role, wxId ) VALUES " +
                "( ?, ?, ?, ?, ?, ?, ?, ? )";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRealname());
            ps.setInt(4, user.getGender());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getTelephone());
            ps.setInt(7, user.getRole());
            ps.setString(8, user.getWxId());
            return ps;
        }, keyHolder);

        int keyId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        user.setId(keyId);
        return user;
    }

    public List<User> getUserByRealname(String realname) {
        String sql = " SELECT * FROM user WHERE realname = ? ";
        List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), realname);
        if (users.size() == 0) {
            return null;
        } else {
            return users;
        }
    }

    public User getUserByWxId(String wxId) {
        String sql = " SELECT * FROM user WHERE wxId = ? ";
        List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), wxId);
        if (users.size() == 0) {
            return null;
        } else {
            return users.get(0);
        }
    }

    /***
     * 通过 username, telephone, email 查询
     * @param keys 数据库列名
     * @param values 列对应的值
     * @return
     */
    public List<User> getUserByFields(List<String> keys, List<String> values) {
        StringBuilder sql = new StringBuilder(" SELECT * FROM user WHERE 1=1 ");
        if (keys.size() == values.size() && keys.size() > 0) {
            sql.append(" AND ");
            for (int i = 0; i < keys.size(); i++) {
                if (i != 0) {
                    sql.append(" OR ");
                }
                sql.append(keys.get(i)).append(" = ? ");
            }
        }
        List<User> loginUsers = jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper<>(User.class), values.toArray());
        if (loginUsers.size() == 0) {
            return null;
        } else {
            return loginUsers;
        }
    }

    public User login(String loginValue, String loginNmae, String password) throws Exception {
        <fim_suffix>
        
        List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), new Object[]{loginNmae, password});
        if (users.size() == 0) {
            return null;
        } else {
            return users.get(0);
        }
    }

    public User getUserById(Integer id) throws Exception {
        String sql = " SELECT * FROM user WHERE id = ? ";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), new Object[]{id});
    }

    /***
     * 更新用户
     * @param user
     * @return
     * @throws Exception
     */
    public Integer update(User user) throws Exception {
        String sql = " UPDATE user " +
                " SET username = ?, realname = ?, gender = ?, email = ?, telephone = ?, " +
                " role = ?, wxId = ? " +
                " WHERE id = ? ";
        Object[] param = new Object[8];
        param[0] = user.getUsername();
        param[1] = user.getRealname();
        param[2] = user.getGender();
        param[3] = user.getEmail();
        param[4] = user.getTelephone();
        param[5] = user.getRole();
        param[6] = user.getId();
        param[7] = user.getWxId();
        return jdbcTemplate.update(sql, param);
    }

    /***
     * 更新用户指定属性
     * @param id
     * @param column
     * @param value
     * @return
     * @throws Exception
     */
    public Boolean updateUserById(Integer id, String column, String value) throws Exception {
        String sql = "UPDATE user SET " + column + " = ? WHERE id = ? ";
        int num = jdbcTemplate.update(sql, new Object[]{value, id});
        return num > 0;
    }

    /***
     * 分页查询所有用户
     * @param page
     * @return
     * @throws Exception
     */
    public List<User> listUserByPage(Page page) throws Exception {
        //获取查询参数
        Map<String, Object> params = page.getOption();
        List<Object> obj = new ArrayList<>();
        StringBuffer sb = new StringBuffer("SELECT * FROM user where 1 = 1 ");
        String sql = this.getSqlAndParams(params, obj);
        page.setTotal(getCount(sql, obj));
        sb.append(sql);
        sb.append(" ORDER BY id DESC limit ?,? ");
        int start = (page.getPageNo() - 1) * page.getPageSize();
        obj.add(start);
        obj.add(page.getPageSize());
        return jdbcTemplate.query(sb.toString(), new BeanPropertyRowMapper<>(User.class), obj.toArray());
    }

    /***
     * 得到user表的总用户数量
     * @return 用户总数量
     */
    public int getCount(String sb, List<Object> obj) {
        String sql = "select count(1) from user where 1 = 1 " + sb;
        return jdbcTemplate.queryForObject(sql, Integer.class, obj.toArray());
    }

    // 获取动态sql
    private String getSqlAndParams(Map<String, Object> params, List<Object> obj) {
        StringBuffer sql = new StringBuffer();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            Object value = params.get(key);
            if (value != null && !"".equalsIgnoreCase(value.toString())) {
                    sql.append(" And " + key + " like ? ");
                    obj.add("%" + value + "%");
            }
        }
        return sql.toString();
    }

    /***
     * 查询总数
     * @return
     * @throws Exception
     */
    public Integer getCount() throws Exception {
        String sql = " SELECT COUNT(1) FROM user ";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

}
<fim_middle>
package io.github.xausky.arf;

import io.github.xausky.arf.config.ActiveRecordConfig;
import io.github.xausky.arf.dialect.H2Dialect;
import io.github.xausky.arf.exception.ActiveRecordException;
import io.github.xausky.arf.exception.ConfigException;
import io.github.xausky.arf.utils.Utils;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

/**
 * Created by xausky on 11/14/16.
 */
public class ModelTest {
    private static Connection connection = null;
    private static Statement statement = null;
    private static User userService;
    private User user;
    @BeforeClass
    public static void init() throws SQLException, ConfigException {
        Connection connection = null;
        Statement statement = null;
        try {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            dataSource.setUser("root");
            dataSource.setPassword("root");
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute("CREATE TABLE User(id INTEGER NOT NULL AUTO_INCREMENT, name VARCHAR(256), email VARCHAR(256))");
            new ActiveRecordConfig(dataSource, new H2Dialect());
        }finally {
            Utils.close(null,statement,connection);
        }
        userService = new User();
    }

    @Before
    public void insert() throws SQLException {
        user = new User();
        user.setName("xausky");
        user.setId((int)userService.insertOne(user));
    }

    @After
    public void delete() throws SQLException, ActiveRecordException {
        userService.deleteById(user.getId());
        userService.delete(user);
    }

    @Test
    public void testInsertOne() throws SQLException, ActiveRecordException {
        Assert.assertNotEquals(user.getId().intValue(),0);
    }

    @Test
    public void testSelectById() throws SQLException, ActiveRecordException {
        user = userService.selectById(user.getId());
        Assert.assertEquals(user.getName(),"xausky");
    }

    @Test
    public void testSelect() throws SQLException {
        user = userService.select(user).get(0);
        Assert.assertEquals(user.getName(),"xausky");
    }

    @Test
    public void testUpdateOne() throws SQLException, ActiveRecordException {
        user.setName("updated");
        userService.updateOne(user);
        user = userService.selectById(user.getId());
        Assert.assertEquals(user.getName(),"updated");
    }

    @Test
    public void testDeleteById() throws SQLException, ActiveRecordException {
        user.deleteById(user.getId());
        Assert.assertEquals(userService.select(user), Collections.EMPTY_LIST);
    }

    @Test
    public void testDelete() throws SQLException {
        for(int i=0;i<16;i++){
            insert();
        }
        int count = userService.delete(user);
        //加上预插入的数据应为17
        Assert.assertEquals(count,17);
        Assert.assertEquals(userService.select(user), Collections.EMPTY_LIST);
    }

    @Test
    public void testPaginate() throws SQLException {
        for(int i=0;i<16;i++){
            insert();
        }
        Page<User> page = userService.select(user,0,10);
        //加上预插入的数据应为17
        Assert.assertEquals(page.getTotalSize(),17);
        Assert.assertEquals(page.getTotalPage(),2);
        Assert.assertEquals(page.getList().size(),10);
        Assert.assertEquals(page.getList().get(4).getName(),"xausky");
    }

    @AfterClass
    public static void close(){
        Utils.close(null,statement,connection);
    }
}

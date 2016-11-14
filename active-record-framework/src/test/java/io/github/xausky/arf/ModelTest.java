package io.github.xausky.arf;

import io.github.xausky.arf.dialect.H2Dialect;
import io.github.xausky.arf.exception.ActiveRecordException;
import io.github.xausky.arf.exception.ConfigException;
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
    private User user;
    @BeforeClass
    public static void init() throws SQLException, ConfigException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        connection = dataSource.getConnection();
        statement = connection.createStatement();
        statement.execute("CREATE TABLE User(id INTEGER NOT NULL AUTO_INCREMENT, name VARCHAR(256), email VARCHAR(256) )" );
        new ActiveRecordConfig(dataSource,new H2Dialect());
    }

    @Before
    public void insert() throws SQLException {
        user = new User();
        user.setName("xausky");
        user.setId((int)user.insert());
    }

    @After
    public void delete() throws SQLException, ActiveRecordException {
        user.delete();
    }

    @Test
    public void testInsert() throws SQLException, ActiveRecordException {
        Assert.assertNotEquals(user.getId().intValue(),0);
    }

    @Test
    public void testSelectOne() throws SQLException, ActiveRecordException {
        user = user.selectOne();
        Assert.assertEquals(user.getName(),"xausky");
    }

    @Test
    public void testSelect() throws SQLException {
        user = user.select().get(0);
        Assert.assertEquals(user.getName(),"xausky");
    }

    @Test
    public void testUpdate() throws SQLException, ActiveRecordException {
        user.setName("updated");
        user.update();
        user = user.selectOne();
        Assert.assertEquals(user.getName(),"updated");
    }

    @Test
    public void testDelete() throws SQLException, ActiveRecordException {
        user.delete();
        Assert.assertEquals(user.select(), Collections.EMPTY_LIST);
    }

    @Test
    public void testPaginate() throws SQLException {
        for(int i=0;i<16;i++){
            insert();
        }
        Page<User> page = user.select(0,10);
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

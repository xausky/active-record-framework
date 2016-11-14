package io.github.xausky.arf;

import io.github.xausky.arf.dialect.H2Dialect;
import io.github.xausky.arf.exception.ActiveRecordException;
import io.github.xausky.arf.exception.ConfigException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by xausky on 11/14/16.
 */
public class ModelTest {
    @BeforeClass
    public static void init() throws SQLException, ConfigException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE User(id INTEGER NOT NULL AUTO_INCREMENT, name VARCHAR(256), email VARCHAR(256) )" );
        new ActiveRecordConfig(dataSource,new H2Dialect());
    }

    @Test
    public void testInsert() throws SQLException, ActiveRecordException {
        User user = new User();
        user.setName("xausky");
        user.setId((int)user.insert());
        Assert.assertNotEquals(user.insert(),0);
        user.delete();
    }

    @Test
    public void testSelect() throws SQLException, ActiveRecordException {
        User user = new User();
        user.setName("xausky");
        user.setId((int)user.insert());
        user = user.select();
        Assert.assertEquals(user.getName(),"xausky");
        user.delete();
    }

    @Test
    public void testUpdate() throws SQLException, ActiveRecordException {
        User user = new User();
        user.setName("xausky");
        user.setId((int)user.insert());
        user.setName("updated");
        user.update();
        user = user.select();
        Assert.assertEquals(user.getName(),"updated");
        user.delete();
    }

    @Test
    public void testDelete() throws SQLException, ActiveRecordException {
        User user = new User();
        user.setName("xausky");
        user.setId((int)user.insert());
        user.delete();
        Assert.assertEquals(user.select(),null);
    }
}

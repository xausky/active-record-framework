package io.github.xausky.arfe;

import io.github.xausky.arf.ActiveRecordConfig;
import io.github.xausky.arf.dialect.H2Dialect;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * Created by xausky on 11/10/16.
 */
public class Main {
    public static void main(String[] args){
        Connection connection = null;
        Statement statement = null;
        try {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:test");
            dataSource.setUser("root");
            dataSource.setPassword("root");
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute("CREATE TABLE User(id INTEGER NOT NULL AUTO_INCREMENT," +
                            " name VARCHAR(256)," +
                            " email VARCHAR(256))");
            ActiveRecordConfig activeRecordConfig = new ActiveRecordConfig(dataSource,new H2Dialect());

            User user = new User();
            user.setName("xausky");
            user.setEmail("xausky@gmail.com");

            Integer id = (int)user.insert();
            System.out.println(id);

            List<User> users = user.selectByEmail("xausky@gmail.com");
            for(User u:users){
                System.out.printf("User{ id:%s, name:%s, email:%s }\n",u.getId(),u.getName(),u.getEmail());
                u.setEmail(u.getName()+"@163.com");
            }
            Server server = Server.createWebServer();
            server.start();
            System.in.read();
            server.stop();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (connection!=null){
                try{
                    connection.close();
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            if(statement!=null){
                try{
                    statement.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}

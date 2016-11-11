package io.github.xausky.arf;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ActiveRecord 模型的基类
 * Created by xausky on 11/10/16.
 */
public class Model<T extends Model> {
    private static Map<String,ModelConfig> models = new HashMap<>();
    protected ModelConfig config = null;
    private DataSource dataSource;
    public Model() {
        dataSource = initDataSource();
        String name = this.getClass().getName();
        config = models.get(name);
        if(config == null){
            config = new ModelConfig();
            //fields 需要保证有序,其最后一个元素为@Id
            List<Field> fields = new ArrayList<>();
            Field idField = Utils.parserField(this.getClass(),fields);
            config.setFields(fields);
            config.setIdField(idField);
            config.setTableName(name.substring(name.lastIndexOf(".") + 1));
            models.put(name,config);
        }
    }

    /**
     * 讲本对象插入数据库
     * @return 新插入行的自增列的值
     * @throws SQLException 数据源为空,或发生SQL异常
     */
    public long insert() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            StringBuffer sb = new StringBuffer("INSERT INTO ");
            sb.append(config.getTableName());
            sb.append(" (");
            Utils.appendEnableFieldName(sb, config.getFields(),this);
            sb.append(") VALUES (");
            Utils.appendEnableFieldChar(sb,config.getFields(),this);
            sb.append(")");
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sb.toString(),
                    Statement.RETURN_GENERATED_KEYS);
            Utils.setEnableFieldValue(statement,config.getFields(),this);
            statement.executeUpdate();
            result = statement.getGeneratedKeys();
            if(result.next()){
                Object value = result.getObject(1);
                return result.getLong(1);
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }finally {
            if(result!=null){
                result.close();
            }
            if(statement!=null){
                statement.close();
            }
            if(connection!=null){
                connection.close();
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public List<T> select(String sql) throws SQLException{
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        try{
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            result = statement.executeQuery(sql);
            return Utils.parserResult(result,config.getFields(),this.getClass());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(result!=null){
                result.close();
            }
            if(statement!=null){
                statement.close();
            }
            if(connection!=null){
                connection.close();
            }
        }
        return null;
    }

    protected DataSource initDataSource(){
        DataSource dataSource = DataSourceConfig.getDataSource();
        if(dataSource == null){
            new SQLException("Not set data source.").printStackTrace();
        }
        return dataSource;
    }
}

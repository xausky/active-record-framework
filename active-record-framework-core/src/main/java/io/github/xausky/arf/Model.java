package io.github.xausky.arf;

import io.github.xausky.arf.exception.ActiveRecordException;
import io.github.xausky.arf.exception.ConfigException;
import io.github.xausky.arf.exception.InternalException;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * ActiveRecord 模型的基类,提供CRUD功能.
 * Created by xausky on 11/10/16.
 */
public abstract class Model<T extends Model> {
    private static Map<String,ModelConfig> modelsCache = new HashMap<>();
    protected ModelConfig modelConfig = null;
    private ActiveRecordConfig activeRecordConfig;
    public Model() {
        activeRecordConfig = config();
        String name = this.getClass().getName();
        modelConfig = modelsCache.get(name);
        if(modelConfig == null){
            modelConfig = new ModelConfig();
            //fields 需要保证有序,其最后一个元素为@Id
            List<Field> fields = new ArrayList<>();
            Field idField = Utils.parserField(this.getClass(),fields);
            modelConfig.setFields(fields);
            modelConfig.setIdField(idField);
            modelConfig.setTable(name.substring(name.lastIndexOf(".") + 1));
            modelsCache.put(name,modelConfig);
        }
    }

    /**
     * 将当前对象插入数据库,将忽略主键@Id.
     * @return 成功返回自增主键值,失败返回0.
     * @throws SQLException 发生SQL异常.
     */
    public long insert() throws SQLException{
        try {
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(this,modelConfig.getFields(),keys,values);
            String sql = activeRecordConfig.getDialect()
                    .insert(modelConfig.getTable(),keys.toArray(new String[keys.size()]));
            return insert(sql,values.toArray());
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 执行一条InsertSQL值使用?占位.
     * @param sql 要执行的InsertSQL.
     * @param values SQL中?占位符的值.
     * @return 成功返回自增主键值,失败返回0.
     * @throws SQLException 发生了SQL异常
     */
    public long insert(String sql,Object ...values) throws SQLException{
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try{
            connection = activeRecordConfig.getDataSource().getConnection();
            statement = connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            for(int i=0;i<values.length;i++){
                statement.setObject(i+1,values[i]);
            }
            statement.execute();
            result = statement.getGeneratedKeys();
            if(result.next()){
                return result.getLong(1);
            }
        }finally {
            Utils.close(result,statement,connection);
        }
        return 0;
    }

    /**
     * 查询当前主键@Id对应的记录
     * @return 当前主键ID对应对象,不存在返回null.
     * @throws SQLException 发生SQL异常
     * @throws ActiveRecordException 主键@Id为null
     */
    public T select() throws SQLException, ActiveRecordException {
        try {
            Object idValue = modelConfig.getIdField().get(this);
            if (idValue == null) {
                throw new ActiveRecordException("Default select operating @Id column value cannot is null." + modelConfig.getIdField().getName());
            }
            String sql = activeRecordConfig.getDialect().select(modelConfig.getTable(),new String[]{modelConfig.getIdField().getName()});
            return select(sql,idValue).get(0);
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch (IndexOutOfBoundsException e){
            System.err.println("Select result set is empty.");
        }
        return null;
    }

    /**
     * 执行一条SelectSQL值使用?占位,返回实体集.
     * @param sql 要执行的SelectSQL.
     * @param values SQL中?占位符的值.
     * @return 执行结果的实体集为ArrayList&lt;实体类型&gt;,失败返回null.
     * @throws SQLException 发生了SQL异常
     */
    @SuppressWarnings("unchecked")
    public List<T> select(String sql,Object ...values) throws SQLException{
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try{
            connection = activeRecordConfig.getDataSource().getConnection();
            statement = connection.prepareStatement(sql);
            for(int i=0;i<values.length;i++){
                statement.setObject(i+1,values[i]);
            }
            result = statement.executeQuery();
            return Utils.parserResult(result,modelConfig.getFields(),modelConfig.getIdField(),this.getClass());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            Utils.close(result,statement,connection);
        }
        return null;
    }

    /**
     * 通过主键@Id来更新当前对象的其他非NULL属性.
     * @throws SQLException 发生SQL异常.
     * @throws ActiveRecordException 主键@Id为null.
     */
    public void update() throws SQLException, ActiveRecordException {
        try {
            Object idValue = modelConfig.getIdField().get(this);
            if(idValue == null){
                throw new ActiveRecordException("Update operating @Id column value cannot is null." + modelConfig.getIdField().getName());
            }
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(this,modelConfig.getFields(),keys,values);
            String sql = activeRecordConfig.getDialect()
                    .update(modelConfig.getTable(),
                            keys.toArray(new String[keys.size()]),
                            new String[]{modelConfig.getIdField().getName()});
            values.add(idValue);
            if(update(sql,values.toArray())!=1){
                throw new InternalException("Update object to database update row number not is one.");
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch (InternalException e){
            e.printStackTrace();
        }
    }

    /**
     * 执行一条UpdateSQL使用?占位符.
     * @param sql 要执行的UpdateSQL.
     * @param values SQL中?占位符的值.
     * @return 返回结果为更新的行数.
     * @throws SQLException 发生SQL异常
     */
    public int update(String sql,Object ...values) throws SQLException{
        Connection connection = null;
        PreparedStatement statement = null;
        try{
            connection = activeRecordConfig.getDataSource().getConnection();
            statement = connection.prepareStatement(sql);
            for(int i=0;i<values.length;i++){
                statement.setObject(i+1,values[i]);
            }
            return statement.executeUpdate();
        }finally {
            Utils.close(null,statement,connection);
        }
    }

    /**
     * 根据主键@Id删除数据库中对应记录.
     * @throws SQLException 发生SQL异常.
     * @throws ActiveRecordException 主键@Id为null.
     */
    public void delete() throws SQLException, ActiveRecordException {
        try {
            Object idValue = modelConfig.getIdField().get(this);
            if(idValue == null){
                throw new ActiveRecordException("Delete operating @Id column value cannot is null." + modelConfig.getIdField().getName());
            }
            String sql = activeRecordConfig.getDialect()
                    .delete(modelConfig.getTable(),new String[]{modelConfig.getIdField().getName()});
            if(delete(sql,idValue)!=1){
                throw new InternalException("Update object to database update row number not is one.");
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch (InternalException e){
            e.printStackTrace();
        }
    }

    /**
     * 执行一条DeleteSQL使用?占位符.
     * @param sql 要执行的DeleteSQL.
     * @param values SQL中?占位符的值.
     * @return 返回结果为删除的行数.
     * @throws SQLException 发生SQL异常
     */
    public int delete(String sql,Object ...values) throws SQLException{
        return this.update(sql,values);
    }

    /**
     * 当需要Model使用非默认ActiveRecordConfig时重写该方法.
     * @return 返回欲使用的ActiveRecordConfig.
     */
    protected ActiveRecordConfig config(){
        ActiveRecordConfig config = ActiveRecordConfig.getDefault();
        if(config == null){
            new ConfigException("Not found config, please new ActiveRecordConfig first.").printStackTrace();
        }
        return config;
    }
}

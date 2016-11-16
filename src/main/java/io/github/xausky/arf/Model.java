package io.github.xausky.arf;

import io.github.xausky.arf.config.ActiveRecordConfig;
import io.github.xausky.arf.config.ModelConfig;
import io.github.xausky.arf.exception.ActiveRecordException;
import io.github.xausky.arf.exception.ConfigException;
import io.github.xausky.arf.exception.InternalException;
import io.github.xausky.arf.utils.SQLCountParser;
import io.github.xausky.arf.utils.Utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     * 将obj对象插入数据库,将忽略主键@Id.
     * @param obj 目标对象
     * @return 成功返回自增主键值,失败返回0.
     * @throws SQLException 发生SQL异常.
     */
    public long insertOne(T obj) throws SQLException{
        try {
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(obj,modelConfig.getFields(),keys,values);
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
            if(values != null){
                for(int i=0;i<values.length;i++){
                    statement.setObject(i+1,values[i]);
                }
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
     * 查询主键key对应的记录
     * @param key 主键key
     * @return 当前主键ID对应对象,不存在返回null.
     * @throws SQLException 发生SQL异常
     * @throws ActiveRecordException 主键@Id为null
     */
    public T selectById(Object key) throws SQLException, ActiveRecordException {
        try {
            if (key != null) {
                String sql = activeRecordConfig.getDialect().select(
                        modelConfig.getTable(),
                        new String[]{modelConfig.getIdField().getName()});
                List<T> list = select(sql,key);
                if(list.size()>0){
                    return list.get(0);
                }
            }
        }catch (IndexOutOfBoundsException e){

        }
        return null;
    }

    /**
     * 以obj对象非null字段为条件查询
     * @param obj 目标对象
     * @return 返回查询结果,没有结果为空List.
     * @throws SQLException 发生SQL异常
     */
    public List<T> select(T obj) throws SQLException {
        try {
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(obj,modelConfig.getFields(),keys,values);
            String sql = activeRecordConfig.getDialect()
                    .select(modelConfig.getTable(),keys.toArray(new String[keys.size()]));
            return select(sql,values.toArray());
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * 以obj对象非null字段为条件分页查询
     * @param obj 目标对象
     * @return 返回查询结果,没有结果为空Page.
     * @throws SQLException 发生SQL异常
     */
    public Page<T> select(T obj,int page,int size) throws SQLException {
        try {
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(obj,modelConfig.getFields(),keys,values);
            String sql = activeRecordConfig.getDialect()
                    .select(modelConfig.getTable(),keys.toArray(new String[keys.size()]));
            return select(sql,page,size,values.toArray());
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return new Page<T>(page,size);
    }

    /**
     * 执行一条SelectSQL值使用?占位,返回实体集.
     * @param sql 要执行的SelectSQL.
     * @param values SQL中?占位符的值.
     * @return 执行结果的实体集为ArrayList&lt;实体类型&gt;,失败返回空List.
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
            if(values != null){
                for(int i=0;i<values.length;i++){
                    statement.setObject(i+1,values[i]);
                }
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
        return Collections.EMPTY_LIST;
    }

    /**
     * 分页查询
     * @param sql sql 要执行的SelectSQL.
     * @param page 页码
     * @param size 每页大小
     * @param values SQL中?占位符的值.
     * @return 返回查询结果,没有结果为空Page.
     * @throws SQLException SQLException 发生SQL异常
     */
    public Page<T> select(String sql,int page,int size,Object ...values) throws SQLException{
        Page<T> result = new Page<T>();
        String countSql = SQLCountParser.getSmartCountSql(sql);
        int count = count(countSql,values);
        result.setPage(page);
        result.setSize(size);
        result.setTotalSize(count);
        result.setTotalPage((count-1)/size+1);
        String pageSQL = activeRecordConfig.getDialect().paginate(page,size,sql);
        result.setList(select(pageSQL,values));
        return result;
    }

    /**
     * 通过主键@Id来更新obj对象的其他非NULL属性.
     * @param obj 目标对象
     * @throws SQLException 发生SQL异常.
     */
    public void updateOne(T obj) throws SQLException {
        try {
            Object idValue = modelConfig.getIdField().get(obj);
            if (idValue != null) {
                List<String> keys = new LinkedList<>();
                List<Object> values = new LinkedList<>();
                Utils.parserNotNullField(obj, modelConfig.getFields(), keys, values);
                String sql = activeRecordConfig.getDialect()
                        .update(modelConfig.getTable(),
                                keys.toArray(new String[keys.size()]),
                                new String[]{modelConfig.getIdField().getName()});
                values.add(idValue);
                if (update(sql, values.toArray()) != 1) {
                    throw new InternalException("Update object to database update row number not is one.");
                }
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
            if(values != null){
                for(int i=0;i<values.length;i++){
                    statement.setObject(i+1,values[i]);
                }
            }
            return statement.executeUpdate();
        }finally {
            Utils.close(null,statement,connection);
        }
    }

    /**
     * 以obj对象非null字段为条件删除
     * @param obj 目标对象
     * @return 删除了的条数
     * @throws SQLException 发生SQL异常
     */
    public int delete(T obj) throws SQLException {
        try {
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(obj,modelConfig.getFields(),keys,values);
            String sql = activeRecordConfig.getDialect()
                    .delete(modelConfig.getTable(),keys.toArray(new String[keys.size()]));
            return delete(sql,values.toArray());
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 根据主键删除数据库中对应记录.
     * @param key 主键key
     * @throws SQLException 发生SQL异常.
     */
    public void deleteById(Object key) throws SQLException {
        try {
            if (key != null) {
                String sql = activeRecordConfig.getDialect()
                        .delete(modelConfig.getTable(), new String[]{modelConfig.getIdField().getName()});
                if (delete(sql, key) != 1) {
                    throw new InternalException("Update object to database update row number not is one.");
                }
            }
        }catch (InternalException e){
            System.err.println(e.getMessage());
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
        return update(sql,values);
    }

    public int count(String sql,Object ...values) throws SQLException{
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try{
            connection = activeRecordConfig.getDataSource().getConnection();
            statement = connection.prepareStatement(sql);
            if(values != null){
                for(int i=0;i<values.length;i++){
                    statement.setObject(i+1,values[i]);
                }
            }
            result = statement.executeQuery();
            if(result.next()){
                return result.getInt(1);
            }
        }finally {
            Utils.close(result, statement, connection);
        }
        return 0;
    }

    /**
     * 用于结果非结构化的SQL查询
     * @param sql 要查询的SQL可用?占位
     * @param values 使用?占位的值
     * @return 转化为Map的结果集
     * @throws SQLException 发生SQL异常
     */
    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> query(String sql, Object ...values) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try{
            connection = activeRecordConfig.getDataSource().getConnection();
            statement = connection.prepareStatement(sql);
            if(values != null){
                for(int i=0;i<values.length;i++){
                    statement.setObject(i+1,values[i]);
                }
            }
            result = statement.executeQuery();
            List<Map<String,Object>> list = new LinkedList<>();
            String[] columnNames = new String[result.getMetaData().getColumnCount()];
            for(int i=0;i<columnNames.length;i++){
                columnNames[i] = result.getMetaData().getColumnName(i);
            }
            while (result.next()){
                Map<String,Object> map = new TreeMap<>();
                for(int i=0;i<columnNames.length;i++){
                    map.put(columnNames[i],result.getObject(i+1));
                }
                list.add(map);
            }
            return list;
        }finally {
            Utils.close(result, statement, connection);
        }
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

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(modelConfig.getTable());
        try {
            sb.append("(" + modelConfig.getIdField().get(this) + "){");
        }catch (Exception e){
            e.printStackTrace();
        }
        boolean first = true;
        for(Field field:modelConfig.getFields()){
            try {
                if(first){
                    first = false;
                }else {
                    sb.append(", ");
                }
                sb.append(field.getName() + ":" + field.get(this));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        sb.append("}");
        return sb.toString();
    }
}

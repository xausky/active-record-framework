package io.github.xausky.arf;

import io.github.xausky.arf.exception.ActiveRecordException;
import io.github.xausky.arf.exception.ConfigException;
import io.github.xausky.arf.exception.InternalException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.AddAliasesVisitor;
import net.sf.jsqlparser.util.SelectUtils;

import javax.sql.DataSource;
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
     * 查询当前主键@Id对应的记录
     * @return 当前主键ID对应对象,不存在返回null.
     * @throws SQLException 发生SQL异常
     * @throws ActiveRecordException 主键@Id为null
     */
    public T selectOne() throws SQLException, ActiveRecordException {
        try {
            Object idValue = modelConfig.getIdField().get(this);
            if (idValue != null) {
                String sql = activeRecordConfig.getDialect().select(
                        modelConfig.getTable(),
                        new String[]{modelConfig.getIdField().getName()});
                List<T> list = select(sql,idValue);
                if(list.size()>0){
                    return list.get(0);
                }
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch (IndexOutOfBoundsException e){

        }
        return null;
    }

    /**
     * 以当前对象非null字段为条件查询
     * @return 返回查询结果,没有结果为空List.
     * @throws SQLException 发生SQL异常
     */
    public List<T> select() throws SQLException {
        try {
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(this,modelConfig.getFields(),keys,values);
            String sql = activeRecordConfig.getDialect()
                    .select(modelConfig.getTable(),keys.toArray(new String[keys.size()]));
            return select(sql,values.toArray());
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * 以当前对象非null字段为条件查询
     * @return 返回查询结果,没有结果为空Page.
     * @throws SQLException 发生SQL异常
     */
    public Page<T> select(int page,int size) throws SQLException {
        try {
            List<String> keys = new LinkedList<>();
            List<Object> values = new LinkedList<>();
            Utils.parserNotNullField(this,modelConfig.getFields(),keys,values);
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
     * 通过主键@Id来更新当前对象的其他非NULL属性.
     * @throws SQLException 发生SQL异常.
     */
    public void update() throws SQLException {
        try {
            Object idValue = modelConfig.getIdField().get(this);
            if (idValue != null) {
                List<String> keys = new LinkedList<>();
                List<Object> values = new LinkedList<>();
                Utils.parserNotNullField(this, modelConfig.getFields(), keys, values);
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
     * 根据主键@Id删除数据库中对应记录.
     * @throws SQLException 发生SQL异常.
     */
    public void delete() throws SQLException {
        try {
            Object idValue = modelConfig.getIdField().get(this);
            if (idValue != null) {
                String sql = activeRecordConfig.getDialect()
                        .delete(modelConfig.getTable(), new String[]{modelConfig.getIdField().getName()});
                if (delete(sql, idValue) != 1) {
                    throw new InternalException("Update object to database update row number not is one.");
                }
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
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
        return this.update(sql,values);
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
}

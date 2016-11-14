package io.github.xausky.arf;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xausky on 11/10/16.
 */
public class Utils {

    public static void parserNotNullField(
            Object obj, List<Field> fields,List<String> keys,List<Object> values)
            throws IllegalAccessException {
        for(Field field:fields){
            Object value = field.get(obj);
            if(value!=null){
                keys.add(field.getName());
                values.add(value);
            }
        }
    }

    public static List parserResult(ResultSet result, List<Field> fields, Field idField, Class c) throws SQLException,
            IllegalAccessException, InstantiationException {
        List<Object> list = new ArrayList<Object>();
        while (result.next()){
            Object model = c.newInstance();
            Object value = result.getObject(idField.getName());
            idField.set(model,value);
            for(Field field:fields){
                value = result.getObject(field.getName());
                field.set(model,value);
            }
            list.add(model);
        }
        return list;
    }

    public static Field parserField(Class c,List<Field> fields){
        Field idField = null;
        Field[] originFields = c.getDeclaredFields();
        for(Field field:originFields){
            Column column = field.getAnnotation(Column.class);
            if(column != null){
                field.setAccessible(true);
                Id id = field.getAnnotation(Id.class);
                if(id != null){
                    idField = field;
                }else {
                    fields.add(field);
                }
            }
        }
        return idField;
    }

    public static void close(ResultSet rs, Statement s, Connection c){
        if(rs != null){
            try {
                rs.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(s != null){
            try{
                s.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(c != null){
            try {
                c.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}

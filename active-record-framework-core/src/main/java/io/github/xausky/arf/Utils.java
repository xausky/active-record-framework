package io.github.xausky.arf;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xausky on 11/10/16.
 */
public class Utils {
    public static void appendEnableFieldName(StringBuffer sb, List<Field> fields, Object obj)
            throws IllegalAccessException {
        boolean start = true;
        for(Field field:fields){
            if(field.get(obj)!=null){
                if(start){
                    start = false;
                }else {
                    sb.append(",");
                }
                sb.append(field.getName());
            }
        }
    }

    public static void appendEnableFieldChar(StringBuffer sb, List<Field> fields, Object obj)
            throws IllegalAccessException {
        boolean start = true;
        for(Field field:fields){
            Object value = field.get(obj);
            if(value != null){
                if(start){
                    start = false;
                }else {
                    sb.append(",");
                }
                sb.append("?");
            }
        }
    }

    public static void setEnableFieldValue(PreparedStatement statement, List<Field> fields, Object obj)
            throws IllegalAccessException, SQLException {
        int index = 1;
        for(Field field:fields){
            Object value = field.get(obj);
            if(value != null){
                statement.setObject(index,value);
                index++;
            }
        }
    }

    public static List parserResult(ResultSet result, List<Field> fields, Class c) throws SQLException,
            IllegalAccessException, InstantiationException {
        List<Object> list = new ArrayList<Object>();
        while (result.next()){
            Object model = c.newInstance();
            for(Field field:fields){
                Object value = result.getObject(field.getName());
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
}

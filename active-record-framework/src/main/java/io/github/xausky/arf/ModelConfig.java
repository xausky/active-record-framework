package io.github.xausky.arf;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by xausky on 11/10/16.
 */
public class ModelConfig {
    private List<Field> fields;
    private Field idField;
    private String table;


    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public Field getIdField() {
        return idField;
    }

    public void setIdField(Field idField) {
        this.idField = idField;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}

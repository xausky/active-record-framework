package io.github.xausky.arf.dialect;

/**
 * Created by xausky on 11/11/16.
 */
public class H2Dialect implements Dialect {
    @Override
    public String insert(String table, String[] keys) {
        boolean start = true;
        StringBuffer sql = new StringBuffer();
        sql.append("insert into `");
        sql.append(table).append("`(");
        StringBuilder temp = new StringBuilder();
        temp.append(") values(");
        for (String key:keys) {
            if(start){
                start = false;
            }else {
                sql.append(", ");
                temp.append(", ");
            }
            sql.append("`").append(key).append("`");
            temp.append("?");
        }
        sql.append(temp.toString()).append(")");
        return sql.toString();
    }

    @Override
    public String select(String table, String[] keys) {
        StringBuilder sql = new StringBuilder("select * from `").append(table).append("`");
        if(keys.length>0) {
            sql.append(" where ");
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) {
                    sql.append(" and ");
                }
                sql.append("`").append(keys[i]).append("` = ?");
            }
        }
        return sql.toString();
    }

    @Override
    public String update(String table,String[] sets, String[] keys) {
        StringBuffer sql = new StringBuffer();
        sql.append("update `").append(table).append("` set ");
        for (int i=0; i<sets.length; i++) {
            if(i>0){
                sql.append(", ");
            }
            sql.append("`").append(sets[i]).append("` = ? ");
        }
        if(keys.length>0) {
            sql.append(" where ");
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) {
                    sql.append(" and ");
                }
                sql.append("`").append(keys[i]).append("` = ?");
            }
        }
        return sql.toString();
    }

    @Override
    public String delete(String table, String[] keys) {
        StringBuilder sql = new StringBuilder("delete from `")
                .append(table).append("`");
        if(keys.length>0) {
            sql.append(" where ");
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) {
                    sql.append(" and ");
                }
                sql.append("`").append(keys[i]).append("` = ?");
            }
        }
        return sql.toString();
    }

    @Override
    public String paginate(int page, int size, String select) {
        int offset = page * (size - 1);
        StringBuilder ret = new StringBuilder();
        ret.append(select);
        ret.append(" limit ").append(offset)
                .append(", ").append(size);
        return ret.toString();
    }
}

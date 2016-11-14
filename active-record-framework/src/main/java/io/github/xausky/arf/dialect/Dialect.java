package io.github.xausky.arf.dialect;

import java.util.Set;

/**
 * Created by xausky on 11/11/16.
 */
public interface Dialect {
    String insert(String table,String[] keys);
    String select(String table,String[] keys);
    String delete(String table,String[] keys);
    String update(String table,String[] sets, String[] keys);
    String paginate(int page, int size, String select);
}

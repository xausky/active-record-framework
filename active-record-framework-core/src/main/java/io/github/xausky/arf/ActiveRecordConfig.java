package io.github.xausky.arf;

import io.github.xausky.arf.dialect.Dialect;
import io.github.xausky.arf.exception.ConfigException;

import javax.sql.DataSource;

/**
 * Created by xausky on 11/11/16.
 */
public class ActiveRecordConfig {
    private static ActiveRecordConfig self;
    private DataSource dataSource;
    private Dialect dialect;

    /**
     * 得到第一个New的Config对象作为默认对象.
     * @return 第一个New的Config对象作为默认对象.
     */
    public static ActiveRecordConfig getDefault(){
        return self;
    }

    public ActiveRecordConfig(DataSource dataSource, Dialect dialect) throws ConfigException {
        if(dataSource==null){
            throw new ConfigException("DataSource cannot is null.");
        }
        if(dialect == null){
            throw new ConfigException("Dialect cannot is null.");
        }
        this.dataSource = dataSource;
        this.dialect = dialect;
        if(self==null){
            self = this;
        }
    };

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }
}

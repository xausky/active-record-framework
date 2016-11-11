package io.github.xausky.arf;

import javax.sql.DataSource;

/**
 * Created by xausky on 11/11/16.
 */
public class DataSourceConfig {
    private static DataSource dataSource;

    public DataSourceConfig(DataSource dataSource){
        DataSourceConfig.dataSource = dataSource;
    }

    public static DataSource getDataSource(){
        return dataSource;
    }
}

package org.chronopolis.replicate.config;

import org.chronopolis.common.settings.DatabaseSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TODO: Is the name ok?
 * TODO: Value - should the property be db.replication or replication.db?
 *
 *
 * Created by shake on 10/16/14.
 */
@Component
public class ReplicationDBSettings implements DatabaseSettings {

    @Value("${db.replication.driver:org.hsqldb.jdbc.JDBCDriver}")
    private String driverClass;

    @Value("${db.replication.url:jdbc:hsqldb:/tmp/replication-db}")
    private String url;

    @Value("${db.replication.username:hsql}")
    private String username;

    @Value("${db.replication.driver:hsql}")
    private String password;


    @Override
    public String getDriverClass() {
        return driverClass;
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setDriverClass(final String driverClass) {
        this.driverClass = driverClass;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}

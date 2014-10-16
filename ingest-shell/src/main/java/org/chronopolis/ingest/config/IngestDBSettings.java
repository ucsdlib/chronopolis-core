package org.chronopolis.ingest.config;

import org.chronopolis.common.settings.DatabaseSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TODO: Is the name ok?
 * TODO: Value - should the property be db.ingest or ingest.db?
 *
 *
 * Created by shake on 10/16/14.
 */
@Component
public class IngestDBSettings implements DatabaseSettings {

    @Value("${db.ingest.driver:org.hsqldb.jdbc.JDBCDriver}")
    private String driverClass;

    @Value("${db.ingest.url:jdbc:hsqldb:/tmp/ingest-db}")
    private String url;

    @Value("${db.ingest.username:hsql}")
    private String username;

    @Value("${db.ingest.password:hsql}")
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

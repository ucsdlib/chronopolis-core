package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Migration to encode our users' passwords with bcrypt
 *
 * Created by shake on 9/7/16.
 */
public class V1_4__Bcrypt implements JdbcMigration {
    private final String update = "UPDATE users SET password = ? WHERE username = ?";
    private final Logger log = LoggerFactory.getLogger(V1_4__Bcrypt.class);

    @Override
    public void migrate(Connection connection) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        PreparedStatement ps = connection.prepareStatement("SELECT username, password FROM users");

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String user = rs.getString(1);
            log.info("Updating user {}", user);
            String password = rs.getString(2);

            String encrypted = encoder.encode(password);

            PreparedStatement us = connection.prepareStatement(update);
            us.setString(1, encrypted);
            us.setString(2, user);
            us.executeUpdate();
            us.close();
        }

        rs.close();
        ps.close();
    }
}

package org.chronopolis.replicate.config;

import org.chronopolis.db.common.RestoreRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by shake on 4/10/14.
 */
@Configuration
@EnableJpaRepositories(basePackages = "org.chronopolis.db",
        includeFilters = @ComponentScan.Filter(value = {RestoreRepository.class},
                                               type = FilterType.ASSIGNABLE_TYPE))
@EnableTransactionManagement
public class JPAConfiguration {

}

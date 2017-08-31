package org.chronopolis.tokenize;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.tokenize.config.TokenTaskConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.concurrent.TimeUnit;

@SpringBootApplication(scanBasePackageClasses = TokenTaskConfiguration.class,
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties({AceConfiguration.class, IngestAPIProperties.class})
public class TokenApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(TokenApplication.class));
    }

    @Override
    public void run(String... strings) throws Exception {
        // todo: prompt
        TimeUnit.MINUTES.sleep(5);
    }
}

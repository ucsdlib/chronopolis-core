package org.chronopolis.replicate.test;

import org.chronopolis.replicate.config.ReplicationConfig;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.scheduled.ReplicationQueryTask;
import org.chronopolis.replicate.service.ReplicationService;
import org.chronopolis.replicate.test.config.TestConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 *
 * Created by shake on 3/30/15.
 */
@ComponentScan(basePackageClasses = {
        ReplicationConfig.class,
        ReplicationQueryTask.class,
        ReplicationSettings.class,
        ReplicationService.class,
        TestConfig.class
}, basePackages = {
        "org.chronopolis.common.settings"
})
@EnableAutoConfiguration
public class TestApplication implements CommandLineRunner {
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");
        SpringApplication.exit(SpringApplication.run(TestApplication.class));
    }

    @Override
    public void run(String... strings) throws Exception {
    }
}

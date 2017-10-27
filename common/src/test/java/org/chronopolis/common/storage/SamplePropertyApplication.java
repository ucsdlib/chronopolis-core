package org.chronopolis.common.storage;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

/**
 *
 * @author shake
 */
@SpringBootApplication
@EnableConfigurationProperties(PreservationProperties.class)
public class SamplePropertyApplication implements CommandLineRunner{

    private final PreservationProperties preservationProperties;

    public SamplePropertyApplication(PreservationProperties preservationProperties) {
        this.preservationProperties = preservationProperties;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(SamplePropertyApplication.class)
                .logStartupInfo(false)
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        for (Posix posix : preservationProperties.getPosix()) {
            System.out.println("==================================");
            System.out.println("Posix Ingest Id: " + posix.getId());
            System.out.println("Posix Ingest Path: " + posix.getPath());
            System.out.println("==================================");
        }

    }

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new PreservationPropertiesValidator();
    }
}

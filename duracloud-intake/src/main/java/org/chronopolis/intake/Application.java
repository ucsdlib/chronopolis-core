package org.chronopolis.intake;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.db.intake.model.Status;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.config.JPAConfiguration;
import org.chronopolis.intake.duracloud.config.JPASettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * Created by shake on 8/1/14.
 */
@ComponentScan(basePackageClasses = {ChronopolisSettings.class,
        IntakeSettings.class},
        // JPASettings.class,
        // JPAConfiguration.class},
        basePackages = {"org.chronopolis.intake.config",
                        "org.chronopolis.intake.rest"})
@EntityScan(basePackageClasses = {Status.class})
@EnableAutoConfiguration
public class Application {

    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Application.class);
        ApplicationContext ctx = springApplication.run(args);
        System.out.println("Enter 'q' to exit");

        boolean done = false;
        while (!done) {
            if ("q".equalsIgnoreCase(readLine())) {
                done = true;
                SpringApplication.exit(ctx);
            }
        }

        //SpringApplication.exit(springApplication.run(args));
    }

}

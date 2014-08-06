package org.chronopolis.intake;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.config.JPAConfiguration;
import org.chronopolis.intake.duracloud.config.JPASettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by shake on 8/1/14.
 */
// TODO: Use packages or packageClasses?
@ComponentScan(basePackageClasses = {ChronopolisSettings.class,
        IntakeSettings.class,
        JPASettings.class,
        JPAConfiguration.class},
        basePackages = {"org.chronopolis.intake.config",
                        "org.chronopolis.intake.rest"})
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.run(args);
        //SpringApplication.exit(springApplication.run(args));
    }

}

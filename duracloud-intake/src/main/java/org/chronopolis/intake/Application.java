package org.chronopolis.intake;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.intake.config.IntakeConfiguration;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.config.JPAConfiguration;
import org.chronopolis.intake.duracloud.config.JPASettings;
import org.chronopolis.intake.rest.BagCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by shake on 8/1/14.
 */
@ComponentScan(basePackageClasses = {ChronopolisSettings.class,
        IntakeSettings.class,
        JPASettings.class,
        JPAConfiguration.class,
        IntakeConfiguration.class,
        BagCreator.class})
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.setLogStartupInfo(true);
        springApplication.run(args);
        //SpringApplication.exit(springApplication.run(args));
    }

}

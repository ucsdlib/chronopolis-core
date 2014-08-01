package org.chronopolis.intake.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by shake on 7/21/14.
 */
@RestController
@RequestMapping("/api/spring")
public class SpringEndpoint {

    private static ApplicationContext context;

    @RequestMapping("shutdown")
    public void shutdown() {
        SpringApplication.exit(context);
    }

    public static void setContext(ApplicationContext context) {
        SpringEndpoint.context = context;
    }

}

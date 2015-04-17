package org.chronopolis.ingest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by shake on 4/15/15.
 */
@Controller
public class SiteController {

    private final Logger log = LoggerFactory.getLogger(SiteController.class);

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getIndex(Model model) {
        log.debug("GET index");
        return "index";
    }

    @RequestMapping(value = "/login")
    public String login() {
        log.debug("LOGIN");
        return "login";
    }

}

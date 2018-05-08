package org.chronopolis.ingest.controller;

import org.chronopolis.tokenize.supervisor.DefaultSupervisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author shake
 */
@Controller
public class MetricsController {

    private final DefaultSupervisor supervisor;

    @Autowired
    public MetricsController(DefaultSupervisor supervisor) {
        this.supervisor = supervisor;
    }

    @GetMapping("/metrics/supervisor")
    public String getSupervisorIntrospection(Model model) {
        model.addAttribute("supervisor", supervisor);
        return "introspector";
    }
}

package org.chronopolis.ingest.controller;

import org.chronopolis.tokenize.supervisor.DefaultSupervisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * This probably isn't the most ideal solution to see what's going on with tokenization but it's a
 * start.
 *
 * @author shake
 */
@Controller
@ConditionalOnProperty(prefix = "ingest", name = "tokenizer.enabled", havingValue = "true")
public class TokenizerStatusController {

    private final DefaultSupervisor supervisor;

    @Autowired
    public TokenizerStatusController(DefaultSupervisor supervisor) {
        this.supervisor = supervisor;
    }

    @GetMapping("/status/supervisor")
    public String getSupervisorIntrospection(Model model) {
        model.addAttribute("supervisor", supervisor);
        return "introspector";
    }
}

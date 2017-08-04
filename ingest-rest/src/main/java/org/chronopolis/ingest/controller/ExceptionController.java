package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.exception.ForbiddenException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(ForbiddenException.class)
    public String handle403Exception(ForbiddenException ex) {
        return "forbidden";
    }

}

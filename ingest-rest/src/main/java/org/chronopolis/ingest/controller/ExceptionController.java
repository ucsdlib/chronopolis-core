package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.ForbiddenException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller to handle passing information to views which display errors
 *
 * Eventually this might be fleshed out to be more than just passing off to the view, but
 * we need to make updates to the exceptions and what not first
 *
 */
@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(ForbiddenException.class)
    public String handle403Exception(ForbiddenException ex) {
        return "exceptions/forbidden";
    }

    @ExceptionHandler(BadRequestException.class)
    public String handle403Exception(BadRequestException ex) {
        return "exceptions/bad_request";
    }

    @ExceptionHandler(NotFoundException.class)
    public String handle403Exception(NotFoundException ex) {
        return "exceptions/not_found";
    }

}

package org.chronopolis.ingest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor which logs requests
 *
 * @author shake
 */
public class LoggingInterceptor extends HandlerInterceptorAdapter {

    private final Logger log = LoggerFactory.getLogger("access-log");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String user = request.getRemoteUser() != null ? request.getRemoteUser() : "anonymous";
        String method = "[" + request.getMethod() + " - " + request.getServletPath() + "]";
        StringBuilder msg = new StringBuilder(method).append(" - ").append(user);
        if (request.getQueryString() != null) {
            msg.append("; params:").append(request.getQueryString());
        }

        log.info(msg.toString());
        return super.preHandle(request, response, handler);
    }

}

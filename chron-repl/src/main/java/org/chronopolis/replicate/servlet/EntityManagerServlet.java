/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.chronopolis.replicate.db.Dba;

/**
 * Entity manager servlet which wraps GET and POST commands
 *
 * @author toaster
 */
public abstract class EntityManagerServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(EntityManagerServlet.class);

    protected abstract void processRequest(HttpServletRequest request,
            HttpServletResponse response, Dba em)
            throws ServletException, IOException;

    private void wrapRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        Dba db = new Dba();

        try {
            NDC.push("[request " + request.getServletPath() + "] ");
            if (LOG.isTraceEnabled()) {
                LOG.trace("parameters: " + request.getQueryString());
            }
            processRequest(request, response, db);
        } catch (Throwable t) {
            LOG.error("Uncaught exception in servlet", t);
            if (t instanceof ServletException) {
                throw (ServletException) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }

        } finally {
            db.closeEm();
            NDC.pop();
        }
    }

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        wrapRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        wrapRequest(request, response);
    }
}

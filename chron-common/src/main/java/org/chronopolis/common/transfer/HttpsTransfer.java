/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.common.transfer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO: Stop requests that are sent w/ http
 *       Move other code over and what not
 *       
 *
 * @author shake
 */
public class HttpsTransfer extends FileTransfer {
    /**
     *
     * @param response
     */
    public int getFile(HttpServletResponse response) { 
        return 0;
    }
}
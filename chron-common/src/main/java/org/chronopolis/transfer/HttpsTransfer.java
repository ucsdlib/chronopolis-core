/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.transfer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 * @author shake
 */
@Controller
@RequestMapping("/somemapping")
public class HttpsTransfer {
    /**
     *
     * @param response
     */
    public void getFile(HttpServletResponse response) { 

    }
}

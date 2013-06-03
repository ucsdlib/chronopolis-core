/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.chronopolis.replicate.db.Dba;
import org.chronopolis.replicate.db.MyEntity;
import org.chronopolis.replicate.db.MyEntityManager;

/**
 *
 * @author toaster
 */
public class MyEntityServlet extends EntityManagerServlet {

    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response, Dba db) throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        MyEntityManager mgr = new MyEntityManager(db);
        try {
            /* TODO output your page here. You may use following sample code. */
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet MyEntityServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>All Entities</h1>");
            for (MyEntity me : mgr.listAll() )
            {
                out.println("<li>");
                out.println(me.getId());
                out.println(" ");
                out.println(me.getName());
            }
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }
}

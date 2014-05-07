package org.chronopolis.common.transfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * TODO: Move to HttpsTransfer
 *
 * @author shake
 */
public class FileServlet extends HttpServlet {

    /*
    private int blockSize = 65536;

    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response,
            EntityManager em) throws ServletException, IOException {
        Collection coll = getCollection(request, PersistUtil.getEntityManager());
        MonitoredItem it = getItem(request, PersistUtil.getEntityManager());
        StorageDriver driver = StorageDriverFactory.createStorageAccess(coll, em);

        String baseDir = coll.getDirectory(); //(it.getParentPath()==null) ? coll.getDirectory() :
                            //coll.getDirectory().concat(it.getParentPath());
        if ( it == null ) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        System.out.println("Base: " + baseDir);
        System.out.println("File: " + it.getPath());

        File file = new File(baseDir, it.getPath());
        InputStream is = driver.getItemInputStream(it.getPath());

        System.out.println(file.exists() + " :: " + file.getAbsolutePath() + ":::" + baseDir);
        System.out.println(file.getName());

        if ( !file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = request.getContentType();
        if ( contentType == null ) {
            contentType = "application/octet-stream";
        }
        response.reset();
        response.setBufferSize(blockSize);
        response.setContentType(contentType);
        response.setHeader("Content-Length", String.valueOf(file.length()));
        System.out.println("Serving file: " + it.getPath());
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                it.getPath() + "\"");

        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {
            in = new BufferedInputStream(new FileInputStream(file), blockSize);
            out = new BufferedOutputStream(response.getOutputStream(), blockSize);

            byte [] buffer = new byte[blockSize];
            int length;
            while ( (length= in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } finally {
            in.close();
            out.close();
        }
    }
    */

}

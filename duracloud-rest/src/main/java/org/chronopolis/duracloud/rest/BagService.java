/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.duracloud.rest;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTful service for notifying us a collection is ready to have a snapshot made
 *
 * @author shake
 */
@Path("bag")
public class BagService {
    private static final Logger log = LoggerFactory.getLogger(BagService.class);
    public static final String MD5_HEAD = "Content-MD5";

    @Context
    private HttpServletRequest request;

    
    @PUT
    @Path("{depositor}/{spaceId}")
    @Consumes("test/plain")
    public Response createSnapshot(@PathParam("depositor") String depositor,
                                   @PathParam("collection") String spaceId, 
                                   @Context HttpHeaders headers,
                                   @HeaderParam(MD5_HEAD) String digest) {
        try {
            InputStream is = request.getInputStream();
            IngestRequest ir = new IngestRequest(depositor, spaceId);
            String compDigest = ir.readStream(is, MessageDigest.getInstance("MD5"));

            // TODO: Give reasons why it's a bad request
            if (digest == null || !digest.equals(compDigest)) {
                log.error("Manifest mismatch");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ir.hasErrors()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            // TODO: Create bag in place
        } catch (IOException ex) {
        } catch (NoSuchAlgorithmException ex) {
        } 
        
        return Response.ok().build();
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.duracloud.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * RESTful service for notifying us a collection is ready to have a snapshot made
 *
 * @author shake
 */
@Path("bag")
public class BagService {
    
    @POST
    @Path("{depositor}/{collection}")
    public Response createSnapshot(@PathParam("depositor") String depositor,
                                   @PathParam("collection") String collection) {
        // Something something
        // Validate manifest
        // Create bag
        return Response.ok().build();
    }
}

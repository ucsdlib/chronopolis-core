/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

/**
 * Could use this for broadcast routing keys
 *
 * @author toaster
 */
public enum RoutingKey {
    INGEST_BROADCAST("ingest.broadcast"),
    REPLICATE_BROADCAST("replicate.broadcast");

    private final String route;

    private RoutingKey(String route) {
        this.route = route;
    }

    public String asRoute() {
        return route;
    }

}

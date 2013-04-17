/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

/**
 * Each of our control flows will have a unique identifier attached to it.  For example, Content Ingest
 * will be "ingest".  Every message exchanged during the control flow / process will be has this tag.
 * ProcessType
 * @author toaster
 */
public enum ProcessType {

    INGEST("ingest"),
    DISTRIBUTE("distribute");
    private String name;

    private ProcessType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

package org.chronopolis.common.ace;

import java.util.Date;

/**
 *
 * Created by shake on 4/17/17.
 */
public class MonitoredItem {

    private Long id;
    private String path;
    private String state;
    private String fileDigest;
    private Long size;
    private Date lastSeen;
    private Date stateChange;
    private Date lastVisited;

    public Long getId() {
        return id;
    }

    public MonitoredItem setId(Long id) {
        this.id = id;
        return this;
    }

    public String getPath() {
        return path;
    }

    public MonitoredItem setPath(String path) {
        this.path = path;
        return this;
    }

    public String getState() {
        return state;
    }

    public MonitoredItem setState(String state) {
        this.state = state;
        return this;
    }

    public String getFileDigest() {
        return fileDigest;
    }

    public MonitoredItem setFileDigest(String fileDigest) {
        this.fileDigest = fileDigest;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public MonitoredItem setSize(Long size) {
        this.size = size;
        return this;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public MonitoredItem setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
        return this;
    }

    public Date getStateChange() {
        return stateChange;
    }

    public MonitoredItem setStateChange(Date stateChange) {
        this.stateChange = stateChange;
        return this;
    }

    public Date getLastVisited() {
        return lastVisited;
    }

    public MonitoredItem setLastVisited(Date lastVisited) {
        this.lastVisited = lastVisited;
        return this;
    }
}

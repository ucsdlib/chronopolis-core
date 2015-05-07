package org.chronopolis.common.dpn;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of a Bag in the DPN-REST Server
 *
 * Created by shake on 5/6/15.
 */
public class DPNBag {

    private char bagType; // One of 'R', 'D', 'I'
    private String uuid;
    private String localId;
    private String firstVersionUuid;
    private String ingestNode;
    private String adminNode;

    private List<String> interpretive;
    private List<String> rights;
    private List<String> replicatingNodes;
    private Map<String, String> fixities;

    private DateTime createdAt;
    private DateTime updatedAt;

    private Long size;
    private Long version;

    public DPNBag() {
        this.interpretive = new ArrayList<>();
        this.rights = new ArrayList<>();
        this.replicatingNodes = new ArrayList<>();
        this.fixities = new HashMap<>();
    }

    public char getBagType() {
        return bagType;
    }

    public DPNBag setBagType(char bagType) {
        this.bagType = bagType;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public DPNBag setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getLocalId() {
        return localId;
    }

    public DPNBag setLocalId(String localId) {
        this.localId = localId;
        return this;
    }

    public String getFirstVersionUuid() {
        return firstVersionUuid;
    }

    public DPNBag setFirstVersionUuid(String firstVersionUuid) {
        this.firstVersionUuid = firstVersionUuid;
        return this;
    }

    public String getIngestNode() {
        return ingestNode;
    }

    public DPNBag setIngestNode(String ingestNode) {
        this.ingestNode = ingestNode;
        return this;
    }

    public String getAdminNode() {
        return adminNode;
    }

    public DPNBag setAdminNode(String adminNode) {
        this.adminNode = adminNode;
        return this;
    }

    public List<String> getInterpretive() {
        return interpretive;
    }

    public DPNBag setInterpretive(List<String> interpretive) {
        this.interpretive = interpretive;
        return this;
    }

    public DPNBag addInterpretive(String interpretive) {
        this.interpretive.add(interpretive);
        return this;
    }

    public List<String> getRights() {
        return rights;
    }

    public DPNBag setRights(List<String> rights) {
        this.rights = rights;
        return this;
    }

    public DPNBag addRights(String uuid) {
        rights.add(uuid);
        return this;
    }

    public List<String> getReplicatingNodes() {
        return replicatingNodes;
    }

    public DPNBag setReplicatingNodes(List<String> replicatingNodes) {
        this.replicatingNodes = replicatingNodes;
        return this;
    }

    public DPNBag addReplicatingNode(String name) {
        replicatingNodes.add(name);
        return this;
    }

    public Map<String, String> getFixities() {
        return fixities;
    }

    public DPNBag setFixities(Map<String, String> fixities) {
        this.fixities = fixities;
        return this;
    }

    public DPNBag addFixity(String algorithm, String digest) {
        this.fixities.put(algorithm, digest);
        return this;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public DPNBag setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public DPNBag setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public DPNBag setSize(Long size) {
        this.size = size;
        return this;
    }

    public Long getVersion() {
        return version;
    }

    public DPNBag setVersion(Long version) {
        this.version = version;
        return this;
    }
}

package org.chronopolis.common.dpn;


import java.util.HashSet;
import java.util.Set;

/**
 * Created by shake on 9/30/14.
 */
public class RegistryItemModel {

    private String dpnObjectId;
    private String localId;
    private String location;
    private String firstNodeName;
    private String previousVersionObjectId;
    private String forwardVersionObjectId;
    private String firstVersionId;
    private String fixityAlgorithm;
    private String fixityValue;
    private String objectType;
    private String state;

    private String lastFixityDate;
    private String creationDate;
    private String lastModifiedDate;

    private long versionNumber;
    private long bagSize;

    private Set<String> replicatingNodeNames = new HashSet<>();
    private Set<String> brighteningObjectId = new HashSet<>();
    private Set<String> rightsObjectId = new HashSet<>();


    public String getDpnObjectId() {
        return dpnObjectId;
    }

    public void setDpnObjectId(final String dpnObjectId) {
        this.dpnObjectId = dpnObjectId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(final String localId) {
        this.localId = localId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getFirstNodeName() {
        return firstNodeName;
    }

    public void setFirstNodeName(final String firstNodeName) {
        this.firstNodeName = firstNodeName;
    }

    public String getPreviousVersionObjectId() {
        return previousVersionObjectId;
    }

    public void setPreviousVersionObjectId(final String previousVersionObjectId) {
        this.previousVersionObjectId = previousVersionObjectId;
    }

    public String getForwardVersionObjectId() {
        return forwardVersionObjectId;
    }

    public void setForwardVersionObjectId(final String forwardVersionObjectId) {
        this.forwardVersionObjectId = forwardVersionObjectId;
    }

    public String getFirstVersionId() {
        return firstVersionId;
    }

    public void setFirstVersionId(final String firstVersionId) {
        this.firstVersionId = firstVersionId;
    }

    public String getFixityAlgorithm() {
        return fixityAlgorithm;
    }

    public void setFixityAlgorithm(final String fixityAlgorithm) {
        this.fixityAlgorithm = fixityAlgorithm;
    }

    public String getFixityValue() {
        return fixityValue;
    }

    public void setFixityValue(final String fixityValue) {
        this.fixityValue = fixityValue;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(final String objectType) {
        this.objectType = objectType;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getLastFixityDate() {
        return lastFixityDate;
    }

    public void setLastFixityDate(final String lastFixityDate) {
        this.lastFixityDate = lastFixityDate;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(final String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(final long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public long getBagSize() {
        return bagSize;
    }

    public void setBagSize(final long bagSize) {
        this.bagSize = bagSize;
    }

    public Set<String> getReplicatingNodeNames() {
        return replicatingNodeNames;
    }

    public void addReplicatingNode(String node) {
        this.replicatingNodeNames.add(node);
    }

    public void setReplicatingNodeNames(final Set<String> replicatingNodeNames) {
        this.replicatingNodeNames = replicatingNodeNames;
    }

    public Set<String> getBrighteningObjectId() {
        return brighteningObjectId;
    }

    public void setBrighteningObjectId(final Set<String> brighteningObjectId) {
        this.brighteningObjectId = brighteningObjectId;
    }

    public Set<String> getRightsObjectId() {
        return rightsObjectId;
    }

    public void setRightsObjectId(final Set<String> rightsObjectId) {
        this.rightsObjectId = rightsObjectId;
    }
}

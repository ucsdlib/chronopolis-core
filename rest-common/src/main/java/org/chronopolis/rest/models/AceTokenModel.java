package org.chronopolis.rest.models;

import java.time.ZonedDateTime;

/**
 * RESTful model for AceTokens
 *
 * @author shake
 */
public class AceTokenModel {

    private Long id;
    private Long bagId;
    private Long round;
    private String proof;
    private String filename;
    private String algorithm;
    private String imsService;
    private ZonedDateTime createDate;

    public Long getId() {
        return id;
    }

    public AceTokenModel setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getBagId() {
        return bagId;
    }

    public AceTokenModel setBagId(Long bagId) {
        this.bagId = bagId;
        return this;
    }

    public Long getRound() {
        return round;
    }

    public AceTokenModel setRound(Long round) {
        this.round = round;
        return this;
    }

    public String getProof() {
        return proof;
    }

    public AceTokenModel setProof(String proof) {
        this.proof = proof;
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public AceTokenModel setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public String getImsService() {
        return imsService;
    }

    public AceTokenModel setImsService(String imsService) {
        this.imsService = imsService;
        return this;
    }

    public ZonedDateTime getCreateDate() {
        return createDate;
    }

    public AceTokenModel setCreateDate(ZonedDateTime createDate) {
        this.createDate = createDate;
        return this;
    }

    public AceTokenModel setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getFilename() {
        return filename;
    }

}


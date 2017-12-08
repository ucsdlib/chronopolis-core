package org.chronopolis.rest.models.storage;

import org.chronopolis.rest.support.StorageUnit;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Information for form data which is used to create a StagingStorage model/entity/whatever
 *
 * @author shake
 */
public class StagingCreate {

    /**
     * The relative location of the data which has been staged
     */
    @NotBlank
    private String location;

    /**
     * The ID of the StorageRegion which data is staged in
     */
    @NotNull
    private Long storageRegion;

    /**
     * The number of files staged
     */
    @Min(1L)
    @NotNull
    private Long totalFiles;

    /**
     * The size on disk of the data
     */
    @Min(1L)
    @NotNull
    private Long size;

    /**
     * The StorageUnit for the size
     */
    @NotNull
    private StorageUnit storageUnit;

    public String getLocation() {
        return location;
    }

    public StagingCreate setLocation(String location) {
        this.location = location;
        return this;
    }

    public Long getStorageRegion() {
        return storageRegion;
    }

    public StagingCreate setStorageRegion(Long storageRegion) {
        this.storageRegion = storageRegion;
        return this;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public StagingCreate setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public StagingCreate setSize(Long size) {
        this.size = size;
        return this;
    }

    public StorageUnit getStorageUnit() {
        return storageUnit;
    }

    public StagingCreate setStorageUnit(StorageUnit storageUnit) {
        this.storageUnit = storageUnit;
        return this;
    }
}

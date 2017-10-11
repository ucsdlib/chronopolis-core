package org.chronopolis.rest.models;

import org.chronopolis.rest.models.storage.DataType;
import org.chronopolis.rest.models.storage.StorageType;
import org.chronopolis.rest.support.StorageUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class RegionEdit {

    @NotNull
    @Min(1)
    private Long capacity;

    @NotNull
    private StorageUnit storageUnit;

    @NotNull
    private DataType dataType;

    @NotNull
    private StorageType storageType;

    public Long getCapacity() {
        return capacity;
    }

    public RegionEdit setCapacity(Long capacity) {
        this.capacity = capacity;
        return this;
    }

    public StorageUnit getStorageUnit() {
        return storageUnit;
    }

    public RegionEdit setStorageUnit(StorageUnit storageUnit) {
        this.storageUnit = storageUnit;
        return this;
    }

    public DataType getDataType() {
        return dataType;
    }

    public RegionEdit setDataType(DataType dataType) {
        this.dataType = dataType;
        return this;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public RegionEdit setStorageType(StorageType storageType) {
        this.storageType = storageType;
        return this;
    }
}

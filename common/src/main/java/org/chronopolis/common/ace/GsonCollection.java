package org.chronopolis.common.ace;


import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulate the needed fields for a JSON post and serialization using gson
 *
 * Created by shake on 2/20/14.
 */
public class GsonCollection {
    private String digestAlgorithm;
    private String directory;
    private String name;
    private String storage;
    private Setting settings;

    public GsonCollection() {}

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public Setting getSettings() {
        return settings;
    }

    public void setSettings(Setting settings) {
        this.settings = settings;
    }

    public class Setting {
        private final List<Entry> entry;

        public Setting() {
            entry = new ArrayList<>();
        }

        // We'll have a method for each of the entries we can add
        public void setAuditTokens() {
        }

        public void setAuditPeriod() {
        }

        public void setProxyData() {
        }
    }

    public class Entry {
        public final String key;
        public final String value;

        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}

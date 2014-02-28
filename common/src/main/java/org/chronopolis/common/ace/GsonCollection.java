package org.chronopolis.common.ace;


import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulate the needed fields for a JSON post and serialization using gson (or jersey??)
 *
 * Created by shake on 2/20/14.
 */
public class GsonCollection {
    private String digestAlgorithm;
    private String directory;
    private String name;
    private String group;
    private String storage;
    private Setting settings;

    public GsonCollection() {
        this.settings = new Setting();
    }

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

    public void addSetting(String key, String val) {
        Entry entry = new Entry(key, val);
        settings.entry.add(entry);
    }

    // We'll have a method for each of the entries we can add
    public void setAuditTokens(String val) {
        Entry entry = new Entry("audit.tokens", val);
        settings.entry.add(entry);
    }

    public void setAuditPeriod(String val) {
        Entry entry = new Entry("audit.period", val);
        settings.entry.add(entry);
    }

    public void setProxyData(String val) {
        Entry entry = new Entry("proxy.data", val);
        settings.entry.add(entry);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public class Setting {
        private final List<Entry> entry;

        public Setting() {
            entry = new ArrayList<>();
        }

    }

    public class Entry {
        private final String key;
        private final String value;

        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}

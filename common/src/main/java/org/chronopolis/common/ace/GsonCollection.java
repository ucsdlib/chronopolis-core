package org.chronopolis.common.ace;


import com.google.gson.Gson;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulate the needed fields for a JSON post and serialization using gson (or jersey??)
 *
 * Created by shake on 2/20/14.
 */
public class GsonCollection {
    private long id;
    private final String digestAlgorithm;
    private final String directory;
    private final String name;
    private final String group;
    private final String storage;
    private final Setting settings;

    private GsonCollection(final Builder builder) {
        this.digestAlgorithm = builder.digestAlgorithm;
        this.directory = builder.directory;
        this.name = builder.name;
        this.group = builder.group;
        this.storage = builder.storage;
        this.settings = builder.settings;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }


    public String getDirectory() {
        return directory;
    }

    public String getName() {
        return name;
    }

    public String getStorage() {
        return storage;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Setting getSettings() {
        return settings;
    }

    public void addSetting(final String key, final String val) {
        Entry entry = new Entry(key, val);
        settings.entry.add(entry);
    }

    public String getGroup() {
        return group;
    }

    public static class Setting {
        private final List<Entry> entry;

        public Setting() {
            entry = new ArrayList<>();
        }

        public List<Entry> getEntry() {
            return entry;
        }
    }

    public static class Entry {
        private final String key;
        private final String value;

        public Entry(final String key, final String value) {
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

    public String toJsonJackson() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationConfig(mapper.getSerializationConfig()
                .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL));
        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder {
        private String digestAlgorithm;
        private String directory;
        private String name;
        private String group;
        private String storage;
        private Setting settings;

        public Builder() {
            this.settings = new Setting();
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder directory(final String directory) {
            this.directory = directory;
            return this;
        }

        public Builder digestAlgorithm(final String digestAlgorithm) {
            this.digestAlgorithm = digestAlgorithm;
            return this;
        }

        public Builder group(final String group) {
            this.group = group;
            return this;
        }

        public Builder storage(final String storage) {
            this.storage = storage;
            return this;
        }

        // We'll have a method for each of the entries we can add
        public Builder auditTokens(final String val) {
            Entry entry = new Entry("audit.tokens", val);
            settings.entry.add(entry);
            return this;
        }

        public Builder auditPeriod(final String val) {
            Entry entry = new Entry("audit.period", val);
            settings.entry.add(entry);
            return this;
        }

        public Builder proxyData(final String val) {
            Entry entry = new Entry("proxy.data", val);
            settings.entry.add(entry);
            return this;
        }

        public GsonCollection build() {
            return new GsonCollection(this);
        }
    }

}

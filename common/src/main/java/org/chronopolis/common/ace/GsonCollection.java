package org.chronopolis.common.ace;


import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, String> settings;
    private final String state;

    private GsonCollection(final Builder builder) {
        this.digestAlgorithm = builder.digestAlgorithm;
        this.directory = builder.directory;
        this.name = builder.name;
        this.group = builder.group;
        this.storage = builder.storage;
        this.settings = ImmutableMap.copyOf(builder.settings);
        this.state = builder.state;
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

    public Map<String, String> getSettings() {
        return settings;
    }

    public String getGroup() {
        return group;
    }

    public String getState() {
        return state;
    }

    // TODO: What's the purpose of these to json methods?
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
        private Map<String, String> settings;
        public String state;

        public Builder() {
            this.settings = new HashMap<>();
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

        public Builder state(final String state) {
            this.state = state;
            return this;
        }

        // We'll have a method for each of the entries we can add
        public Builder auditTokens(final String val) {
            settings.put("audit.tokens", val);
            return this;
        }

        public Builder auditPeriod(final String val) {
            settings.put("audit.period", val);
            return this;
        }

        public Builder proxyData(final String val) {
            settings.put("proxy.data", val);
            return this;
        }

        public GsonCollection build() {
            return new GsonCollection(this);
        }
    }

}

package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import org.chronopolis.ingest.models.Paged;

import java.util.List;

/**
 * Data binding for queries on AceTokens
 *
 * @author shake
 */
public class AceTokenFilter extends Paged {

    private Long bagId;
    private String algorithm;
    private List<String> filename;

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public Long getBagId() {
        return bagId;
    }

    public AceTokenFilter setBagId(Long bagId) {
        this.bagId = bagId;
        parameters.put("bagId", bagId.toString());
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public AceTokenFilter setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        parameters.put("algorithm", algorithm);
        return this;
    }

    public List<String> getFilename() {
        return filename;
    }

    public AceTokenFilter setFilename(List<String> filename) {
        this.filename = filename;
        // need to test putAll
        parameters.putAll("filename", filename);
        return this;
    }

    @Override
    public LinkedListMultimap<String, String> getParameters() {
        return parameters;
    }

    public AceTokenFilter setParameters(LinkedListMultimap<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }
}

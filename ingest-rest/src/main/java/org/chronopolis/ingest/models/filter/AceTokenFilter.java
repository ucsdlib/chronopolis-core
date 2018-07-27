package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.kot.entities.QAceToken;

/**
 * Data binding for queries on AceTokens
 *
 * @author shake
 */
public class AceTokenFilter extends Paged {

    private QAceToken aceToken = QAceToken.aceToken;
    private final BooleanBuilder builder = new BooleanBuilder();

    private Long bagId;
    private String algorithm;
    private String filename;

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public Long getBagId() {
        return bagId;
    }

    public AceTokenFilter setBagId(Long bagId) {
        if (bagId != null) {
            this.bagId = bagId;
            parameters.put("bagId", bagId.toString());
            builder.and(aceToken.bag.id.eq(bagId));
        }
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public AceTokenFilter setAlgorithm(String algorithm) {
        if (algorithm != null && !algorithm.isEmpty()) {
            this.algorithm = algorithm;
            parameters.put("algorithm", algorithm);
            builder.and(aceToken.algorithm.eq(algorithm));
        }
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public AceTokenFilter setFilename(String filename) {
        // might be good to check each filename for emptiness
        if (filename != null && ! filename.isEmpty()) {
            this.filename = filename;
            // parameters.putAll("filename", filename);
            parameters.put("filename", filename);
            builder.and(aceToken.filename.eq(filename));
        }
        return this;
    }

    @Override
    public LinkedListMultimap<String, String> getParameters() {
        return parameters;
    }

    @Override
    public BooleanBuilder getQuery() {
        return builder;
    }

    @Override
    public OrderSpecifier getOrderSpecifier() {
        Order dir = getDirection();
        OrderSpecifier orderSpecifier;

        switch (getOrderBy()) {
            case "bagId":
                orderSpecifier = new OrderSpecifier<>(dir, aceToken.bag.id);
                break;
            case "filename":
                orderSpecifier = new OrderSpecifier<>(dir, aceToken.filename);
                break;
            case "createdAt":
                orderSpecifier = new OrderSpecifier<>(dir, aceToken.createDate);
                break;
            default:
                orderSpecifier = new OrderSpecifier<>(dir, aceToken.id);
                break;
        }

        return orderSpecifier;
    }

    public AceTokenFilter setParameters(LinkedListMultimap<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }
}

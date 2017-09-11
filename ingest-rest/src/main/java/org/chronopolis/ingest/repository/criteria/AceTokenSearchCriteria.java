package org.chronopolis.ingest.repository.criteria;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.QAceToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AceTokenSearchCriteria implements SearchCriteria {

    private final QAceToken aceToken;
    private Map<Object, BooleanExpression> criteria;

    public AceTokenSearchCriteria() {
        aceToken = QAceToken.aceToken;
        criteria = new HashMap<>();
    }

    public AceTokenSearchCriteria withId(Long id) {
        if (id != null) {
            criteria.put(Params.SORT_ID, aceToken.id.eq(id));
        }

        return this;
    }

    public AceTokenSearchCriteria withBagId(Long bagId) {
        if (bagId != null) {
            criteria.put("BAG_ID", aceToken.bag.id.eq(bagId));
        }

        return this;
    }

    public AceTokenSearchCriteria withFilenames(List<String> filenames) {
        if (filenames != null && !filenames.isEmpty()) {
            criteria.put("PATHS", aceToken.filename.in(filenames));
        }

        return this;
    }

    public AceTokenSearchCriteria withAlgorithm(String algorithm) {
        if (algorithm != null && !algorithm.isEmpty()) {
            criteria.put("ALGORITHM", aceToken.algorithm.eq(algorithm));
        }

        return this;
    }

    @Override
    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}

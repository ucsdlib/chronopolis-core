package org.chronopolis.ingest.api;

import org.chronopolis.ingest.models.filter.AceTokenFilter;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.QAceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * API for querying tokens
 * <p>
 * Created by shake on 7/27/17.
 */
@RestController
@RequestMapping("/api/tokens")
public class TokenController {
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final PagedDao dao;

    @Autowired
    public TokenController(PagedDao dao) {
        this.dao = dao;
    }

    /**
     * Retrieve ACE Tokens, optionally filtered
     *
     * @param principal the user requesting to retrieve tokens
     * @param filter    the parameters to filter on
     * @return the ACE Tokens matching the filter
     */
    @GetMapping
    public Page<AceToken> getTokens(Principal principal, @ModelAttribute AceTokenFilter filter) {
        access.info("[GET /api/tokens] - {}", principal.getName());
        return dao.findPage(QAceToken.aceToken, filter);
    }

    /**
     * Retrieve an ACE Token by its id
     *
     * @param principal the user requesting the token
     * @param id        the id of the token
     * @return the ACE Token
     */
    @GetMapping("/{id}")
    public ResponseEntity<AceToken> getToken(Principal principal, @PathVariable("id") Long id) {
        access.info("[GET /api/tokens/{}] - ", id, principal.getName());
        AceToken token = dao.findOne(QAceToken.aceToken, QAceToken.aceToken.id.eq(id));
        ResponseEntity<AceToken> response = ResponseEntity.ok(token);
        if (token == null) {
            response = ResponseEntity.notFound().build();
        }

        return response;
    }

}

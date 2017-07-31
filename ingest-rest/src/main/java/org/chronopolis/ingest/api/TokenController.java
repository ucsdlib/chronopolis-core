package org.chronopolis.ingest.api;

import org.chronopolis.ingest.models.Paged;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.AceTokenSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.AceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * API for querying tokens
 *
 * Created by shake on 7/27/17.
 */
@RestController
@RequestMapping("/api/tokens")
public class TokenController {
    private final Logger log = LoggerFactory.getLogger(TokenController.class);

    private final SearchService<AceToken, Long, TokenRepository> tokens;

    @Autowired
    public TokenController(SearchService<AceToken, Long, TokenRepository> tokenService) {
        this.tokens = tokenService;
    }

    @GetMapping
    public Page<AceToken> getTokens(Principal principal, @ModelAttribute Paged paged) {
        log.info("[GET /api/tokens] - {}", principal.getName());
        AceTokenSearchCriteria criteria = new AceTokenSearchCriteria();
        return tokens.findAll(criteria, paged.createPageRequest());
    }

    @GetMapping("/{id}")
    public AceToken getToken(Principal principal, @PathVariable("id") Long id) {
        log.info("[GET /api/tokens/{}] - ", id, principal.getName());
        AceTokenSearchCriteria criteria = new AceTokenSearchCriteria().withId(id);
        return tokens.find(criteria);
    }

}

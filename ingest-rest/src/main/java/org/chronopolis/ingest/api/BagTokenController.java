package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.models.filter.AceTokenFilter;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.AceTokenSearchCriteria;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.AceTokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Date;

/**
 * Operations on the tokens of a bag
 *
 * @author shake
 */
@RestController
@RequestMapping("/api/bags/{id}")
public class BagTokenController {

    private final Logger log = LoggerFactory.getLogger(BagTokenController.class);

    private final SearchService<Bag, Long, BagRepository> bags;
    private final SearchService<AceToken, Long, TokenRepository> tokens;

    @Autowired
    public BagTokenController(SearchService<Bag, Long, BagRepository> bagService,
                              SearchService<AceToken, Long, TokenRepository> tokenService) {
        this.bags = bagService;
        this.tokens = tokenService;
    }

    @GetMapping("/tokens")
    public Page<AceToken> getTokensForBag(Principal principal, @PathVariable("id") Long id, @ModelAttribute AceTokenFilter filter) {
        log.info("[GET /api/bags/{}/tokens] - {}", id, principal.getName());
        AceTokenSearchCriteria searchCriteria = new AceTokenSearchCriteria()
                .withBagId(id)
                .withFilenames(filter.getFilename())
                .withAlgorithm(filter.getAlgorithm());

        return tokens.findAll(searchCriteria, filter.createPageRequest());
    }

    @PostMapping("/tokens")
    public ResponseEntity<AceToken> createTokenForBag(Principal principal,
                                                      @PathVariable("id") Long id,
                                                      @RequestBody AceTokenModel model) {
        log.info("[POST /api/bags/{}/tokens] - {}", id, principal.getName());

        ResponseEntity response; // = ResponseEntity.status(HttpStatus.CREATED);
        Bag bag = bags.find(new BagSearchCriteria().withId(id));
        AceToken token = tokens.find(new AceTokenSearchCriteria().withBagId(id).withFilenames(ImmutableList.of(model.getFilename())));
        if (bag == null) {
            response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else if (token != null) {
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
        } else {
            token = new AceToken(bag,
                    Date.from(model.getCreateDate().toInstant()),
                    model.getFilename(),
                    model.getProof(),
                    model.getImsService(),
                    model.getAlgorithm(),
                    model.getRound());
            tokens.save(token);
            response = ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(token);
        }

        return response;
    }

}

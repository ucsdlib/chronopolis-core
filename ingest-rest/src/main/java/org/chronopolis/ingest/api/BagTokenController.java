package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.filter.AceTokenFilter;
import org.chronopolis.ingest.repository.dao.TokenDao;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.models.create.AceTokenCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.security.Principal;

/**
 * Operations on the tokens of a bag
 *
 * @author shake
 */
@RestController
@RequestMapping("/api/bags/{id}")
public class BagTokenController extends IngestController {

    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final TokenDao dao;

    @Autowired
    public BagTokenController(TokenDao dao) {
        this.dao = dao;
    }

    /**
     * Retrieve all tokens for a bag
     *
     * @param principal the user requesting the tokens
     * @param id        the id of the bag
     * @param filter    parameters to filter on
     * @return A paged view of tokens
     */
    @GetMapping("/tokens")
    public Page<AceToken> getTokensForBag(Principal principal,
                                          @PathVariable("id") Long id,
                                          @ModelAttribute AceTokenFilter filter) {
        access.info("[GET /api/bags/{}/tokens] - {}", id, principal.getName());
        filter.setBagId(id);
        return dao.findPage(QAceToken.aceToken, filter);
    }

    /**
     * Create a token for a given bag
     * <p>
     * ResponseCodes:
     * 201 - Created successfully
     * 400 - RequestBody could not be validated
     * 401 - Unauthorized
     * 403 - User is forbidden for modification of the resource (i.e. not the owner of the bag)
     * 404 - Bag not found
     * 409 - Token already exists for bag
     *
     * @param principal the user creating an ACE Token for a bag
     * @param id        the id of the bag
     * @param model     the information for the ACE Token
     * @return the newly created token
     */
    @PostMapping("/tokens")
    public ResponseEntity<AceToken> createTokenForBag(Principal principal,
                                                      @PathVariable("id") Long id,
                                                      @Valid @RequestBody AceTokenCreate model) {
        access.info("[POST /api/bags/{}/tokens] - {}", id, principal.getName());
        access.info("Post parameters - {};{}", model.getBagId(), model.getFilename());
        ResponseEntity<AceToken> token = dao.createToken(principal, id, model);
        access.info("YO RESPONSE IS {}", token.getStatusCode());
        return token;
    }

}

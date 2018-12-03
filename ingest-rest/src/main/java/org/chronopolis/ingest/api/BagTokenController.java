package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.filter.AceTokenFilter;
import org.chronopolis.ingest.repository.dao.TokenDao;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.models.create.AceTokenCreate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
        filter.setBagId(id);
        return dao.findPage(QAceToken.aceToken, filter);
    }

    /**
     * Retrieve an {@link AceToken} given a Bag identifier and a File identifier
     *
     * @param principal the security principal of the user
     * @param bagId     the identifier of the {@link Bag}
     * @param fileId    the identifier of the {@link BagFile}
     * @return HTTP 200 - An AceToken was found for the given Bag and File
     *         HTTP 404 - An AceToken was not found for the given Bag and File
     */
    @GetMapping("/files/{file_id}/token")
    public ResponseEntity<AceToken> getTokenForFile(Principal principal,
                                                    @PathVariable("id") Long bagId,
                                                    @PathVariable("file_id") Long fileId) {
        ResponseEntity<AceToken> response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        AceToken token = dao.findOne(QAceToken.aceToken,
                QAceToken.aceToken.bag.id.eq(bagId).and(QAceToken.aceToken.file.id.eq(fileId)));

        if (token != null) {
            response = ResponseEntity.ok(token);
        }

        return response;
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
        return dao.createToken(principal, id, model);
    }

    /**
     * Create a token for a given bag and file
     *
     * If the file obtained by the file_id does not correspond to the filename in the AceTokenModel
     * or belong to the Bag obtained by the bag_id, a BadRequest will be returned.
     *
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
    @PostMapping("/files/{file_id}/token")
    public ResponseEntity<AceToken> createTokenForFile(Principal principal,
                                                       @PathVariable("id") Long id,
                                                       @PathVariable("file_id") Long fileId,
                                                       @Valid @RequestBody AceTokenCreate model) {
        return dao.createToken(principal, id, fileId, model);
    }

}

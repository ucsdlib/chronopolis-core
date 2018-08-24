package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.models.create.AceTokenCreate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import java.security.Principal;
import java.util.Date;

import static org.chronopolis.ingest.IngestController.hasRoleAdmin;

/**
 * Data accessor for AceTokens. Really just to move the logic for creating a token from a
 * {@link AceTokenCreate} out of the BagTokenController.
 *
 * Can we make this a {@link org.springframework.stereotype.Component} to avoid needed to create the
 * Bean ourselves?
 *
 * @author shake
 */
public class TokenDao extends PagedDAO {
    private EntityManager em;

    public TokenDao(EntityManager em) {
        super(em);

        this.em = em;
    }

    /**
     * Create and persist an {@link AceToken} for a {@link Bag} given its id.
     *
     * @param principal the principal of the user
     * @param bagId     the id of the {@link Bag}
     * @param model     the {@link AceTokenCreate} with the information about the token
     * @return a {@link ResponseEntity} with the created {@link AceToken} as its body
     * possible responses are:
     * HTTP 201 - The AceToken was created successfully
     * HTTP 400 - If the request is not valid
     * HTTP 403 - If the user is not authorized to update the Bag
     * HTTP 409 - If an AceToken is already in place for the BagFile
     */
    public ResponseEntity<AceToken> createToken(Principal principal,
                                                Long bagId,
                                                AceTokenCreate model) {
        ResponseEntity<AceToken> response;
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Bag bag = queryFactory.from(QBag.bag)
                .where(QBag.bag.id.eq(bagId))
                .select(QBag.bag)
                .fetchOne();

        BagFile bagFile = queryFactory.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.filename.eq(model.getFilename())
                        .and(QBagFile.bagFile.bag.id.eq(bagId)))
                .fetchOne();

        Long tokenId = queryFactory.select(QAceToken.aceToken.id)
                .from(QAceToken.aceToken)
                .where(QAceToken.aceToken.bag.id.eq(bagId)
                        .and(QAceToken.aceToken.file.filename.eq(model.getFilename())))
                .fetchCount();

        //noinspection Duplicates
        if (bag == null || bagFile == null) {
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } else if (!authorized(principal, bag)) {
            response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else if (tokenId > 0) {
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
        } else {
            AceToken token = new AceToken(
                    model.getProof(),
                    model.getRound(),
                    model.getImsService(),
                    model.getAlgorithm(),
                    model.getImsHost(),
                    Date.from(model.getCreateDate().toInstant()),
                    bagFile);
            bagFile.setToken(token);
            token.setBag(bag);
            save(bag);
            response = ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(token);
        }

        return response;
    }

    /**
     * Helper to check if a user is authorized to update a resource.
     * <p>
     * Not really fond of this implementation.
     *
     * @param principal the principal of the user
     * @param bag       the bag being modified
     * @return true if the user can update the bag; false otherwise
     */
    private boolean authorized(Principal principal, Bag bag) {
        return hasRoleAdmin() || bag.getCreator().equalsIgnoreCase(principal.getName());
    }
}

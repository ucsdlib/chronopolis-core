package org.chronopolis.ingest.repository.dao;

import com.querydsl.core.QueryModifiers;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.tokens.TokenWriter;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.models.create.AceTokenCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Date;
import java.util.List;

/**
 * Data accessor for AceTokens. Really just to move the logic for creating a token from a
 * {@link AceTokenCreate} out of the BagTokenController.
 * <p>
 * Can we make this a {@link org.springframework.stereotype.Component} to avoid needed to create the
 * Bean ourselves?
 *
 * @author shake
 */
public class TokenDao extends PagedDao {
    private final Logger log = LoggerFactory.getLogger(TokenDao.class);
    private final EntityManager em;

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
     * @return see {@link #createToken(Principal, Bag, BagFile, AceTokenCreate)}
     */
    public ResponseEntity<AceToken> createToken(Principal principal,
                                                Long bagId,
                                                AceTokenCreate model) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Bag bag = queryFactory.from(QBag.bag)
                .where(QBag.bag.id.eq(bagId))
                .select(QBag.bag)
                .fetchOne();

        BagFile bagFile = queryFactory.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.filename.eq(model.getFilename())
                        .and(QBagFile.bagFile.bag.id.eq(bagId)))
                .fetchOne();

        return createToken(principal, bag, bagFile, model);
    }

    /**
     * Boop
     *
     * @param principal the security principal of the user
     * @param bagId     the id of the {@link Bag}
     * @param fileId    the id of the {@link BagFile}
     * @param model     the {@link AceTokenCreate} with the information about the token
     * @return See {@link #createToken(Principal, Bag, BagFile, AceTokenCreate)}
     */
    public ResponseEntity<AceToken> createToken(Principal principal,
                                                Long bagId,
                                                Long fileId,
                                                AceTokenCreate model) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Bag bag = queryFactory.from(QBag.bag)
                .where(QBag.bag.id.eq(bagId))
                .select(QBag.bag)
                .fetchOne();

        BagFile bagFile = queryFactory.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.id.eq(fileId)
                        .and(QBagFile.bagFile.filename.eq(model.getFilename()))
                        // make sure the file is part of the bag
                        .and(QBagFile.bagFile.bag.id.eq(bagId)))
                .fetchOne();

        return createToken(principal, bag, bagFile, model);
    }

    /**
     * Create and persist an {@link AceToken} given a {@link Bag} and {@link BagFile}
     *
     * @param principal the principal of the user
     * @param bag       the {@link Bag} which the {@link BagFile} belongs to
     * @param file      the {@link BagFile} to associate with the {@link AceToken}
     * @param model     the {@link AceTokenCreate} with the information about the token
     * @return a {@link ResponseEntity} with the created {@link AceToken} as its body
     *         possible responses are:
     *         HTTP 201 - The AceToken was created successfully
     *         HTTP 400 - If the request is not valid
     *         HTTP 403 - If the user is not authorized to update the Bag
     *         HTTP 409 - If an AceToken is already in place for the BagFile
     */
    public ResponseEntity<AceToken> createToken(Principal principal,
                                                Bag bag,
                                                BagFile file,
                                                AceTokenCreate model) {
        ResponseEntity<AceToken> response;
        //noinspection Duplicates
        if (bag == null || file == null) {
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } else if (!authorized(principal, bag)) {
            response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else if (file.getToken() != null) {
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
        } else {
            AceToken token = new AceToken(
                    model.getProof(),
                    model.getRound(),
                    model.getImsService(),
                    model.getAlgorithm(),
                    model.getImsHost(),
                    Date.from(model.getCreateDate().toInstant()),
                    bag,
                    file);
            file.setToken(token);
            save(bag);
            response = ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(token);
        }

        return response;
    }

    /**
     * Write the {@link AceToken}s belonging to a {@link Bag} to an {@link OutputStream}
     *
     * @param id the id of the {@link Bag} to write {@link AceToken}s for
     * @param stream the {@link OutputStream} to write to
     */
    public void writeToStream(Long id, OutputStream stream) {
        QBagFile bagFile = QBagFile.bagFile;
        QAceToken aceToken = QAceToken.aceToken;
        long offset;
        long page = 0;
        long limit = 10000;
        boolean next = true;

        JPAQueryFactory qf = getJPAQueryFactory();
        try (TokenWriter writer = new TokenWriter(stream)) {
            while (next) {
                offset = page * limit;
                QueryModifiers queryModifiers = new QueryModifiers(limit, offset);

                List<org.chronopolis.rest.entities.projections.AceToken> result = qf.from(aceToken)
                        .innerJoin(aceToken.file, bagFile)
                        .where(aceToken.bag.id.eq(id))
                        .orderBy(aceToken.id.asc())
                        .restrict(queryModifiers)
                        .transform(GroupBy.groupBy(aceToken.id).list(Projections.constructor(
                                org.chronopolis.rest.entities.projections.AceToken.class,
                                aceToken.id,
                                aceToken.bag.id,
                                aceToken.round,
                                aceToken.imsHost,
                                aceToken.imsService,
                                aceToken.algorithm,
                                aceToken.proof,
                                aceToken.createDate,
                                bagFile.filename
                        )));

                for (org.chronopolis.rest.entities.projections.AceToken token : result) {
                    writer.startProjection(token);
                    writer.writeTokenEntry();
                }

                next = result.size() == limit;
                if (next) {
                    ++page;
                }
            }
        } catch (IOException e) {
            log.error("Failed to write tokens to output stream", e);
        }
    }

}

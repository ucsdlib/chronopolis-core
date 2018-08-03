package org.chronopolis.ingest.tokens;

import com.google.common.collect.ImmutableMap;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.serializers.ExtensionsKt;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * todo: old BagModel -> new Bag when tokenizer is updated
 *
 */
@DataJpaTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
                scripts = "classpath:sql/createBagsWithTokens.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
                scripts = "classpath:sql/deleteBagsWithTokens.sql")
})
public class IngestTokenRegistrarTest extends IngestTest {

    @Autowired
    private EntityManager entityManager;

    private PagedDAO dao;
    private TokenWorkSupervisor tws;
    private IngestTokenRegistrar registrar;

    private final String TOKEN_FORMAT = "(%s,%s)::%s";
    private final String FILENAME = "/manifest-sha256.txt";
    private final String DEPOSITOR = "test-depositor";
    private final String BAG_ONE_NAME = "new-bag-1";
    private final String BAG_THREE_NAME = "new-bag-3";
    // From sql
    // private Bag bagOne = new Bag().setName(BAG_ONE_NAME).setDepositor(DEPOSITOR);
    // private Bag bagThree = new Bag().setName(BAG_THREE_NAME).setDepositor(DEPOSITOR);
    private XMLGregorianCalendar xmlCal;

    @Before
    public void setup() throws DatatypeConfigurationException {
        tws = mock(TokenWorkSupervisor.class);
        dao = new PagedDAO(entityManager);
        registrar = new IngestTokenRegistrar(dao, tws);

        GregorianCalendar gc = GregorianCalendar.from(ZonedDateTime.now());
        xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
    }

    private Long getId(final String name) {
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        return factory.from(QBag.bag)
                .select(QBag.bag.id)
                .where(QBag.bag.depositor.namespace.eq(DEPOSITOR).and(QBag.bag.name.eq(name)))
                .fetchOne();
    }

    private Bag getBag(final String name) {
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        return factory.selectFrom(QBag.bag)
                .where(QBag.bag.depositor.namespace.eq(DEPOSITOR).and(QBag.bag.name.eq(name)))
                .fetchOne();
    }

    @Test
    public void saveToken() {
        Bag bag = getBag(BAG_ONE_NAME);
        ManifestEntry entry = new ManifestEntry(ExtensionsKt.model(bag), FILENAME, "digest");
        TokenResponse response = new TokenResponse();
        response.setRoundId(1L);
        response.setTimestamp(xmlCal);
        response.setDigestService("SHA-256");
        response.setDigestProvider("SHA-256");
        response.setTokenClassName("SHA-256");
        response.setTimestamp(new XMLGregorianCalendarImpl());
        response.setName(String.format(TOKEN_FORMAT, DEPOSITOR, BAG_ONE_NAME, FILENAME));
        ImmutableMap<ManifestEntry, TokenResponse> map = ImmutableMap.of(entry, response);
        registrar.register(map);

        verify(tws, times(1)).complete(eq(entry));
        Assert.assertEquals(1, tokenCount(bag.getId()));
    }

    @Test
    public void saveTokenConflict() {
        Bag bag = getBag(BAG_THREE_NAME);
        ManifestEntry entry = new ManifestEntry(ExtensionsKt.model(bag), FILENAME, "digest");
        TokenResponse response = new TokenResponse();
        response.setRoundId(1L);
        response.setTimestamp(xmlCal);
        response.setDigestService("SHA-256");
        response.setDigestProvider("SHA-256");
        response.setTokenClassName("SHA-256");
        response.setTimestamp(new XMLGregorianCalendarImpl());
        response.setName(String.format(TOKEN_FORMAT, DEPOSITOR, BAG_ONE_NAME, FILENAME));
        ImmutableMap<ManifestEntry, TokenResponse> map = ImmutableMap.of(entry, response);
        registrar.register(map);

        verify(tws, times(1)).complete(eq(entry));

        Assert.assertEquals(1, tokenCount(bag.getId()));
        AceToken token = dao.findOne(QAceToken.aceToken, QAceToken.aceToken.filename.eq(FILENAME)
                .and(QAceToken.aceToken.bag.id.eq(bag.getId())));

        // check that the token was not overwritten
        Assert.assertNotNull(token);
        Assert.assertEquals("test-proof-hw", token.getProof());
    }

    @Test
    public void saveTokenBagNotExists() {
        long id = -100L;
        Bag bag = getBag(BAG_ONE_NAME);
        // not the prettiest but we can't update the Bag entity or else we get a hibernate exception
        org.chronopolis.rest.models.Bag model = ExtensionsKt.model(bag);
        org.chronopolis.rest.models.Bag invalid = model.copy(
                -100L, model.getSize(), model.getTotalFiles(), model.getBagStorage(),
                model.getTokenStorage(), model.getCreatedAt(), model.getUpdatedAt(),
                model.getName(), model.getCreator(), model.getDepositor(), model.getStatus(),
                model.getReplicatingNodes());
        ManifestEntry entry = new ManifestEntry(invalid, FILENAME, "digest");
        TokenResponse response = new TokenResponse();
        response.setRoundId(1L);
        response.setTimestamp(xmlCal);
        response.setDigestService("SHA-256");
        response.setDigestProvider("SHA-256");
        response.setTokenClassName("SHA-256");
        response.setTimestamp(new XMLGregorianCalendarImpl());
        response.setName(String.format(TOKEN_FORMAT, DEPOSITOR, BAG_ONE_NAME, FILENAME));
        ImmutableMap<ManifestEntry, TokenResponse> map = ImmutableMap.of(entry, response);
        registrar.register(map);

        verify(tws, times(1)).complete(eq(entry));
        Assert.assertEquals(0, tokenCount(id));
    }

    private long tokenCount(Long bagId) {
        QAceToken qAceToken = QAceToken.aceToken;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        return queryFactory.selectFrom(qAceToken)
                .where(qAceToken.bag.id.eq(bagId).and(qAceToken.filename.eq(FILENAME)))
                .fetchCount();
    }


}
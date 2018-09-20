package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.models.create.AceTokenCreate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.security.Principal;

import static com.google.common.collect.ImmutableSet.of;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

/**
 * Tests for {@link TokenDao}
 *
 * cliffers:
 * bag-1 has no tokens, ok to create
 * bag-3 has tokens for all files, can use for conflict
 * See R__helper_functions.sql for full details
 *
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/create.sql"),
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = "classpath:sql/delete.sql")
})
public class TokenDaoTest extends IngestTest {

    private final String filename = "/manifest-sha256.txt";
    private final Principal authorized = () -> "test-admin";
    private final Principal unauthorized = () -> "forbidden";
    private static UserDetails user = new User("test-admin", "test-admin", of(() -> "ROLE_USER"));

    @MockBean private SecurityContext context;
    @MockBean private Authentication authentication;
    @Autowired private EntityManager entityManager;

    @Before
    public void setup() {
        SecurityContextHolder.setContext(context);
        mockAuthenticated();
    }

    private void mockAuthenticated() {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
    }

    @Test
    public void createToken() {
        final String bagName = "bag-1";
        TokenDao dao = new TokenDao(entityManager);
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(bagName));

        AceTokenCreate create = new AceTokenCreate();
        create.setBagId(bag.getId());
        create.setFilename(filename);

        ResponseEntity<AceToken> response = dao.createToken(authorized, bag.getId(), create);
        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void createFailures() {
        final String bagName = "bag-3";
        final String invalidFile = "/file-does-not-exist";
        TokenDao dao = new TokenDao(entityManager);
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(bagName));

        AceTokenCreate create = new AceTokenCreate();
        create.setBagId(bag.getId());
        create.setFilename(filename);

        ResponseEntity<AceToken> response = dao.createToken(authorized, bag.getId(), create);
        Assert.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        response = dao.createToken(unauthorized, bag.getId(), create);
        Assert.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        create.setFilename(invalidFile);
        response = dao.createToken(authorized, bag.getId(), create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        create.setBagId(Long.MAX_VALUE);
        response = dao.createToken(authorized, Long.MAX_VALUE, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // and once for file id is part of a different bag
        JPAQueryFactory qf = dao.getJPAQueryFactory();
        BagFile otherBagsFile = qf.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.bag.name.eq("bag-2"))
                .fetchFirst();
        create.setBagId(bag.getId());
        create.setFilename(otherBagsFile.getFilename());
        response = dao.createToken(authorized, bag.getId(), otherBagsFile.getId(), create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    }
}
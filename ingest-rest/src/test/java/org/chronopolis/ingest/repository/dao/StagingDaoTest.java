package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.create.StagingCreate;
import org.chronopolis.rest.models.enums.StorageUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
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
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static org.chronopolis.ingest.JpaContext.CREATE_SCRIPT;
import static org.chronopolis.ingest.JpaContext.DELETE_SCRIPT;
import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_BAG;
import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_TOKEN;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

/**
 * Tests for {@link StagingDao}
 *
 * @author shake
 */
@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class)
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = CREATE_SCRIPT),
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
})
public class StagingDaoTest extends IngestTest {

    private final Principal authorized = () -> "test-admin";
    private final Principal unauthorized = () -> "forbidden";
    private final UserDetails user = new User("test-admin", "test-admin", of(() -> "ROLE_USER"));

    @MockBean
    private SecurityContext context;
    @MockBean
    private Authentication authentication;

    @Autowired
    private EntityManager entityManager;

    @Before
    public void setup() {
        SecurityContextHolder.setContext(context);
        mockAuthenticated();
    }

    // an unfortunate side-effect of spring handling auth
    // ideally auth will be something which
    // 1) is not a static method
    // 2) is more open to us in our application (on a user entity fetch from the db, for example)
    private void mockAuthenticated() {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
    }

    /**
     * All the Bags which we create contain an active BAG Staging Resource so...
     * 1: Fetch the active storage
     * 2: Set it inactive
     * 3: Issue request for creating a new Storage Resource
     */
    @Test
    public void createStaging() {
        // setup first
        final String name = "bag-1";
        final String tagmanifest = "/tagmanifest-sha256.txt";
        StagingDao dao = new StagingDao(entityManager);
        JPAQueryFactory jpaQueryFactory = dao.getJPAQueryFactory();

        StorageRegion storageRegion = jpaQueryFactory.selectFrom(QStorageRegion.storageRegion)
                .fetchFirst();
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(name));
        Depositor depositor = bag.getDepositor();

        Assert.assertNotNull(bag);
        Assert.assertNotNull(depositor);
        Assert.assertNotNull(storageRegion);

        // fetch and set inactive
        Optional<StagingStorage> storageFetch =
                dao.activeStorageForBag(bag, DISCRIMINATOR_BAG);

        Assert.assertTrue(storageFetch.isPresent());
        StagingStorage storage = storageFetch.get();
        storage.setActive(false);
        dao.save(storage);

        // new create
        StagingCreate create = new StagingCreate(depositor.getNamespace() + "/" + name,
                tagmanifest,
                storageRegion.getId(),
                3L, 30L, StorageUnit.B);

        ResponseEntity<StagingStorage> success =
                dao.createStaging(authorized, bag.getId(), DISCRIMINATOR_BAG, create);

        Assert.assertEquals(HttpStatus.CREATED, success.getStatusCode());
        Assert.assertNotNull(success.getBody());
    }

    @Test
    public void testCreateFail() {
        StagingCreate create;
        ResponseEntity<StagingStorage> response;
        final String name = "bag-1";
        final String fileDne = "/does-not-exist";
        final String tagmanifest = "/tagmanifest-sha256.txt";

        StagingDao dao = new StagingDao(entityManager);
        JPAQueryFactory jpaQueryFactory = dao.getJPAQueryFactory();

        StorageRegion storageRegion = jpaQueryFactory.selectFrom(QStorageRegion.storageRegion)
                .fetchFirst();
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(name));
        Depositor depositor = bag.getDepositor();

        Assert.assertNotNull(bag);
        Assert.assertNotNull(depositor);
        Assert.assertNotNull(storageRegion);

        // null storage region -> bad request
        create = new StagingCreate(depositor.getNamespace() + "/" + name,
                tagmanifest, Long.MAX_VALUE, 3L, 30L, StorageUnit.B);
        response = dao.createStaging(authorized, bag.getId(), DISCRIMINATOR_BAG, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // null bag -> bad request
        create = new StagingCreate(depositor.getNamespace() + "/" + name,
                tagmanifest, storageRegion.getId(), 3L, 30L, StorageUnit.B);
        response = dao.createStaging(authorized, Long.MAX_VALUE, DISCRIMINATOR_BAG, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // unauthorized -> expect forbidden
        response = dao.createStaging(unauthorized, bag.getId(), DISCRIMINATOR_BAG, create);
        Assert.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        // everything valid, but already have staging -> conflict
        response = dao.createStaging(authorized, bag.getId(), DISCRIMINATOR_BAG, create);
        Assert.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        // invalid file -> bad request
        create = new StagingCreate(depositor.getNamespace(),
                fileDne, storageRegion.getId(), 3L, 30L, StorageUnit.B);
        response = dao.createStaging(authorized, bag.getId(), DISCRIMINATOR_TOKEN, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
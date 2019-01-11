package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.support.QueryResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.models.create.FileCreate;
import org.chronopolis.rest.models.create.FixityCreate;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
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

import static com.google.common.collect.ImmutableList.of;
import static org.chronopolis.ingest.JpaContext.CREATE_SCRIPT;
import static org.chronopolis.ingest.JpaContext.DELETE_SCRIPT;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

/**
 * Tests for {@link BagFileDao}
 *
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = CREATE_SCRIPT),
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
})
public class BagFileDaoTest extends IngestTest {

    private Long id = null;
    private final Long size = 1L;
    private final Principal authorized = () -> "test-admin";
    private final Principal unauthorized = () -> "forbidden";
    private static final String FIXITY_VALUE =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final FixityAlgorithm FIXITY_ALGORITHM = FixityAlgorithm.SHA_256;

    private static UserDetails user = new User("test-admin", "test-admin", of(() -> "ROLE_USER"));

    @MockBean private SecurityContext context;
    @MockBean private Authentication authentication;

    @Autowired private EntityManager entityManager;


    private BagFileDao dao;

    @Before
    public void setup() {
        id = idFor();
        dao = new BagFileDao(entityManager);

        SecurityContextHolder.setContext(context);
        mockAuthenticated();
    }

    // an unfortunate side-effect of spring handling auth
    private void mockAuthenticated() {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
    }

    private Long idFor() {
        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        return qf.select(QBag.bag.id)
                .from(QBag.bag)
                .limit(1)
                .fetchOne();
    }

    private Long fileIdFor() {
        String filename = "/manifest-sha256.txt";
        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        return qf.select(QBagFile.bagFile.id)
                .from(QBagFile.bagFile)
                .where(QBagFile.bagFile.bag.id.eq(id).and(QBagFile.bagFile.filename.eq(filename)))
                .fetchOne();
    }

    @Test
    public void createBagFile() {
        String filename = "/test-create-ok";
        FileCreate create = new FileCreate(filename, size, FIXITY_VALUE, FIXITY_ALGORITHM, id);
        QueryResult<BagFile> bagFile = dao.createBagFile(authorized, create);

        Assert.assertEquals(HttpStatus.CREATED, bagFile.response().getStatusCode());
        Assert.assertTrue(bagFile.get().isPresent());
        Assert.assertNotEquals(0, bagFile.get().get().getId());

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        BagFile bf = qf.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.bag.id.eq(id).and(QBagFile.bagFile.filename.eq(filename)))
                .fetchOne();

        Assert.assertNotNull(bf);
        Assert.assertEquals(bf, bagFile.get().get());
    }

    @Test
    public void createBagFileBadRequest() {
        // we have two bad request paths to check
        // null bag_id
        String filename = "/test-create-bad-request";
        FileCreate create = new FileCreate(filename, size, FIXITY_VALUE, FIXITY_ALGORITHM, null);
        QueryResult<BagFile> bagFile = dao.createBagFile(authorized, create);
        // could also check the error if we wanted
        Assert.assertEquals(HttpStatus.BAD_REQUEST, bagFile.response().getStatusCode());
        Assert.assertFalse(bagFile.get().isPresent());

        // invalid bag
        create.setBag(Long.MAX_VALUE);
        bagFile = dao.createBagFile(authorized, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, bagFile.response().getStatusCode());
        Assert.assertFalse(bagFile.get().isPresent());
    }

    @Test
    public void createBagFileForbidden() {
        String filename = "/test-create-forbidden";
        FileCreate create = new FileCreate(filename, size, FIXITY_VALUE, FIXITY_ALGORITHM, id);
        QueryResult<BagFile> bagFile = dao.createBagFile(unauthorized, create);

        Assert.assertEquals(HttpStatus.FORBIDDEN, bagFile.response().getStatusCode());
        Assert.assertFalse(bagFile.get().isPresent());
    }


    @Test
    public void createBagFileConflict() {
        // a known file from our init sql function
        String filename = "/manifest-sha256.txt";
        FileCreate create = new FileCreate(filename, size, FIXITY_VALUE, FIXITY_ALGORITHM, id);
        QueryResult<BagFile> bagFile = dao.createBagFile(authorized, create);

        Assert.assertEquals(HttpStatus.CONFLICT, bagFile.response().getStatusCode());
        Assert.assertFalse(bagFile.get().isPresent());
    }

    @Test
    public void createFixity() {
        // create a bag file with no fixity first
        Bag bag = entityManager.find(Bag.class, id);

        BagFile bf = new BagFile();
        bf.setBag(bag);
        bf.setSize(100L);
        bf.setFilename("/test-create-fixity");
        entityManager.persist(bf);
        entityManager.refresh(bf);

        FixityCreate fixityCreate = new FixityCreate(FIXITY_ALGORITHM, FIXITY_VALUE);
        QueryResult<Fixity> fixity = dao.createFixity(authorized, id, bf.getId(), fixityCreate);

        Assert.assertEquals(HttpStatus.CREATED, fixity.response().getStatusCode());
        Assert.assertTrue(fixity.get().isPresent());
    }

    @Test
    public void createFixityBadRequest() {
        // once again a few paths to check
        // unsupported fixity algorithm
        FixityCreate create = new FixityCreate(FixityAlgorithm.UNSUPPORTED, FIXITY_VALUE);
        QueryResult<Fixity> fixity = dao.createFixity(authorized, id, id, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());

        // invalid bag
        create.setAlgorithm(FIXITY_ALGORITHM);
        dao.createFixity(authorized, Long.MAX_VALUE, id, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());

        // invalid file
        dao.createFixity(authorized, id, Long.MAX_VALUE, create);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());
    }

    @Test
    public void createFixityForbidden() {
        Long fileId = fileIdFor();
        FixityCreate create = new FixityCreate(FIXITY_ALGORITHM, FIXITY_VALUE);
        QueryResult<Fixity> fixity = dao.createFixity(unauthorized, id, fileId, create);
        Assert.assertEquals(HttpStatus.FORBIDDEN, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());
    }

    @Test
    public void createFixityConflict() {
        Long fileId = fileIdFor();
        FixityCreate create = new FixityCreate(FIXITY_ALGORITHM, FIXITY_VALUE);
        QueryResult<Fixity> fixity = dao.createFixity(authorized, id, fileId, create);
        Assert.assertEquals(HttpStatus.CONFLICT, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());
    }

    @Test
    public void fixityFor() {
        Long fileId = fileIdFor();
        QueryResult<Fixity> fixity = dao.fixityFor(id, fileId, FIXITY_ALGORITHM.getCanonical());
        Assert.assertEquals(HttpStatus.OK, fixity.response().getStatusCode());
        Assert.assertTrue(fixity.get().isPresent());
    }

    @Test
    public void fixityForBadRequest() {
        // two paths
        Long fileId = fileIdFor();
        // invalid bag_id
        QueryResult<Fixity> fixity =
                dao.fixityFor(Long.MAX_VALUE, fileId, FIXITY_ALGORITHM.getCanonical());
        Assert.assertEquals(HttpStatus.BAD_REQUEST, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());

        // invalid file_id
        fixity = dao.fixityFor(id, Long.MAX_VALUE, FIXITY_ALGORITHM.getCanonical());
        Assert.assertEquals(HttpStatus.BAD_REQUEST, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());
    }

    @Test
    public void fixityForNotFound() {
        Long fileId = fileIdFor();
        QueryResult<Fixity> fixity =
                dao.fixityFor(id, fileId, FixityAlgorithm.UNSUPPORTED.getCanonical());
        Assert.assertEquals(HttpStatus.NOT_FOUND, fixity.response().getStatusCode());
        Assert.assertFalse(fixity.get().isPresent());
    }
}
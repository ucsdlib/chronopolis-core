package org.chronopolis.ingest.task;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.function.Predicate;

import static org.chronopolis.ingest.JpaContext.CREATE_SCRIPT;
import static org.chronopolis.ingest.JpaContext.DELETE_SCRIPT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = CREATE_SCRIPT),
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
})
public class LocalTokenizationTest extends IngestTest {

    private static final Long ID = 1L;
    private static final String USERNAME = "test-admin";
    private final Collection<Predicate<ManifestEntry>> predicates = ImmutableList.of();

    @Mock private TokenWorkSupervisor tws;
    @Mock private TrackingThreadPoolExecutor<Bag> executor;

    @Autowired private EntityManager entityManager;
    private LocalTokenization localTokenization;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PagedDao dao = new PagedDao(entityManager);
        IngestProperties properties = new IngestProperties();
        properties.getTokenizer().setEnabled(true);
        properties.getTokenizer().setUsername(USERNAME);
        properties.getTokenizer().setStaging(new Posix().setId(ID));

        localTokenization = new LocalTokenization(dao, tws,
                properties, executor, predicates);
    }

    @Test
    public void runLocalTokenization() {
        localTokenization.searchForBags();

        Mockito.verify(executor, times(1)).submitIfAvailable(any(), any());
    }

}

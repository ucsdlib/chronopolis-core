package org.chronopolis.ingest.task;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.api.IngestAPIProperties;
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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.function.Predicate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
                scripts = "classpath:sql/createBags.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
                scripts = "classpath:sql/deleteBags.sql")
})
public class LocalTokenizationTest extends IngestTest {

    private static final Long ID = 1L;
    private static final String USERNAME = "admin";
    private final Collection<Predicate<ManifestEntry>> predicates = ImmutableList.of();

    @Mock private TokenWorkSupervisor tws;
    @Mock private TrackingThreadPoolExecutor<Bag> executor;

    @Autowired private EntityManager entityManager;
    private LocalTokenization localTokenization;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PagedDAO dao = new PagedDAO(entityManager);
        BagStagingProperties properties =
                new BagStagingProperties().setPosix(new Posix().setId(ID));
        IngestAPIProperties apiProperties = new IngestAPIProperties().setUsername(USERNAME);

        localTokenization = new LocalTokenization(dao, tws, apiProperties, properties, executor, predicates);
    }

    @Test
    public void runLocalTokenization() {
        localTokenization.searchForBags();

        Mockito.verify(executor, times(9)).submitIfAvailable(any(), any());
    }

}
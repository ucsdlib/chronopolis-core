package org.chronopolis.tokenize;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Fixity;
import org.chronopolis.rest.models.StagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.chronopolis.tokenize.filter.HttpFilter;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BagProcessorTest {

    private static final String HW_NAME = "/data/hello_world";
    private static final String HW_DIGEST =
            "a948904f2f0f479b8f8197694b30184b0d2ed1c1cd2a1ec0fb85d299a192a447";

    public static final String EMPTY_FIXITY =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final String DEPOSITOR = "test-depositor";
    private static final String COLLECTION = "test-bag-1";

    @Mock private HttpFilter filter;
    @Mock private TokenWorkSupervisor supervisor;
    // if we have the files on disk, is there any reason to mock this?
    @Mock private BagProcessor.Digester digester;

    private Bag bag;
    private BagProcessor processor;
    private BagStagingProperties properties;
    private Collection<Predicate<ManifestEntry>> predicates;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        predicates = ImmutableList.of(filter);

        // setup our test bag to read from
        URL bags = ClassLoader.getSystemClassLoader().getResource("bags");
        properties = new BagStagingProperties()
                .setPosix(new Posix().setPath(bags.getPath()));

        Fixity fixity = new Fixity(EMPTY_FIXITY, FixityAlgorithm.SHA_256, ZonedDateTime.now());
        HashSet<Fixity> fixities = new HashSet<>();
        fixities.add(fixity);
        StagingStorage bagStorage = new StagingStorage(true, 1L, 1L, 1L,
                DEPOSITOR + "/" + COLLECTION, fixities);
        bag = new Bag(1L, 1L, 1L, bagStorage, bagStorage, ZonedDateTime.now(), ZonedDateTime.now(),
                COLLECTION, DEPOSITOR, DEPOSITOR, BagStatus.DEPOSITED, new HashSet<>());

    }

    @Test
    public void runAll() {
        processor = new BagProcessor(bag, predicates, properties, supervisor);
        when(filter.test(any(ManifestEntry.class))).thenReturn(true);
        processor.run();
        verify(filter, times(3)).test(any(ManifestEntry.class));
        verify(supervisor, times(3)).start(any(ManifestEntry.class));
    }

    @Test
    public void runManifestNotValid() {
        processor = new BagProcessor(bag, predicates, properties, digester, supervisor);
        ManifestEntry hwEntry = new ManifestEntry(bag, HW_NAME, HW_DIGEST);
        when(filter.test(eq(hwEntry))).thenReturn(true);
        when(digester.digest(eq(HW_NAME))).thenReturn(Optional.of(HW_DIGEST + "-bad"));
        processor.run();
        verify(filter, times(1)).test(eq(hwEntry));
        verify(digester, times(1)).digest(eq(HW_NAME));
        verify(supervisor, times(0)).start(any(ManifestEntry.class));
    }

}
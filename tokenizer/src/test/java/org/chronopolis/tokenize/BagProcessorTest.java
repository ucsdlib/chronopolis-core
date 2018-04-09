package org.chronopolis.tokenize;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import org.chronopolis.tokenize.filter.HttpFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BagProcessorTest {

    private static final String HW_NAME = "data/hello_world";
    private static final String HW_DIGEST = "a948904f2f0f479b8f8197694b30184b0d2ed1c1cd2a1ec0fb85d299a192a447";

    private final String depositor = "test-depositor";
    private final String collection = "test-bag-1";

    @Mock private HttpFilter filter;
    @Mock private StateMachine stateMachine;
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

        StagingStorageModel bagStorage = new StagingStorageModel()
                .setPath(depositor + "/" + collection);
        bag = new Bag()
                .setId(1L)
                .setName(collection)
                .setDepositor(depositor)
                .setBagStorage(bagStorage);

    }

    @Test
    public void runAll() {
        processor = new BagProcessor(bag, predicates, properties, stateMachine);
        when(filter.test(any(ManifestEntry.class))).thenReturn(true);
        processor.run();
        verify(filter, times(3)).test(any(ManifestEntry.class));
        verify(stateMachine, times(3)).start(any(ManifestEntry.class));
    }

    @Test
    public void runManifestNotValid() {
        processor = new BagProcessor(bag, predicates, properties, digester, stateMachine);
        ManifestEntry hwEntry = new ManifestEntry(bag, HW_NAME, HW_DIGEST);
        when(filter.test(eq(hwEntry))).thenReturn(true);
        when(digester.digest(eq(HW_NAME))).thenReturn(Optional.of(HW_DIGEST + "-bad"));
        processor.run();
        verify(filter, times(1)).test(eq(hwEntry));
        verify(digester, times(1)).digest(eq(HW_NAME));
        verify(stateMachine, times(0)).start(any(ManifestEntry.class));
    }

}
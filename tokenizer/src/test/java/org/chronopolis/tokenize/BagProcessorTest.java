package org.chronopolis.tokenize;

import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.util.Filter;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BagProcessorTest {

    private static final String HW_NAME = "data/hello_world";
    private static final String HW_DIGEST = "a948904f2f0f479b8f8197694b30184b0d2ed1c1cd2a1ec0fb85d299a192a447";

    private final String depositor = "test-depositor";
    private final String collection = "test-bag-1";

    @Mock private Filter<String> filter;
    // if we have the files on disk, is there any reason to mock this?
    @Mock private BagProcessor.Digester digester;
    @Mock private ChronopolisTokenRequestBatch batch;

    private Bag bag;
    private BagProcessor processor;
    private BagStagingProperties properties;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

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
        processor = new BagProcessor(bag, filter, properties, batch);
        when(filter.contains(eq(HW_NAME))).thenReturn(false);
        processor.run();
        verify(filter, times(3)).contains(anyString());
        verify(batch, times(3)).add(any(ManifestEntry.class));
    }

    @Test
    public void runManifestNotValid() {
        processor = new BagProcessor(bag, filter, properties, batch, digester);
        ManifestEntry hwEntry = new ManifestEntry(bag, HW_NAME, HW_DIGEST);
        hwEntry.setCalculatedDigest(HW_DIGEST + "-bad");
        when(filter.contains(eq(HW_NAME))).thenReturn(false);
        when(digester.entryFrom(any(Bag.class), eq(HW_NAME), eq(HW_DIGEST))).thenReturn(hwEntry);
        processor.run();
        verify(filter, times(1)).contains(anyString());
        verify(digester, times(1)).entryFrom(any(Bag.class), anyString(), anyString());
        verify(batch, times(0)).add(any(ManifestEntry.class));
    }

}
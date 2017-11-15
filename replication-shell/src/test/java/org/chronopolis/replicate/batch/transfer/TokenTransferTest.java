package org.chronopolis.replicate.batch.transfer;

import com.google.common.io.ByteSource;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.OperationType;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.Replication;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class TokenTransferTest {

    @Mock private Bucket bucket;
    @Mock private FileTransfer ft;
    @Mock private ReplicationService replications;

    private StorageOperation op;
    private Replication replication;

    private TokenTransfer transfer;
    private Path EMPTY_PATH;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        EMPTY_PATH = Paths.get("");
        op = new StorageOperation()
                .setType(OperationType.NOP)
                .setSize(0L)
                .setLink("link")
                .setIdentifier("id")
                .setPath(Paths.get("path"));

        replication = new Replication()
                .setId(1L);

        transfer = new TokenTransfer(bucket, op, replication, replications);
    }

    @Test(expected = RuntimeException.class)
    public void failHash() throws Exception {
        // kind of sucks to mock all this... otherwise we can setup everything to what we should expect
        when(bucket.transfer(eq(op))).thenReturn(Optional.of(ft));
        when(ft.get()).thenReturn(EMPTY_PATH);
        when(ft.getOutput()).thenReturn(ByteSource.wrap("hello".getBytes()).openStream());
        when(bucket.hash(eq(op), eq(EMPTY_PATH))).thenReturn(Optional.empty());

        transfer.run();

        verify(bucket, times(1)).transfer(eq(op));
        verify(bucket, times(1)).hash(eq(op), eq(EMPTY_PATH));
        verify(ft, times(1)).get();
        verify(ft, times(1)).getOutput();
        verify(replications, times(0)).updateTokenStoreFixity(1L, any(FixityUpdate.class));
    }

}
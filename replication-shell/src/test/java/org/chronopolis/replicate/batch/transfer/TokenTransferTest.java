package org.chronopolis.replicate.batch.transfer;

import com.google.common.io.ByteSource;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.OperationType;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.FixityUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static java.time.ZonedDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class TokenTransferTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Path EMPTY_PATH = Paths.get("");
    private final Bucket bucket = mock(Bucket.class);
    private final FileTransfer ft = mock(FileTransfer.class);
    private final ReplicationService replications = mock(ReplicationService.class);

    private SingleFileOperation op;
    private TokenTransfer transfer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        op = new SingleFileOperation(Paths.get("path"));
        op.setType(OperationType.NOP);
        op.setSize(0L);
        op.setLink("link");
        op.setIdentifier("id");

        Bag bag = new Bag(1L, 1L, 1L, null, null, now(), now(), "test-bag", "token-transfer-test",
                "test-depositor", BagStatus.DEPOSITED, Collections.emptySet());
        Replication replication = new Replication(1L, now(), now(), ReplicationStatus.PENDING,
                "link", "link", "test-protocol", "", "", "test-node", bag
        );
        ReplicationProperties properties = new ReplicationProperties();

        transfer = new TokenTransfer(bucket, op, replication, replications, properties);
    }

    @Test
    public void failHash() throws Exception {
        // kind of sucks to mock all this... otherwise we can setup everything to what we should expect
        when(bucket.transfer(eq(op))).thenReturn(Optional.of(ft));
        when(ft.get()).thenReturn(EMPTY_PATH);
        when(ft.getOutput()).thenReturn(ByteSource.wrap("hello".getBytes()).openStream());
        when(bucket.hash(eq(op), eq(EMPTY_PATH))).thenReturn(Optional.empty());

        exception.expect(RuntimeException.class);
        transfer.run();

        verify(bucket, times(1)).transfer(eq(op));
        verify(bucket, times(1)).hash(eq(op), eq(EMPTY_PATH));
        verify(ft, times(1)).get();
        verify(ft, times(1)).getOutput();
        verify(replications, times(0)).updateTokenStoreFixity(1L, any(FixityUpdate.class));
    }

}
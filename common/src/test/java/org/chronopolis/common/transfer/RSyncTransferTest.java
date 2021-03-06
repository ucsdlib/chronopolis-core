package org.chronopolis.common.transfer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * Created by shake on 10/21/16.
 */
public class RSyncTransferTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Path local = Paths.get("");

    @Test
    public void lastRemote() {
        String link = "test@test.host:/path/to/folder";
        RSyncTransfer xfer = new RSyncTransfer(link, local);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test
    public void lastRemoteNoSlash() {
        String link = "test@test.host:folder";
        RSyncTransfer xfer = new RSyncTransfer(link, local);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test
    public void lastLocal() {
        String link = "/path/to/folder";
        RSyncTransfer xfer = new RSyncTransfer(link, local);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test
    public void lastLocalNoSlash() {
        String link = "folder";
        RSyncTransfer xfer = new RSyncTransfer(link, local);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test
    public void lastEmpty() {
        String link = "";
        RSyncTransfer xfer = new RSyncTransfer(link, local);
        exception.expect(IllegalArgumentException.class);
        xfer.last();
    }

    @Test
    public void lastRemoteEmpty() {
        String link = "test@test.host:";
        RSyncTransfer xfer = new RSyncTransfer(link, local);
        exception.expect(IllegalArgumentException.class);
        xfer.last();
    }

    @Test
    public void lastLocalEmpty() {
        String link = "";
        RSyncTransfer xfer = new RSyncTransfer(link, local);
        exception.expect(IllegalArgumentException.class);
        xfer.last();
    }

    @Test
    public void lastNull() {
        RSyncTransfer xfer = new RSyncTransfer(null, local);
        exception.expect(IllegalArgumentException.class);
        xfer.last();
    }

}
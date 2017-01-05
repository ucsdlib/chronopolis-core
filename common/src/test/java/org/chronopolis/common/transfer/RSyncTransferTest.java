package org.chronopolis.common.transfer;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * Created by shake on 10/21/16.
 */
public class RSyncTransferTest {

    @Test
    public void lastRemote() throws Exception {
        String link = "test@test.host:/path/to/folder";
        RSyncTransfer xfer = new RSyncTransfer(link);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test
    public void lastRemoteNoSlash() {
        String link = "test@test.host:folder";
        RSyncTransfer xfer = new RSyncTransfer(link);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test
    public void lastLocal() {
        String link = "/path/to/folder";
        RSyncTransfer xfer = new RSyncTransfer(link);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test
    public void lastLocalNoSlash() {
        String link = "folder";
        RSyncTransfer xfer = new RSyncTransfer(link);

        String last = xfer.last();
        Assert.assertEquals("folder", last);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lastEmpty() {
        String link = "";
        RSyncTransfer xfer = new RSyncTransfer(link);
        String last = xfer.last();
    }

    @Test(expected = IllegalArgumentException.class)
    public void lastRemoteEmpty() {
        String link = "test@test.host:";
        RSyncTransfer xfer = new RSyncTransfer(link);
        String last = xfer.last();
    }

    @Test(expected = IllegalArgumentException.class)
    public void lastLocalEmpty() {
        String link = "";
        RSyncTransfer xfer = new RSyncTransfer(link);
        String last = xfer.last();
    }

    @Test(expected = IllegalArgumentException.class)
    public void lastNull() {
        RSyncTransfer xfer = new RSyncTransfer(null);
        String last = xfer.last();
    }

}
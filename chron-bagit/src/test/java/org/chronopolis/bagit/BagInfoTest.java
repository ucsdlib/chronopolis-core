/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.chronopolis.bagit.util.TagMetaElement;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.chronopolis.bagit.TestUtil.createReader;
import org.chronopolis.bagit.util.PayloadOxum;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author shake
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BagInfoProcessor.class)
public class BagInfoTest {
    BagInfoProcessor bagInfoProcessor;
    private final String bagSizeRE = "Bag-Size";
    private final String baggingDateRE = "Bagging-Date";
    private final String oxumRE = "Payload-Oxum";
    private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
    private Path bagInfoPath;
    private FileSystem mockfs;
    private File mockFile;
    private PayloadOxum mockPayload;

    @Before 
    public void setUp() {
        bagInfoPath = PowerMock.createMock(Path.class);
        mockfs = PowerMock.createMock(FileSystem.class);
        mockFile = PowerMock.createMock(File.class);
        //mockPayload = PowerMock.createMock(PayloadOxum.class);
    }

    private void setupExpects(BufferedReader reader) throws IOException {
        EasyMock.expect(bagInfoPath.resolve("bag-info.txt")).andReturn(bagInfoPath);
        EasyMock.expect(bagInfoPath.toFile()).andReturn(mockFile).anyTimes();
        EasyMock.expect(mockFile.exists()).andReturn(Boolean.TRUE).anyTimes();
        EasyMock.expect(bagInfoPath.getFileSystem()).andReturn(mockfs).anyTimes();
        PowerMock.mockStatic(Files.class);
        EasyMock.expect(Files.newBufferedReader(bagInfoPath, 
                                                Charset.forName("UTF-8")))
                                                .andReturn(reader);
        
        EasyMock.expect(bagInfoPath.getParent()).andReturn(bagInfoPath);
        EasyMock.expect(bagInfoPath.resolve("data")).andReturn(bagInfoPath);
    }

    private void replayMocks() {
        PowerMock.replay(bagInfoPath, mockFile, Files.class);
    }
    
    @Test
    public void testInit() throws IOException {
        String date = dateFormat.format(new Date());
        TagMetaElement size = new TagMetaElement(bagSizeRE, "1 M", true);
        TagMetaElement bagDate = new TagMetaElement(baggingDateRE, 
                                                    date,
                                                    true);
        TagMetaElement oxum = new TagMetaElement(oxumRE, "1048576.1", true);
        BufferedReader reader = createReader(size, bagDate, oxum);
        setupExpects(reader);
        replayMocks();
        bagInfoProcessor = new BagInfoProcessor(bagInfoPath);
         
        Assert.assertEquals(1, bagInfoProcessor.getPayloadOxum().getNumFiles());
        Assert.assertEquals(1048576, 
                            bagInfoProcessor.getPayloadOxum().getOctetCount());
        Assert.assertEquals(bagSizeRE, bagInfoProcessor.getBagSize().getKey());
        Assert.assertEquals("1 M", bagInfoProcessor.getBagSize().getValue());
        Assert.assertEquals(baggingDateRE, 
                            bagInfoProcessor.getBaggingDate().getKey());
        Assert.assertEquals(date, bagInfoProcessor.getBaggingDate().getValue());
    }

    

    @Test
    public void testValid() throws IOException, Exception {
        long octet = 1048576;
        long files = 1;
        TagMetaElement oxum = new TagMetaElement(oxumRE, "1048576.1", true);
        BufferedReader reader = createReader(oxum);
        setupExpects(reader);
        EasyMock.expect(bagInfoPath.getParent()).andReturn(bagInfoPath);
        EasyMock.expect(bagInfoPath.resolve("data")).andReturn(bagInfoPath);
        replayMocks();
        bagInfoProcessor = new BagInfoProcessor(bagInfoPath);
        
        // We already know the parsing works, and in order to validate we walk
        // the file tree. By setting the mock, we'll be able to simple expect
        // the call
        mockPayload = PowerMock.createMockAndExpectNew(PayloadOxum.class);
        mockPayload.calculateOxum(bagInfoPath);
        PowerMock.expectLastCall();
        EasyMock.expect(mockPayload.getOctetCount()).andReturn(octet);
        EasyMock.expect(mockPayload.getNumFiles()).andReturn(files);
        PowerMock.replay(mockPayload, PayloadOxum.class);
         
        Assert.assertTrue(bagInfoProcessor.valid());
    }

    @Test
    public void testInvalidFile() throws IOException, Exception {
        TagMetaElement oxum = new TagMetaElement(oxumRE, "0.0", true);
        BufferedReader reader = createReader(oxum);
        setupExpects(reader);
        replayMocks();
        bagInfoProcessor = new BagInfoProcessor(bagInfoPath);

        Assert.assertFalse(bagInfoProcessor.valid());
    }
    
}

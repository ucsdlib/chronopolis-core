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
    Path bagInfoPath;
    FileSystem mockfs;
    File mockFile;

    @Before 
    public void setUp() {
        bagInfoPath = PowerMock.createMock(Path.class);
        mockfs = PowerMock.createMock(FileSystem.class);
        mockFile = PowerMock.createMock(File.class);
    }

    private void setupExpects(BufferedReader reader) throws IOException {
        EasyMock.expect(bagInfoPath.resolve("bag-info.txt")).andReturn(bagInfoPath);
        EasyMock.expect(bagInfoPath.toFile()).andReturn(mockFile).anyTimes();
        EasyMock.expect(mockFile.exists()).andReturn(Boolean.TRUE).anyTimes();
        EasyMock.expect(bagInfoPath.getFileSystem()).andReturn(mockfs);
        PowerMock.mockStatic(Files.class);
        EasyMock.expect(Files.newBufferedReader(bagInfoPath, 
                                                Charset.forName("UTF-8")))
                                                .andReturn(reader);
        
        PowerMock.replay(bagInfoPath, mockFile, Files.class);
    }
    

    @Test
    public void testValidFile() throws IOException {
        TagMetaElement size = new TagMetaElement(bagSizeRE, "1 M");
        TagMetaElement bagDate = new TagMetaElement(baggingDateRE, 
                                                    dateFormat.format(new Date()));
        TagMetaElement oxum = new TagMetaElement(oxumRE, "1048576.1");
        BufferedReader reader = createReader(size, bagDate, oxum);
        setupExpects(reader);
        bagInfoProcessor = new BagInfoProcessor(bagInfoPath);
         
        Assert.assertTrue(bagInfoProcessor.valid());
    }

    @Test
    public void testInvalidFile() throws IOException {
        TagMetaElement oxum = new TagMetaElement(oxumRE, "0.0");
        BufferedReader reader = createReader(oxum);
        setupExpects(reader);
        bagInfoProcessor = new BagInfoProcessor(bagInfoPath);
         
        Assert.assertFalse(bagInfoProcessor.valid());
    }
    
}

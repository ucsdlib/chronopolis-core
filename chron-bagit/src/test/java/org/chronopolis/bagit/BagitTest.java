/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
//import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import org.chronopolis.bagit.util.TagMetaElement;


import static org.chronopolis.bagit.TestUtil.createReader;
import org.easymock.EasyMock;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 *
 * @author shake
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BagitProcessor.class)
public class BagitTest extends TestUtil {
    private final String bagVersionRE = "BagIt-Version";
    private final String tagFileRE = "Tag-File-Character-Encoding";
    private final String currentBagVersion = "0.97";
    private final String tagFileEncoding = "UTF-8";
    BagitProcessor bagitProcessor;
    Path bagitPath;
    File mockFile;
    FileSystem mockFileSystem;


    private void setupExpects(BufferedReader reader) throws IOException {
        EasyMock.expect(bagitPath.resolve("bagit.txt")).andReturn(bagitPath);
        EasyMock.expect(bagitPath.toFile()).andReturn(mockFile).times(2);
        EasyMock.expect(mockFile.exists()).andReturn(Boolean.TRUE).times(3);
        EasyMock.expect(bagitPath.getFileSystem()).andReturn(mockFileSystem);
        PowerMock.mockStatic(Files.class);
        EasyMock.expect(Files.newBufferedReader(bagitPath, 
                                                Charset.forName("UTF-8")))
                                                .andReturn(reader);
        
        PowerMock.replay(bagitPath, mockFile, Files.class);
    }
    
    @Before
    public void setUp() {
        //bagitProcessor = EasyMock.createMock(BagitProcessor.class);
        bagitPath = PowerMock.createMock(Path.class);
        mockFile = PowerMock.createMock(File.class);
    }

    @Test
    public void testValid() throws IOException {
        TagMetaElement version = new TagMetaElement(bagVersionRE, currentBagVersion);
        TagMetaElement encoding = new TagMetaElement(tagFileRE, tagFileEncoding); 
        BufferedReader reader = createReader(version, encoding);
        setupExpects(reader);
        bagitProcessor = new BagitProcessor(bagitPath);
        assert(bagitProcessor.valid());
    }

    @Test
    public void testMissingTagEncoding() throws IOException {
        TagMetaElement version = new TagMetaElement(bagVersionRE, currentBagVersion);
        BufferedReader reader = createReader(version);
        setupExpects(reader);
        bagitProcessor = new BagitProcessor(bagitPath);
        assert(!bagitProcessor.valid());
    }

    @Test
    public void testMissingVersion() throws IOException {
        TagMetaElement encoding = new TagMetaElement(tagFileRE, tagFileEncoding);
        BufferedReader reader = createReader(encoding);
        setupExpects(reader);
        bagitProcessor = new BagitProcessor(bagitPath);
        assert(!bagitProcessor.valid());
    }

    //@Test
    public void testEmptyValues() throws IOException {
        TagMetaElement version = new TagMetaElement(bagVersionRE, "");
        TagMetaElement encoding = new TagMetaElement(tagFileRE, ""); 
        BufferedReader reader = createReader(version, encoding);
        setupExpects(reader);
        assert(!bagitProcessor.valid());
    }

    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import org.chronopolis.bagit.util.TagMetaElement;


/**
 *
 * @author shake
 */
public class BagitTest {
    private final String bagVersionRE = "BagIt-Version";
    private final String tagFileRE = "Tag-File-Character-Encoding";
    private final String currentBagVersion = "0.97";
    private final String tagFileEncoding = "UTF-8";
    BagitProcessor bagitProcessor;
    Path bagitPath;

    @Before
    public void setUp() {
        //bagitProcessor = EasyMock.createMock(BagitProcessor.class);
        bagitPath = EasyMock.createMock(Path.class);
    }

    private BufferedReader createReader(TagMetaElement ...elements) throws IOException {
        PipedWriter pw = new PipedWriter();
        try (BufferedWriter writer = new BufferedWriter(pw)) {
            int i = 0;
            for ( TagMetaElement e : elements ) {
                writer.write(e.toString());
                if ( ++i < elements.length) {
                    writer.newLine();
                }
            }
        }
        PipedReader pr = new PipedReader();
        pr.connect(pw);
        BufferedReader reader = new BufferedReader(pr);
        return reader;
    }

    @Test
    public void testValid() throws IOException {
        TagMetaElement version = new TagMetaElement(bagVersionRE, currentBagVersion);
        TagMetaElement encoding = new TagMetaElement(tagFileRE, tagFileEncoding); 
        BufferedReader reader = createReader(version, encoding);
        EasyMock.expect(Files.newBufferedReader(bagitPath, Charset.forName("UTF-8"))).andReturn(reader);
        bagitProcessor = new BagitProcessor(bagitPath);
        EasyMock.expect(bagitPath.toFile().exists()).andReturn(Boolean.TRUE);
        assert(bagitProcessor.valid());
    }

    @Test
    public void testInvalid() throws IOException {
        TagMetaElement version = new TagMetaElement(bagVersionRE, currentBagVersion);
        BufferedReader reader = createReader(version);
        EasyMock.expect(Files.newBufferedReader(bagitPath, Charset.forName("UTF-8"))).andReturn(reader);
        bagitProcessor = new BagitProcessor(bagitPath);
        EasyMock.expect(bagitPath.toFile().exists()).andReturn(Boolean.TRUE);
        assert(!bagitProcessor.valid());
    }

    @Test
    public void testEmptyValues() throws IOException {
        TagMetaElement version = new TagMetaElement(bagVersionRE, "");
        TagMetaElement encoding = new TagMetaElement(tagFileRE, ""); 
        BufferedReader reader = createReader(version, encoding);
        EasyMock.expect(Files.newBufferedReader(bagitPath, Charset.forName("UTF-8"))).andReturn(reader);
        bagitProcessor = new BagitProcessor(bagitPath);
        EasyMock.expect(bagitPath.toFile().exists()).andReturn(Boolean.TRUE);
        assert(!bagitProcessor.valid());
    }

    
}

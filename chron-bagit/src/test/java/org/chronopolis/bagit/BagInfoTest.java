/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.chronopolis.bagit.util.TagMetaElement;
//import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.chronopolis.bagit.TestUtil.createReader;

/**
 *
 * @author shake
 */
public class BagInfoTest {
    BagInfoProcessor bagInfoProcessor;
    private final String bagSizeRE = "Bag-Size";
    private final String baggingDateRE = "Bagging-Date";
    private final String oxumRE = "Payload-Oxum";
    private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
    Path bagInfoPath;

    @Before 
    public void setUp() {
//        bagInfoPath = EasyMock.createMock(Path.class);
    }

    //@Test
    public void testValidFile() throws IOException {
        TagMetaElement size = new TagMetaElement(bagSizeRE, "1 M");
        TagMetaElement bagDate = new TagMetaElement(baggingDateRE, 
                                                    dateFormat.format(new Date()));
        TagMetaElement oxum = new TagMetaElement(oxumRE, "1048576.1");
        BufferedReader reader = createReader(size, bagDate, oxum);
        /*
        EasyMock.expect(Files.newBufferedReader(bagInfoPath, 
                                                Charset.forName("UTF-8")))
                                                .andReturn(reader);
                                                */
        bagInfoProcessor = new BagInfoProcessor(bagInfoPath);
         
        assert(bagInfoProcessor.valid());
    }

    @Test
    public void testInvalidFile() throws IOException {

    }
    
}

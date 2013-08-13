/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.PipedWriter;
import java.nio.file.Path;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author shake
 */
public class BagInfoTest {
    BagInfoProcessor bagInfoProcessor;
    Path bagInfoPath;

    @Before 
    public void setUp() {
        bagInfoPath = EasyMock.createMock(Path.class);
        bagInfoProcessor = EasyMock.createMock(BagInfoProcessor.class);
    }

    @Test
    public void testValidFile() {
        //TagMetaElement 
        BufferedWriter bw;
        bw = new BufferedWriter(new PipedWriter());
        //bw.write(null);
        
        assert(1==1);
    }
    
}

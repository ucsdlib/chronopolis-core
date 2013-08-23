/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Iterator;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

/**
 *
 * @author shake
 */
public class ManifestTest {
    
    ManifestProcessor mp;
    FileSystem mockFS;
    PathMatcher mockPathMatcher;
    FileSystemProvider mockFSP;
    Path mockPath;

    @Before
    public void setup() {
        mockPath = PowerMock.createMock(Path.class);
        mockFS = PowerMock.createMock(FileSystem.class);
        mockPathMatcher = PowerMock.createMock(PathMatcher.class);
        mockFSP = PowerMock.createMock(FileSystemProvider.class);
        mp = new ManifestProcessor(mockPath);
    }


    //@Test
    public void testValidManifest() throws Exception {
        String hello = "hello";
        PowerMock.mockStatic(Files.class);
        EasyMock.expect(Files.newDirectoryStream(mockPath, "*manifest-*.txt")).andReturn(new MockDirectoryStream());
        /*
        EasyMock.expect(mockPath.getFileSystem()).andReturn(mockFS);
        EasyMock.expect(mockFS.getPathMatcher("glob:*manifest-*.txt")).andReturn(mockPathMatcher);
        EasyMock.expect(mockFS.provider()).andReturn(mockFSP);
        */
        PowerMock.replay(mockPath, Files.class, mockFS, mockPathMatcher, mockFSP);
        
        Boolean v = mp.call();
        Assert.assertTrue(v);
    }


    public class MockDirectoryStream implements DirectoryStream {

        @Override
        public Iterator iterator() {
            ArrayList<Path> dirList = new ArrayList<>();
            dirList.add(Paths.get("manifest-sha256.txt"));
            return dirList.iterator();
        }

        @Override
        public void close() throws IOException {
        }

    } 
}

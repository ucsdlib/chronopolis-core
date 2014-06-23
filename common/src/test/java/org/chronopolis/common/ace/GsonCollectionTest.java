package org.chronopolis.common.ace;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Created by shake on 2/28/14.
 */
@RunWith(JUnit4.class)
public class GsonCollectionTest {
    static final String DIGEST = "SHA-256";
    static final String DIRECTORY = "Directory";
    static final String NAME = "Gson";
    static final String GROUP = "Gson-Group";
    static final String STORAGE = "local";
    static final String AP = "120";
    static final String AT = "true";
    static final String PD = "false";

    @Test
    public void testToJson() throws Exception {
        Gson deserializer = new Gson();
        GsonCollection gson = new GsonCollection.Builder()
            .digestAlgorithm(DIGEST)
            .directory(DIRECTORY)
            .name(NAME)
            .group(GROUP)
            .storage(STORAGE)
            .auditPeriod(AP)
            .auditTokens(AT)
            .proxyData(PD)
            .build();

        System.out.println(gson.toJson());

        GsonCollection fromJson = deserializer.fromJson(gson.toJson(), GsonCollection.class);

        Assert.assertEquals(DIGEST, fromJson.getDigestAlgorithm());
        Assert.assertEquals(DIRECTORY, fromJson.getDirectory());
        Assert.assertEquals(NAME, fromJson.getName());
        Assert.assertEquals(GROUP, fromJson.getGroup());
        Assert.assertEquals(STORAGE, fromJson.getStorage());
    }

}

package org.chronopolis.replicate.batch.rsync;

import com.google.common.collect.ImmutableList;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.rest.api.FileService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.StagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.test.support.CallWrapper;
import org.chronopolis.test.support.ExceptingCallWrapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListingDownloaderTest {

    private final String testName = "listing-test";

    private Bag bag;
    private ReplicationProperties properties;

    private final FileService files = mock(FileService.class);

    @Test
    public void success() throws IOException {
        setup("get-success-kotlin");

        when(files.getFileList(bag.getId()))
                .thenReturn(new CallWrapper<>(generateResponseBody()));

        ListingDownloader downloader = new ListingDownloader(bag, files, properties);
        Result<List<Path>> result = downloader.get();

        Assert.assertTrue(result instanceof Result.Success);
        Result.Success<List<Path>> success = (Result.Success<List<Path>>) result;

        Assert.assertFalse(success.getData().isEmpty());
        for (Path path : success.getData()) {
            List<String> lines = Files.readAllLines(path);
            Assert.assertEquals(1, lines.size());
        }

    }

    @Test
    public void apiException() throws IOException {
        String failureTest = "get-failure";
        setup(failureTest);

        when(files.getFileList(bag.getId()))
                .thenReturn(new ExceptingCallWrapper<>(generateResponseBody()));

        ListingDownloader downloader = new ListingDownloader(bag, files, properties);
        Result<List<Path>> result = downloader.get();

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Result.Error);
        long count = -1;
        Path path = Paths.get(properties.getWorkDirectory(), testName, failureTest);
        try (Stream<Path> files = Files.list(path)) {
            count = files.count();
        } catch (IOException ignored) {
        }

        Assert.assertEquals(0, count);
    }

    @Test
    public void writeFailure() throws IOException {
        setup("get-failure-write");
        // probably not the best idea since it might restrict us to unix
        ReplicationProperties properties = new ReplicationProperties().setWorkDirectory("/dev/null");

        when(files.getFileList(bag.getId()))
                .thenReturn(new CallWrapper<>(generateResponseBody()));

        ListingDownloader downloader = new ListingDownloader(bag, files, properties);
        Result<List<Path>> result = downloader.get();

        Assert.assertTrue(result instanceof Result.Error);
        Result.Error error = (Result.Error) result;
        Assert.assertTrue(error.getException() instanceof IOException);
    }

    @Test
    public void testWriter() {
        // possibly similar to the issue as above
        ListingDownloader.BodyWriter writer =
                new ListingDownloader.BodyWriter(Paths.get("/tmp/dne"));

        Result<Path> result = writer.write(0, ImmutableList.of("test"));

        Assert.assertTrue(result instanceof Result.Error);
    }

    private ResponseBody generateResponseBody() {
        return ResponseBody.create(MediaType.parse("text/plain"),
                "file-0\nfile-1\nfile-2\nfile-3\nfile-4");
    }

    private void setup(String name) throws IOException {
        Path workDir = Files.createTempDirectory(testName);
        properties = new ReplicationProperties()
                .setWorkDirectory(workDir.toString());

        StagingStorage storage = new StagingStorage(true, 10L, 1L, 5L, testName, of());
        bag = new Bag(1L, 10L, 5L, storage, storage, ZonedDateTime.now(), ZonedDateTime.now(),
                name, testName, testName, BagStatus.REPLICATING, of());
    }

    @After
    public void cleanup() {
        if (properties != null && properties.getWorkDirectory() != null) {
            try (Stream<Path> paths = Files.walk(Paths.get(properties.getWorkDirectory()))) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ignored) {
            }
        }
    }

}

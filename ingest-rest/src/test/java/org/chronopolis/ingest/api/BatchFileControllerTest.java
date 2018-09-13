package org.chronopolis.ingest.api;

import com.querydsl.core.types.Predicate;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.support.BagFileCSVProcessor;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basic tests for the {@link BatchFileController}
 * <p>
 * Not sure how to test for the Internal Server Errors, maybe sending a null stream or something
 *
 * @author shake
 */
@RunWith(SpringRunner.class)
@SuppressWarnings("Duplicates")
@WebMvcTest(controllers = BatchFileController.class)
public class BatchFileControllerTest extends ControllerTest {

    private ConcurrentSkipListSet<Long> processing = new ConcurrentSkipListSet<>();

    @MockBean private PagedDAO dao;
    @MockBean private BagFileCSVProcessor processor;

    @Before
    public void setup() {
        BatchFileController controller = new BatchFileController(dao, processor, processing);
        setupMvc(controller);
    }

    @After
    public void after() {
        Assert.assertTrue(processing.isEmpty());
    }

    @Test
    public void uploadConflict() throws Exception {
        processing.add(1L);
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("valid.csv");

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.authorized(eq(authorizedPrincipal), eq(new Bag()))).thenReturn(true);
        MockMultipartFile csvMp = new MockMultipartFile(
                "file",
                "valid.csv",
                MediaType.TEXT_PLAIN_VALUE,
                Files.newInputStream(toCsv, StandardOpenOption.READ));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.fileUpload("/api/bags/{id}/files", 1L)
                        .file(csvMp)
                        .principal(authorizedPrincipal);
        mvc.perform(request)
                .andExpect(status().is(HttpStatus.CONFLICT.value()));

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).authorized(eq(authorizedPrincipal), any());
        verify(processor, never()).apply(eq(1L), any());

        processing.remove(1L);
    }

    @Test
    public void uploadForbidden() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("valid.csv");

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.authorized(eq(authorizedPrincipal), eq(new Bag()))).thenReturn(false);
        MockMultipartFile csvMp = new MockMultipartFile(
                "file",
                "valid.csv",
                MediaType.TEXT_PLAIN_VALUE,
                Files.newInputStream(toCsv, StandardOpenOption.READ));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.fileUpload("/api/bags/{id}/files", 1L)
                        .file(csvMp)
                        .principal(authorizedPrincipal);
        mvc.perform(request)
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).authorized(eq(authorizedPrincipal), any());
        verify(processor, never()).apply(eq(1L), any());
    }

    @Test
    public void uploadBagNotFound() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("valid.csv");

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(null);
        MockMultipartFile csvMp = new MockMultipartFile(
                "file",
                "valid.csv",
                MediaType.TEXT_PLAIN_VALUE,
                Files.newInputStream(toCsv, StandardOpenOption.READ));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.fileUpload("/api/bags/{id}/files", 1L)
                        .file(csvMp)
                        .principal(authorizedPrincipal);
        mvc.perform(request)
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, never()).authorized(eq(authorizedPrincipal), any());
        verify(processor, never()).apply(eq(1L), any());
    }

    @Test
    public void uploadCsv() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("valid.csv");

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.authorized(eq(authorizedPrincipal), eq(new Bag()))).thenReturn(true);
        when(processor.apply(eq(1L), any())).thenReturn(ResponseEntity.ok().build());

        MockMultipartFile csvMp = new MockMultipartFile(
                "file",
                "valid.csv",
                MediaType.TEXT_PLAIN_VALUE,
                Files.newInputStream(toCsv, StandardOpenOption.READ));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.fileUpload("/api/bags/{id}/files", 1L)
                        .file(csvMp)
                        .principal(authorizedPrincipal);
        mvc.perform(request)
                // .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).authorized(eq(authorizedPrincipal), any());
        verify(processor, times(1)).apply(eq(1L), any());
    }

    @Test
    public void uploadCsvNoMediaType() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("valid.csv");

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.authorized(eq(authorizedPrincipal), eq(new Bag()))).thenReturn(true);

        MockMultipartFile csvMp = new MockMultipartFile(
                "file",
                Files.newInputStream(toCsv, StandardOpenOption.READ));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.fileUpload("/api/bags/{id}/files", 1L)
                        .file(csvMp)
                        .principal(authorizedPrincipal);
        mvc.perform(request)
                // .andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).authorized(eq(authorizedPrincipal), any());
        verify(processor, never()).apply(any(), any());
    }

    @Test
    public void uploadCsvInvalid() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("invalid.csv");

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.authorized(eq(authorizedPrincipal), eq(new Bag()))).thenReturn(true);
        when(processor.apply(eq(1L), any())).thenReturn(ResponseEntity.ok().build());

        MockMultipartFile csvMp = new MockMultipartFile(
                "file",
                "invalid.csv",
                MediaType.TEXT_PLAIN_VALUE,
                Files.newInputStream(toCsv, StandardOpenOption.READ));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.fileUpload("/api/bags/{id}/files", 1L)
                        .file(csvMp)
                        .principal(authorizedPrincipal);
        mvc.perform(request)
                // .andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).authorized(eq(authorizedPrincipal), any());
        verify(processor, never()).apply(any(), any());
    }

    @Test
    public void uploadCsvInconsistent() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("inconsistent.csv");

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.authorized(eq(authorizedPrincipal), eq(new Bag()))).thenReturn(true);
        when(processor.apply(eq(1L), any())).thenReturn(ResponseEntity.ok().build());

        MockMultipartFile csvMp = new MockMultipartFile(
                "file",
                "inconsistent.csv",
                MediaType.TEXT_PLAIN_VALUE,
                Files.newInputStream(toCsv, StandardOpenOption.READ));
        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.fileUpload("/api/bags/{id}/files", 1L)
                        .file(csvMp)
                        .principal(authorizedPrincipal);

        mvc.perform(request)
                .andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).authorized(eq(authorizedPrincipal), any());
        verify(processor, never()).apply(any(), any());
    }

}
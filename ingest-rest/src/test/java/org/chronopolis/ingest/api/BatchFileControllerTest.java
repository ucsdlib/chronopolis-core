package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.support.BagFileCSVProcessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basic tests for the {@link BatchFileController}
 *
 * Not sure how to test for the Internal Server Errors, maybe sending a null stream or something
 *
 * @author shake
 */
@SuppressWarnings("Duplicates")
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BatchFileController.class)
public class BatchFileControllerTest extends ControllerTest {

    @MockBean private PagedDAO dao;
    @MockBean private BagFileCSVProcessor processor;

    @Before
    public void setup() {
        BatchFileController controller = new BatchFileController(processor);
        setupMvc(controller);
    }

    @Test
    public void uploadCsv() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("large-0.csv");

        Mockito.when(processor.apply(eq(1L), any())).thenReturn(ResponseEntity.ok().build());

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

        Mockito.verify(processor, times(1)).apply(eq(1L), any());
    }

    @Test
    public void uploadCsvNoMediaType() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("valid.csv");

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

        Mockito.verify(processor, never()).apply(any(), any());
    }

    @Test
    public void uploadCsvInvalid() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("invalid.csv");

        Mockito.when(processor.apply(eq(1L), any())).thenReturn(ResponseEntity.ok().build());

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

        Mockito.verify(processor, never()).apply(any(), any());
    }

    @Test
    public void uploadCsvInconsistent() throws Exception {
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("inconsistent.csv");

        Mockito.when(processor.apply(eq(1L), any())).thenReturn(ResponseEntity.ok().build());

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

        Mockito.verify(processor, never()).apply(any(), any());
    }

}
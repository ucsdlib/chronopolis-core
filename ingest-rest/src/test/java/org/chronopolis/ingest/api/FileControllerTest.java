package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableSet;
import com.querydsl.core.types.Predicate;
import org.chronopolis.ingest.repository.dao.DataFileDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.TokenStore;
import org.chronopolis.rest.entities.storage.Fixity;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Simple tests for the FileController
 * <p>
 * note: if a controller method simply delegates to the dao, we don't test here
 * todo: check json serialization
 *
 * @author shake
 */
@RunWith(SpringRunner.class)
@SuppressWarnings("Duplicates")
@WebMvcTest(controllers = FileController.class)
public class FileControllerTest extends ControllerTest {

    @MockBean
    DataFileDao dao;

    @Before
    public void setup() {
        FileController controller = new FileController(dao);
        setupMvc(controller);
    }

    @Test
    @Ignore
    public void getFiles() {
        TokenStore ts = new TokenStore();
        ts.setId(2L);
        ts.setFilename("/tokens");
        ts.setSize(1L);
        ts.setBag(null);
        ts.setDtype("TOKEN_STORE");
        ts.setCreatedAt(ZonedDateTime.now());
        ts.setUpdatedAt(ZonedDateTime.now());

        BagFile bf = new BagFile();
        bf.setId(1L);
        bf.setSize(1L);
        bf.setBag(null);
        bf.setDtype("BAG");
        bf.setFilename("/bag-file");
        bf.setFixities(ImmutableSet.of());
        bf.setCreatedAt(ZonedDateTime.now());
        bf.setUpdatedAt(ZonedDateTime.now());

        // BagFileFilter filter = new BagFileFilter();
        // response -> of(ts, bf);
        when(dao.findPage(eq(QBagFile.bagFile), any())).thenReturn(null);
    }

    @Test
    @Ignore
    public void getBagFiles() {
        TokenStore ts = new TokenStore();
        ts.setId(2L);
        ts.setFilename("/tokens");

        BagFile bf = new BagFile();
        bf.setDtype("BAG");
        bf.setFilename("/bag-file");

        // response -> of(ts, bf)

        when(dao.findPage(eq(QBagFile.bagFile), any())).thenReturn(null);
    }

    @Test
    public void getFile() throws Exception {
        TokenStore ts = new TokenStore();
        ts.setId(2L);
        ts.setFilename("/tokens");

        when(dao.findOne(eq(QDataFile.dataFile), any(Predicate.class))).thenReturn(ts);

        mvc.perform(get("/api/files/{id}", 2L)
                .principal(authorizedPrincipal))
                .andDo(print())
                .andExpect(status().isOk());

        verify(dao, times(1)).findOne(eq(QDataFile.dataFile), any(Predicate.class));
    }

    @Test
    public void getFileNotFound() throws Exception {
        when(dao.findOne(eq(QDataFile.dataFile), any(Predicate.class))).thenReturn(null);

        mvc.perform(get("/api/files/{id}", 2L)
                .principal(authorizedPrincipal))
                .andExpect(status().isNotFound());

        verify(dao, times(1)).findOne(eq(QDataFile.dataFile), any(Predicate.class));
    }

    @Test
    public void getBagFile() throws Exception {
        Bag bag = new Bag();
        BagFile bf = new BagFile();
        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(bag);
        when(dao.findOne(eq(QBagFile.bagFile), any(Predicate.class))).thenReturn(bf);

        mvc.perform(get("/api/bags/{bag_id}/files/{file_id}", 1L, 2L)
                .principal(authorizedPrincipal))
                .andExpect(status().isOk());

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).findOne(eq(QBagFile.bagFile), any(Predicate.class));
    }

    @Test
    public void getBagFileBadRequest() throws Exception {
        BagFile bf = new BagFile();
        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(null);
        when(dao.findOne(eq(QBagFile.bagFile), any(Predicate.class))).thenReturn(bf);

        mvc.perform(get("/api/bags/{bag_id}/files/{file_id}", 1L, 2L)
                .principal(authorizedPrincipal))
                .andExpect(status().isBadRequest());

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).findOne(eq(QBagFile.bagFile), any(Predicate.class));
    }

    @Test
    public void getBagFileNotFound() throws Exception {
        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.findOne(eq(QBagFile.bagFile), any(Predicate.class))).thenReturn(null);

        mvc.perform(get("/api/bags/{bag_id}/files/{file_id}", 1L, 2L)
                .principal(authorizedPrincipal))
                .andExpect(status().isNotFound());

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).findOne(eq(QBagFile.bagFile), any(Predicate.class));
    }


    @Test
    public void getBagFileFixities() throws Exception {
        Fixity fixity = new Fixity();
        fixity.setAlgorithm("SHA-256");
        fixity.setCreatedAt(ZonedDateTime.now());
        fixity.setValue("that-one-empty-value");
        BagFile bagFile = new BagFile();
        bagFile.setFixities(ImmutableSet.of(fixity));

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.findOne(eq(QBagFile.bagFile), any(Predicate.class))).thenReturn(bagFile);

        mvc.perform(get("/api/bags/{bag_id}/files/{file_id}/fixity", 1L, 2L)
                .principal(authorizedPrincipal))
                .andExpect(status().isOk());

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).findOne(eq(QBagFile.bagFile), any(Predicate.class));
    }

    @Test
    public void getBagFileFixitiesBagIsNull() throws Exception {
        BagFile bagFile = new BagFile();
        bagFile.setFixities(ImmutableSet.of());

        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(null);
        when(dao.findOne(eq(QBagFile.bagFile), any(Predicate.class))).thenReturn(bagFile);

        mvc.perform(get("/api/bags/{bag_id}/files/{file_id}/fixity", 1L, 2L)
                .principal(authorizedPrincipal))
                .andExpect(status().isBadRequest());

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).findOne(eq(QBagFile.bagFile), any(Predicate.class));
    }

    @Test
    public void getBagFileFixitiesFileIsNull() throws Exception {
        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(new Bag());
        when(dao.findOne(eq(QBagFile.bagFile), any(Predicate.class))).thenReturn(null);

        mvc.perform(get("/api/bags/{bag_id}/files/{file_id}/fixity", 1L, 2L)
                .principal(authorizedPrincipal))
                .andExpect(status().isBadRequest());

        verify(dao, times(1)).findOne(eq(QBag.bag), any(Predicate.class));
        verify(dao, times(1)).findOne(eq(QBagFile.bagFile), any(Predicate.class));
    }
}
package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import org.chronopolis.ingest.repository.dao.BagDao;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.projections.CompleteBag;
import org.chronopolis.rest.entities.projections.PartialBag;
import org.chronopolis.rest.models.create.BagCreate;
import org.chronopolis.rest.models.enums.BagStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Tests for the staging API
 *
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BagController.class)
public class BagControllerTest extends ControllerTest {

    private static final String BAG = "test-bag";
    private static final String LOCATION = "bags/test-bag-0";

    // Mocks for the StagingController
    @MockBean private BagDao dao;

    @Before
    public void setup() {
        BagController controller = new BagController(dao);
        setupMvc(controller);
    }

    @Test
    public void testGetBags() throws Exception {
        PageImpl<PartialBag> bags = new PageImpl<>(ImmutableList.of(partialBag()));
        when(dao.findViewAsPage(any())).thenReturn(bags);

        mvc.perform(
                get("/api/bags/")
                        .principal(() -> "user"))
                // .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetBag() throws Exception {
        when(dao.findCompleteView(1L)).thenReturn(completeView());

        mvc.perform(
                get("/api/bags/{id}", 1L)
                        .principal(() -> "user"))
                // .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.depositor").value("test-depositor"))
                .andExpect(jsonPath("$.name").value("test-bag"));
    }

    private PartialBag partialBag() {
        return new PartialBag(1L, BAG, NAMESPACE, 1L, 1L, BagStatus.DEPOSITED,
                ZonedDateTime.now(), ZonedDateTime.now(), NAMESPACE, emptySet());
    }

    private CompleteBag completeView() {
        return new CompleteBag(1L, BAG, NAMESPACE, 1L, 1L, BagStatus.INITIALIZED,
                ZonedDateTime.now(), ZonedDateTime.now(), NAMESPACE, emptySet(), emptyMap());
    }

    @Test
    public void testGetDoesNotExist() throws Exception {
        when(dao.findOne(any(), any(Predicate.class))).thenReturn(null);
        mvc.perform(
                get("/api/bags/{id}", 100L)
                        .principal(() -> "user"))
                // .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testStageBag() throws Exception {
        // Bag Ingest Request
        BagCreate request = new BagCreate(BAG, 1L, 1L, 1L, LOCATION, NAMESPACE);

        // created bag to return
        Bag bag = bag();
        BagCreateResult result = new BagCreateResult(bag);

        when(dao.processRequest(eq("user"), eq(request))).thenReturn(result);

        mvc.perform(
                post("/api/bags")
                    .principal(() -> "user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJson(request)))
                // .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depositor").value(NAMESPACE))
                .andExpect(jsonPath("$.name").value(BAG));

        verify(dao, times(1)).processRequest(eq("user"), eq(request));
    }

    private Bag bag() {
        Bag bag = new Bag(BAG, "namespace", DEPOSITOR, 1L, 1L, BagStatus.DEPOSITED);
        bag.setId(1L);
        return bag;
    }

}
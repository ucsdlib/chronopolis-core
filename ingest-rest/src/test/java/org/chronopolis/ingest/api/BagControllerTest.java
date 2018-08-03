package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.create.BagCreate;
import org.chronopolis.rest.models.enums.BagStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
    // private static final String NAMESPACE = "test-depositor";
    // private final Depositor DEPOSITOR = new Depositor(NAMESPACE, NAMESPACE, NAMESPACE);

    // Mocks for the StagingController
    @MockBean private BagService bagService;

    @Before
    public void setup() {
        BagController controller = new BagController(bagService);
        setupMvc(controller);
    }

    @Test
    public void testGetBags() throws Exception {
        when(bagService.findAll(eq(new BagSearchCriteria()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(ImmutableList.of(bag())));

        mvc.perform(
                get("/api/bags/")
                        .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetBag() throws Exception {
        when(bagService.find(eq(new BagSearchCriteria().withId(1L)))).thenReturn(bag());

        mvc.perform(
                get("/api/bags/{id}", 1L)
                        .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.depositor").value("test-depositor"))
                .andExpect(jsonPath("$.name").value("test-bag"));
    }

    @Test
    public void testGetDoesNotExist() throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(null);
        mvc.perform(
                get("/api/bags/{id}", 100L)
                        .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testStageBag() throws Exception {
        // Bag Ingest Request
        BagCreate request = new BagCreate(BAG, 1L, 1L, 1L, LOCATION, NAMESPACE);

        // created bag to return
        Bag bag = bag();
        BagCreateResult result = new BagCreateResult(bag);

        when(bagService.processRequest(eq("user"), eq(request))).thenReturn(result);

        mvc.perform(
                post("/api/bags")
                    .principal(() -> "user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJson(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depositor").value(NAMESPACE))
                .andExpect(jsonPath("$.name").value(BAG));

        verify(bagService, times(1)).processRequest(eq("user"), eq(request));
    }

    private Bag bag() {
        Bag b = new Bag(BAG, "namespace", DEPOSITOR, 1L, 1L, BagStatus.DEPOSITED);
        b.setId(1L);
        b.setBagStorage(Collections.emptySet());
        b.setTokenStorage(Collections.emptySet());
        b.setDistributions(Collections.emptySet());
        return b;
    }

}
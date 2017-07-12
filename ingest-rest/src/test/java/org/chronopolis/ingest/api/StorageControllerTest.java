package org.chronopolis.ingest.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.models.RegionCreate;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.storage.StorageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * Created by shake on 7/11/17.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(secure = false, controllers = StorageController.class)
@ContextConfiguration(classes = WebContext.class)
public class StorageControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    private SearchService<StorageRegion, Long, StorageRegionRepository> service;

    @Test
    public void getRegion() throws Exception {
        when(service.find(any(SearchCriteria.class))).thenReturn(null);

        mvc.perform(get("/api/storage")
                .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getRegions() throws Exception {
        mvc.perform(get("/api/storage")
                .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void createRegion() throws Exception {
        RegionCreate request = new RegionCreate();
        request.setCapacity(1000L)
                .setType(StorageType.LOCAL)
                .setReplicationPath("/test-path")
                .setReplicationServer("test-server")
                .setReplicationUser("test-user");

        mvc.perform(post("/api/storage")
                .principal(() -> "user")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJson(request)))
                .andDo(print())
                .andExpect(status().is(200));
    }

    private <T> String asJson(T request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
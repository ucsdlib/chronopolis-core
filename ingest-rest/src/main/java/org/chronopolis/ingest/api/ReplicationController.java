package org.chronopolis.ingest.api;

import org.chronopolis.ingest.model.ReplicationAction;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/staging")
public class ReplicationController {

    public static List<ReplicationAction> currentActions = new ArrayList<>();

    @RequestMapping(value = "/action")
    public String updateAction(@RequestBody ReplicationAction action) {
        return "action-id";
    }

}

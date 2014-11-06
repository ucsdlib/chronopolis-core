package org.chronopolis.ingest.api;

import org.chronopolis.ingest.model.Bag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/staging")
public class StagingController {

    public static List<Bag> bags = new ArrayList<>();

    @RequestMapping(value = "bags", method = RequestMethod.GET)
    public List<Bag> getBags() {
        return bags;
    }

    @RequestMapping(value = "bags/{bag-id}", method = RequestMethod.GET)
    public Bag getBag(@PathVariable("bag-id") String bagId) {
        return new Bag();
    }

    @RequestMapping(value = "bag", method = RequestMethod.PUT)
    public String putBag(@RequestBody Bag bag) {
        return "ok";
    }

}

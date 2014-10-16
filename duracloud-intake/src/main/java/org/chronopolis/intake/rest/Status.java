package org.chronopolis.intake.rest;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Created by shake on 3/10/14.
 */
@RestController
@RequestMapping("/api/status")
public class Status {
    private static final Logger log = Logger.getLogger(Status.class);

    // StatusAccessor statusAccessor = PersistUtil.getStatusAccessor();

    @RequestMapping(value = "{id}/bagging", method = RequestMethod.GET)
    public ResponseEntity<String> getBaggingStatus(@PathVariable("id") String id) throws IOException {
        // BagStatus status = statusAccessor.get(id);
        // if (status == null) {
        //    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        // }

        // return new ResponseEntity<>(MessageWrapper.toJson(status.getBaggingStatus()), HttpStatus.OK);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @RequestMapping(value = "{id}/chronopolis", method = RequestMethod.GET)
    public ResponseEntity<String> getChronopolisStatus(@PathVariable("id") String id) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "{id}/dpn", method = RequestMethod.GET)
    public ResponseEntity<String> getDPNStatus(@PathVariable("id") String id) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "{id}/all", method = RequestMethod.GET)
    public ResponseEntity<String> getAllStatus(@PathVariable("id") String id) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

}

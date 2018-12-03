package org.chronopolis.ingest.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by shake on 5/29/15.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    /**
     * Get the current running version of the Ingest Server
     *
     * @return the version
     */
    @RequestMapping(method = RequestMethod.GET)
    public Version getVersion() {
        return new Version();
    }

    private class Version {
        private final String version =
                getClass().getPackage().getImplementationVersion();

        public String getVersion() {
            return version;
        }
    }

}

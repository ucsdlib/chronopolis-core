package org.chronopolis.ingest.api;

import org.chronopolis.ingest.support.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by shake on 5/29/15.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    @RequestMapping(method = RequestMethod.GET)
    public Version getVersion() {
        access.info("[GET /api/version]");
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

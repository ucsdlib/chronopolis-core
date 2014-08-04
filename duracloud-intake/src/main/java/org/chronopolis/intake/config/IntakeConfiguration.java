package org.chronopolis.intake.config;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.intake.duracloud.batch.SnapshotJobManager;
import org.chronopolis.intake.duracloud.batch.SnapshotProcessor;
import org.chronopolis.intake.duracloud.batch.SnapshotReader;
import org.chronopolis.intake.duracloud.batch.SnapshotWriter;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by shake on 8/4/14.
 */
@Configuration
public class IntakeConfiguration {

    @Autowired
    IntakeSettings intakeSettings;

    @Autowired
    ChronopolisSettings chronopolisSettings;

    @Bean
    SnapshotProcessor snapshotProcessor() {
        return new SnapshotProcessor(intakeSettings);
    }

    @Bean
    SnapshotWriter snapshotWriter() {
        return new SnapshotWriter(null, null, chronopolisSettings);
    }

    @Bean(destroyMethod = "destroy")
    SnapshotJobManager snapshotJobManager() {
        return new SnapshotJobManager(snapshotProcessor(), snapshotWriter(), intakeSettings);
    }


}

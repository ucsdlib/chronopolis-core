package org.chronopolis.replicate.test.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by shake on 3/30/15.
 */
@Configuration
public class TestConfig {

    @Autowired
    JobBuilderFactory factory;

    @Autowired
    StepBuilderFactory sbf;

    @Bean
    Job job() {
        return factory.get("collection-replicate")
                .start(sbf.get("test-step")
                        .tasklet(new Tasklet() {
                            @Override
                            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                                TimeUnit.SECONDS.sleep(5);
                                return RepeatStatus.FINISHED;
                            }
                        })
                        .build())
                .build();
    }

}

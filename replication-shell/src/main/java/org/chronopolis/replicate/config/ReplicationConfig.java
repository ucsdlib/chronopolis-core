package org.chronopolis.replicate.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.amqp.error.ErrorHandlerImpl;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.CredentialRequestInterceptor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.settings.AMQPSettings;
import org.chronopolis.common.settings.AceSettings;
import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicateMessageListener;
import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.chronopolis.replicate.batch.ReplicationStepListener;
import org.chronopolis.replicate.processor.CollectionInitProcessor;
import org.chronopolis.replicate.processor.CollectionRestoreLocationProcessor;
import org.chronopolis.replicate.processor.CollectionRestoreRequestProcessor;
import org.chronopolis.replicate.processor.FileQueryProcessor;
import org.chronopolis.replicate.processor.FileQueryResponseProcessor;
import org.chronopolis.common.util.URIUtil;
import org.chronopolis.rest.api.IngestAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import retrofit.RestAdapter;

/**
 * Created by shake on 4/16/14.
 */
@Configuration
@EnableBatchProcessing
@Import({JPAConfiguration.class})
public class ReplicationConfig {
    public final Logger log = LoggerFactory.getLogger(ReplicationConfig.class);

    @Autowired
    JobRepository jobRepository;

    @Bean
    AceService aceService(AceSettings aceSettings) {
        // Next build the retrofit adapter
        String endpoint = URIUtil.buildAceUri(aceSettings.getAmHost(),
                aceSettings.getAmPort(),
                aceSettings.getAmPath()).toString();

        CredentialRequestInterceptor interceptor = new CredentialRequestInterceptor(
                aceSettings.getAmUser(),
                aceSettings.getAmPassword());

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setRequestInterceptor(interceptor)
                .build();

        return restAdapter.create(AceService.class);
    }

    @Bean
    IngestAPI ingestAPI(IngestAPISettings apiSettings) {
        String endpoint =  URIUtil.buildAceUri(
                apiSettings.getIngestAPIHost(),
                apiSettings.getIngestAPIPort(),
                apiSettings.getIngestAPIPath()).toString();

        // TODO: This can timeout on long polls, see SO for potential fix
        // http://stackoverflow.com/questions/24669309/how-to-increase-timeout-for-retrofit-requests-in-robospice-android
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setRequestInterceptor(new CredentialRequestInterceptor(
                        apiSettings.getIngestAPIUsername(),
                        apiSettings.getIngestAPIPassword()))
                .build();

        return adapter.create(IngestAPI.class);
    }

    @Bean
    ReplicationJobStarter jobStarter(ChronProducer producer,
                                     MessageFactory messageFactory,
                                     ReplicationSettings settings,
                                     MailUtil mailUtil,
                                     AceService aceService,
                                     IngestAPI ingestAPI,
                                     ReplicationStepListener replicationStepListener,
                                     JobLauncher jobLauncher,
                                     JobBuilderFactory jobBuilderFactory,
                                     StepBuilderFactory stepBuilderFactory) {
        return new ReplicationJobStarter(producer,
                messageFactory,
                settings,
                mailUtil,
                aceService,
                ingestAPI,
                replicationStepListener,
                jobLauncher,
                jobBuilderFactory,
                stepBuilderFactory);
    }

    /*
    @Bean
    RestoreRepository restoreRepository() {
        return null;
    }
    */
    @Bean
    ReplicationStepListener replicationStepListener(ReplicationSettings replicationSettings,
                                                    MailUtil mailUtil) {
        return new ReplicationStepListener(replicationSettings, mailUtil);
    }

    @Bean
    MailUtil mailUtil(SMTPSettings smtpSettings) {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(smtpSettings.getFrom());
        mailUtil.setSmtpTo(smtpSettings.getTo());
        mailUtil.setSmtpHost(smtpSettings.getHost());
        mailUtil.setSmtpSend(smtpSettings.getSend());
        return mailUtil;
    }

    @Bean
    MessageFactory messageFactory(ReplicationSettings chronopolisSettings) {
        return new MessageFactory(chronopolisSettings);
    }

    @Bean
    ConnectionListener connectionListener() {
        return new ConnectionListenerImpl();
    }

    @Bean
    ConnectionFactory rabbitConnectionFactory(AMQPSettings amqpSettings) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setRequestedHeartbeat(60);
        connectionFactory.setConnectionTimeout(300);
        connectionFactory.setVirtualHost(amqpSettings.getVirtualHost());

        return connectionFactory;
    }

    @Bean
    CachingConnectionFactory connectionFactory(AMQPSettings amqpSettings,
                                               ConnectionFactory rabbitConnectionFactory) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitConnectionFactory);

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        connectionFactory.addConnectionListener(connectionListener());
        connectionFactory.setAddresses(amqpSettings.getAddresses());

        return connectionFactory;
    }

    @Bean
    RabbitTemplate rabbitTemplate(AMQPSettings amqpSettings,
                                  CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange(amqpSettings.getExchange());
        template.setConnectionFactory(connectionFactory);
        template.setMandatory(true);


        return template;
    }

    @Bean
    TaskExecutor simpleAsyncTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(2);
        return taskExecutor;
    }

    /*
    @Bean
    JobRepository jobRepository() throws Exception {
        MapJobRepositoryFactoryBean factory =
                new MapJobRepositoryFactoryBean();
        factory.afterPropertiesSet();
        return factory.getObject();

    }
    */

    @Bean
    JobLauncher jobLauncher(JobRepository jobRepository, TaskExecutor simpleAsyncTaskExecutor) {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(simpleAsyncTaskExecutor);
        return jobLauncher;
    }

    @Bean
    TopicProducer producer(RabbitTemplate rabbitTemplate) {
        return new TopicProducer(rabbitTemplate);
    }

    @Bean
    FileQueryProcessor fileQueryProcessor(TopicProducer producer) {
        return new FileQueryProcessor(producer);
    }

    @Bean
    FileQueryResponseProcessor fileQueryResponseProcessor(TopicProducer producer) {
        return new FileQueryResponseProcessor(producer);
    }

    @Bean
    CollectionInitProcessor collectionInitProcessor(ReplicationJobStarter replicationJobStarter) {
        return new CollectionInitProcessor(replicationJobStarter);
    }

    @Bean
    CollectionRestoreRequestProcessor collectionRestoreRequestProcessor(ChronProducer producer,
                                                                        MessageFactory messageFactory,
                                                                        AceService aceService,
                                                                        RestoreRepository restoreRepository) {
        return new CollectionRestoreRequestProcessor(producer,
                messageFactory,
                aceService,
                restoreRepository);
    }

    @Bean
    CollectionRestoreLocationProcessor collectionRestoreLocationProcessor(ReplicationSettings chronopolisSettings,
                                                                          ChronProducer producer,
                                                                          MessageFactory messageFactory,
                                                                          RestoreRepository restoreRepository,
                                                                          MailUtil mailUtil) {
        return new CollectionRestoreLocationProcessor(chronopolisSettings,
                producer,
                messageFactory,
                restoreRepository,
                mailUtil);
    }

    @Bean
    MessageListener messageListener(CollectionRestoreRequestProcessor collectionRestoreRequestProcessor,
                                    CollectionRestoreLocationProcessor collectionRestoreLocationProcessor,
                                    CollectionInitProcessor collectionInitProcessor,
                                    FileQueryProcessor fileQueryProcessor,
                                    FileQueryResponseProcessor fileQueryResponseProcessor) {
        return new ReplicateMessageListener(
                fileQueryProcessor,
                fileQueryResponseProcessor,
                collectionInitProcessor,
                collectionRestoreRequestProcessor,
                collectionRestoreLocationProcessor);
    }

    @Bean
    ErrorHandlerImpl errorHandler() {
        return new ErrorHandlerImpl();
    }

    @Bean
    TopicExchange topicExchange(AMQPSettings amqpSettings) {
        return new TopicExchange(amqpSettings.getExchange());
    }

    @Bean
    Queue broadcastQueue(ReplicationSettings replicationSettings) {
       return new Queue(replicationSettings.getBroadcastQueueName(), true);
    }

    @Bean
    Binding broadcastBinding(TopicExchange topicExchange,
                             ReplicationSettings replicationSettings,
                             Queue broadcastQueue) {
        return BindingBuilder.bind(broadcastQueue)
                             .to(topicExchange)
                             .with(replicationSettings.getBroadcastQueueBinding());
    }


    @Bean
    Queue directQueue(ReplicationSettings replicationSettings) {
       return new Queue(replicationSettings.getDirectQueueName(), true);
    }

    @Bean
    Binding directBinding(ReplicationSettings replicationSettings,
                          Queue directQueue,
                          TopicExchange topicExchange) {
        return BindingBuilder.bind(directQueue)
                             .to(topicExchange)
                             .with(replicationSettings.getDirectQueueBinding());
    }

    @Bean
    RabbitAdmin rabbitAdmin(final Binding directBinding,
                            final Binding broadcastBinding,
                            final Queue directQueue,
                            final Queue broadcastQueue,
                            TopicExchange topicExchange,
                            CachingConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(topicExchange);

        // admin.declareQueue(testQueue());
        admin.declareQueue(broadcastQueue);
        admin.declareQueue(directQueue);

        // admin.declareBinding(testBinding());
        admin.declareBinding(broadcastBinding);
        admin.declareBinding(directBinding);

        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer(MessageListener messageListener,
                                                                  ReplicationSettings replicationSettings,
                                                                  CachingConnectionFactory connectionFactory) {
        String broadcastQueueName = replicationSettings.getBroadcastQueueName();
        String directQueueName = replicationSettings.getDirectQueueName();

        log.info("Broadcast queue {} bound to {}",
                broadcastQueueName,
                replicationSettings.getBroadcastQueueBinding());
        log.info("Direct queue {} bound to {}",
                directQueueName,
                replicationSettings.getDirectQueueBinding());

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setErrorHandler(errorHandler());
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(broadcastQueueName, directQueueName);
        container.setMessageListener(messageListener);
        return container;
    }

}

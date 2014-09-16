package org.chronopolis.ingest.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.restore.CollectionRestore;
import org.chronopolis.common.restore.LocalRestore;
import org.chronopolis.common.settings.AMQPSettings;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.ingest.IngestMessageListener;
import org.chronopolis.ingest.processor.CollectionInitCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionInitReplyProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreReplyProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreRequestProcessor;
import org.chronopolis.ingest.processor.PackageIngestStatusQueryProcessor;
import org.chronopolis.ingest.processor.PackageReadyProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shake on 4/10/14.
 */
@Configuration
@Import(IngestJPAConfiguration.class)
public class IngestConfiguration {

    private final Logger log = LoggerFactory.getLogger(IngestConfiguration.class);

    @Autowired
    public DatabaseManager manager;

    @Autowired
    public RestoreRepository restoreRepository;

    @Bean
    public ConnectionListenerImpl connectionListener() {
        return new ConnectionListenerImpl();
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory(AMQPSettings amqpSettings) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        // Move to properties
        connectionFactory.setRequestedHeartbeat(60);
        connectionFactory.setConnectionTimeout(300);
        connectionFactory.setVirtualHost(amqpSettings.getVirtualHost());

        return connectionFactory;
    }


    @Bean
    public CachingConnectionFactory connectionFactory(AMQPSettings amqpSettings,
                                                      ConnectionFactory rabbitConnectionFactory) {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(rabbitConnectionFactory);

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        List<ConnectionListener> connectionListenerList = new ArrayList<>();
        connectionListenerList.add(connectionListener());

        connectionFactory.setConnectionListeners(connectionListenerList);
        connectionFactory.setAddresses(amqpSettings.getServer());

        return connectionFactory;
    }

    @Bean
    public MailUtil mailUtil(SMTPSettings smtpSettings) {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(smtpSettings.getFrom());
        mailUtil.setSmtpTo(smtpSettings.getTo());
        mailUtil.setSmtpHost(smtpSettings.getHost());
        mailUtil.setSmtpSend(smtpSettings.getSend());
        return mailUtil;
    }

    @Bean
    public MessageFactory messageFactory(IngestSettings chronopolisSettings) {
        MessageFactory messageFactory = new MessageFactory(chronopolisSettings);
        return messageFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(AMQPSettings amqpSettings,
                                         CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange(amqpSettings.getExchange());
        template.setConnectionFactory(connectionFactory);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public TopicProducer producer(RabbitTemplate rabbitTemplate) {
        return new TopicProducer(rabbitTemplate);
    }

    @Bean
    public PackageReadyProcessor packageReadyProcessor(MessageFactory messageFactory,
                                                       TopicProducer producer,
                                                       MailUtil mailUtil,
                                                       IngestSettings settings) {
        return new PackageReadyProcessor(producer,
                settings,
                messageFactory,
                manager,
                mailUtil);
    }

    @Bean
    public CollectionInitCompleteProcessor collectionInitCompleteProcessor(MessageFactory messageFactory,
                                                                           TopicProducer producer,
                                                                           MailUtil mailUtil,
                                                                           IngestSettings settings) {
        return new CollectionInitCompleteProcessor(producer,
                messageFactory,
                manager,
                mailUtil);
    }

    @Bean
    public CollectionInitReplyProcessor collectionInitReplyProcessor(MessageFactory messageFactory,
                                                                     TopicProducer producer,
                                                                     IngestSettings settings) {
        return new CollectionInitReplyProcessor(
                producer,
                messageFactory,
                manager
        );
    }

    @Bean
    public PackageIngestStatusQueryProcessor packageIngestStatusQueryProcessor(TopicProducer producer) {
        return new PackageIngestStatusQueryProcessor(producer);
    }

    @Bean
    public CollectionRestoreRequestProcessor collectionRestoreRequestProcessor(MessageFactory messageFactory,
                                                                               TopicProducer producer,
                                                                               CollectionRestore collectionRestore,
                                                                               IngestSettings settings,
                                                                               MailUtil mailUtil) {
        return new CollectionRestoreRequestProcessor(producer,
                settings,
                messageFactory,
                collectionRestore,
                restoreRepository,
                mailUtil);
    }

    @Bean
    public CollectionRestoreCompleteProcessor collectionRestoreCompleteProcessor(ChronProducer producer,
                                                                                 MessageFactory messageFactory,
                                                                                 RestoreRepository restoreRepository) {
        return new CollectionRestoreCompleteProcessor(producer,
                messageFactory,
                restoreRepository);
    }

    @Bean
    public CollectionRestoreReplyProcessor collectionRestoreReplyProcessor(IngestSettings settings,
                                                                           ChronProducer producer,
                                                                           MessageFactory messageFactory,
                                                                           RestoreRepository restoreRepository) {
        return new CollectionRestoreReplyProcessor(settings,
                producer,
                messageFactory,
                restoreRepository
        );
    }

    @Bean
    public CollectionRestore collectionRestore(IngestSettings settings) {
        return new LocalRestore(Paths.get(settings.getPreservation()),
                Paths.get(settings.getRestore()));
    }

    @Bean
    public MessageListener messageListener(PackageReadyProcessor packageReadyProcessor,
                                           PackageIngestStatusQueryProcessor packageIngestStatusQueryProcessor,
                                           CollectionInitCompleteProcessor collectionInitCompleteProcessor,
                                           CollectionInitReplyProcessor collectionInitReplyProcessor,
                                           CollectionRestoreReplyProcessor collectionRestoreReplyProcessor,
                                           CollectionRestoreCompleteProcessor collectionRestoreCompleteProcessor,
                                           CollectionRestoreRequestProcessor collectionRestoreRequestProcessor) {
        return new IngestMessageListener(packageIngestStatusQueryProcessor,
                packageReadyProcessor,
                collectionInitCompleteProcessor,
                collectionInitReplyProcessor,
                collectionRestoreRequestProcessor,
                collectionRestoreReplyProcessor,
                collectionRestoreCompleteProcessor);
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("chronopolis-control");
    }

    @Bean
    Queue broadcastQueue(IngestSettings settings) {
        return new Queue(settings.getBroadcastQueueName(), true);
    }

    @Bean
    Queue directIngestQueue(IngestSettings settings) {
        return new Queue(settings.getDirectQueueName(), true);
    }

    @Bean
    Binding broadcastBinding(Queue broadcastQueue,
                             IngestSettings settings) {
        log.info("Binding queue {} to {}",
                broadcastQueue.getName(),
                settings.getBroadcastQueueBinding());
        return BindingBuilder.bind(broadcastQueue)
                             .to(topicExchange())
                             .with(settings.getBroadcastQueueBinding());
    }

    @Bean
    Binding directIngestBinding(Queue directIngestQueue,
                                IngestSettings settings) {
        log.info("Binding queue {} to {}",
                directIngestQueue.getName(),
                settings.getDirectQueueBinding());
        return BindingBuilder.bind(directIngestQueue)
                             .to(topicExchange())
                             .with(settings.getDirectQueueBinding());
    }

    @Bean
    RabbitAdmin rabbitAdmin(Queue broadcastQueue,
                            Queue directIngestQueue,
                            Binding broadcastBinding,
                            Binding directIngestBinding,
                            CachingConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // our exchange
        admin.declareExchange(topicExchange());
        // our queues
        log.info("Declaring queues {} and {}",
                broadcastQueue.getName(),
                directIngestQueue.getName());
        admin.declareQueue(broadcastQueue);
        admin.declareQueue(directIngestQueue);
        // our bindings
        log.info("Declaring bindings {} and {}",
                broadcastBinding.getRoutingKey(),
                directIngestBinding.getRoutingKey());
        admin.declareBinding(broadcastBinding);
        admin.declareBinding(directIngestBinding);
        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer(MessageListener messageListener,
                                                                  CachingConnectionFactory connectionFactory,
                                                                  IngestSettings settings) {
        // String testQueueName = env.getProperty(PROPERTIES_RABBIT_TEST_QUEUE_NAME);
        String broadcastQueueName = settings.getBroadcastQueueName();
        String directQueueName = settings.getDirectQueueName();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(broadcastQueueName, directQueueName);
        container.setMessageListener(messageListener);
        container.afterPropertiesSet();
        return container;
    }


}

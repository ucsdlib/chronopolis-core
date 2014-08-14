package org.chronopolis.ingest.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.restore.CollectionRestore;
import org.chronopolis.common.restore.LocalRestore;
import org.chronopolis.common.settings.ChronopolisSettings;
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
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shake on 4/10/14.
 */
@Configuration
@PropertySource({"file:ingest.properties"})
@Import(IngestJPAConfiguration.class)
public class IngestConfiguration {
    public static final String PROPERTIES_SMTP_HOST = "smtp.host";
    public static final String PROPERTIES_SMTP_FROM = "smtp.from";
    public static final String PROPERTIES_SMTP_TO = "smtp.to";

    private final Logger log = LoggerFactory.getLogger(IngestConfiguration.class);

    @Autowired
    public DatabaseManager manager;

    @Autowired
    public RestoreRepository restoreRepository;

    @Resource
    public Environment env;

    @Bean
    public ConnectionListenerImpl connectionListener() {
        return new ConnectionListenerImpl();
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        // Move to properties
        connectionFactory.setRequestedHeartbeat(60);
        connectionFactory.setConnectionTimeout(300);

        String virtualHost = env.getProperty("node.virtual.host");
        if (virtualHost == null) {
            System.out.println("Using default virtual host");
            virtualHost = "chronopolis";
        }

        connectionFactory.setVirtualHost(virtualHost);

        return connectionFactory;
    }


    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(rabbitConnectionFactory());

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        List<ConnectionListener> connectionListenerList = new ArrayList<>();
        connectionListenerList.add(connectionListener());

        connectionFactory.setConnectionListeners(connectionListenerList);
        connectionFactory.setAddresses("adapt-mq.umiacs.umd.edu");

        return connectionFactory;
    }

    @Bean
    public MailUtil mailUtil() {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(env.getProperty(PROPERTIES_SMTP_FROM));
        mailUtil.setSmtpTo(env.getProperty(PROPERTIES_SMTP_TO));
        mailUtil.setSmtpHost(env.getProperty(PROPERTIES_SMTP_HOST));

        return mailUtil;
    }

    @Bean
    public MessageFactory messageFactory(ChronopolisSettings chronopolisSettings) {
        MessageFactory messageFactory = new MessageFactory(chronopolisSettings);
        return messageFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange("chronopolis-control");
        template.setConnectionFactory(connectionFactory());
        template.setMandatory(true);
        return template;
    }

    @Bean
    public TopicProducer producer() {
        return new TopicProducer(rabbitTemplate());
    }

    @Bean
    public PackageReadyProcessor packageReadyProcessor(MessageFactory messageFactory,
                                                       IngestSettings settings) {
        return new PackageReadyProcessor(producer(),
                settings,
                messageFactory,
                manager,
                mailUtil());
    }

    @Bean
    public CollectionInitCompleteProcessor collectionInitCompleteProcessor(MessageFactory messageFactory,
                                                                           IngestSettings settings) {
        return new CollectionInitCompleteProcessor(producer(),
                messageFactory,
                manager,
                mailUtil());
    }

    @Bean
    public CollectionInitReplyProcessor collectionInitReplyProcessor(MessageFactory messageFactory,
                                                                     IngestSettings settings) {
        return new CollectionInitReplyProcessor(
                producer(),
                messageFactory,
                manager
        );
    }

    @Bean
    public PackageIngestStatusQueryProcessor packageIngestStatusQueryProcessor() {
        return new PackageIngestStatusQueryProcessor(producer());
    }

    @Bean
    public CollectionRestoreRequestProcessor collectionRestoreRequestProcessor(MessageFactory messageFactory,
                                                                               CollectionRestore collectionRestore,
                                                                               IngestSettings settings) {
        return new CollectionRestoreRequestProcessor(producer(),
                settings,
                messageFactory,
                collectionRestore,
                restoreRepository);
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
                                                                           MessageFactory messageFactory) {
        return new CollectionRestoreReplyProcessor(settings,
                producer,
                messageFactory
        );
    }

    @Bean
    public CollectionRestore collectionRestore(IngestSettings settings) {
        return new LocalRestore(Paths.get(settings.getPreservation()),
                Paths.get(settings.getRestore()));
    }

    @Bean
    public MessageListener messageListener(PackageReadyProcessor packageReadyProcessor,
                                           CollectionInitCompleteProcessor collectionInitCompleteProcessor,
                                           CollectionInitReplyProcessor collectionInitReplyProcessor,
                                           CollectionRestoreReplyProcessor collectionRestoreReplyProcessor,
                                           CollectionRestoreCompleteProcessor collectionRestoreCompleteProcessor,
                                           CollectionRestoreRequestProcessor collectionRestoreRequestProcessor) {
        return new IngestMessageListener(packageIngestStatusQueryProcessor(),
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
                            Binding directIngestBinding) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
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
                                                                  IngestSettings settings) {
        // String testQueueName = env.getProperty(PROPERTIES_RABBIT_TEST_QUEUE_NAME);
        String broadcastQueueName = settings.getBroadcastQueueName();
        String directQueueName = settings.getDirectQueueName();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(broadcastQueueName, directQueueName);
        container.setMessageListener(messageListener);
        container.afterPropertiesSet();
        return container;
    }


}

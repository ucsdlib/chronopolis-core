package org.chronopolis.ingest.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.restore.CollectionRestore;
import org.chronopolis.common.restore.LocalRestore;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.ingest.IngestMessageListener;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.processor.CollectionInitCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionInitReplyProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreRequestProcessor;
import org.chronopolis.ingest.processor.PackageIngestStatusQueryProcessor;
import org.chronopolis.ingest.processor.PackageReadyProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
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
import java.util.Collections;
import java.util.List;

import static org.chronopolis.ingest.IngestProperties.PROPERTIES_BROADCAST_ROUTING_KEY;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_DPN_PUSH;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_EXCHANGE;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_EXTERNAL_USER;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_IMS_HOST_NAME;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_INBOUND_ROUTING_KEY;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_NODE_NAME;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_STAGE;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_STORAGE_SERVER;
import static org.chronopolis.ingest.IngestProperties.PROPERTIES_TOKEN_STAGE;

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
    public static final String PROPERTIES_CHRON_NODES = "chron.nodes";
    public static final String PROPERTIES_PRES_STORAGE = "node.storage.preservation";

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
    public IngestProperties ingestProperties() {
        List<String> chronNodes = new ArrayList<>();
        String fromProps = env.getProperty(PROPERTIES_CHRON_NODES);
        if (fromProps != null) {
            Collections.addAll(chronNodes, fromProps.split(","));
        }

        IngestProperties properties = new IngestProperties(
                env.getProperty(PROPERTIES_NODE_NAME),
                env.getProperty(PROPERTIES_STAGE),
                env.getProperty(PROPERTIES_EXCHANGE),
                env.getProperty(PROPERTIES_INBOUND_ROUTING_KEY),
                env.getProperty(PROPERTIES_BROADCAST_ROUTING_KEY),
                env.getProperty(PROPERTIES_TOKEN_STAGE),
                env.getProperty(PROPERTIES_IMS_HOST_NAME),
                env.getProperty(PROPERTIES_STORAGE_SERVER),
                env.getProperty(PROPERTIES_EXTERNAL_USER),
                env.getProperty(PROPERTIES_PRES_STORAGE),
                env.getProperty(PROPERTIES_DPN_PUSH, Boolean.class),
                chronNodes);

        return properties;
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
    public MessageFactory messageFactory() {
        MessageFactory messageFactory = new MessageFactory(ingestProperties());
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
    public PackageReadyProcessor packageReadyProcessor() {
        return new PackageReadyProcessor(producer(),
                ingestProperties(),
                messageFactory(),
                manager,
                mailUtil());
    }

    @Bean
    public CollectionInitCompleteProcessor collectionInitCompleteProcessor() {
        return new CollectionInitCompleteProcessor(producer(),
                ingestProperties(),
                messageFactory(),
                manager,
                mailUtil());
    }

    @Bean
    public CollectionInitReplyProcessor collectionInitReplyProcessor() {
        return new CollectionInitReplyProcessor(ingestProperties(),
                producer(),
                messageFactory(),
                manager
        );
    }

    @Bean
    public PackageIngestStatusQueryProcessor packageIngestStatusQueryProcessor() {
        return new PackageIngestStatusQueryProcessor(producer());
    }

    @Bean
    public CollectionRestoreRequestProcessor collectionRestoreRequestProcessor() {
        return new CollectionRestoreRequestProcessor(producer(),
                ingestProperties(),
                messageFactory(),
                collectionRestore(),
                restoreRepository);
    }

    @Bean
    public CollectionRestore collectionRestore() {
        return new LocalRestore(Paths.get(ingestProperties().getPreservation()),
                Paths.get(ingestProperties().getStage()));
    }

    @Bean
    public MessageListener messageListener() {
        return new IngestMessageListener(packageIngestStatusQueryProcessor(),
                packageReadyProcessor(),
                collectionInitCompleteProcessor(),
                collectionInitReplyProcessor(),
                collectionRestoreRequestProcessor());
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("chronopolis-control");
    }

    /*
    @Bean
    Queue testQueue() {
        return new Queue(env.getProperty(PROPERTIES_RABBIT_TEST_QUEUE_NAME), true);
    }

    @Bean
    Binding testBinding() {
        return BindingBuilder.bind(testQueue())
                .to(topicExchange())
                .with(env.getProperty(PROPERTIES_RABBIT_TEST_BINDING_NAME));
    }
    */

    @Bean
    Queue broadcastQueue() {
        return new Queue(ingestProperties().getBroadcastQueueName(), true);
    }

    @Bean
    Queue directIngestQueue() {
        return new Queue(ingestProperties().getDirectQueueName(), true);
    }

    @Bean
    Binding broadcastBinding() {
        return BindingBuilder.bind(broadcastQueue())
                             .to(topicExchange())
                             .with(ingestProperties().getBroadcastQueueBinding());
    }

    @Bean
    Binding directIngestBinding() {
        return BindingBuilder.bind(directIngestQueue())
                             .to(topicExchange())
                             .with(ingestProperties().getDirectQueueBinding());
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        // our exchange
        admin.declareExchange(topicExchange());
        // our queues
        // admin.declareQueue(testQueue());
        admin.declareQueue(broadcastQueue());
        admin.declareQueue(directIngestQueue());
        // our bindings
        // admin.declareBinding(testBinding());
        admin.declareBinding(broadcastBinding());
        admin.declareBinding(directIngestBinding());
        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer() {
        // String testQueueName = env.getProperty(PROPERTIES_RABBIT_TEST_QUEUE_NAME);
        String broadcastQueueName = ingestProperties().getBroadcastQueueName();
        String directQueueName = ingestProperties().getDirectQueueName();
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(broadcastQueueName, directQueueName);
        container.setMessageListener(messageListener());
        return container;
    }


}

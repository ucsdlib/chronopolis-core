package org.chronopolis.ingest.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.ingest.IngestMessageListener;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.processor.CollectionInitCompleteProcessor;
import org.chronopolis.ingest.processor.PackageIngestStatusQueryProcessor;
import org.chronopolis.ingest.processor.PackageReadyProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shake on 4/10/14.
 */
@Configuration
@PropertySource("classpath:ingest.properties")
public class IngestConfiguration {
    public static final String PROPERTIES_NODE_NAME = "nodeName";
    public static final String PROPERTIES_STAGE = "stage";
    public static final String PROPERTIES_EXCHANGE = "exchange";
    public static final String PROPERTIES_INBOUND_ROUTING_KEY = "inboundRoutingKey";
    public static final String PROPERTIES_INGEST_BROADCAST_ROUTING_KEY = "ingestBroadcastRoutingKey";
    public static final String PROPERTIES_TOKEN_STAGE = "tokenStage";
    public static final String PROPERTIES_IMS_HOST_NAME = "imsHostName";
    public static final String PROPERTIES_STORAGE_SERVER = "storageServer";
    public static final String PROPERTIES_EXTERNAL_USER = "externalUser";
    public static final String PROPERTIES_DPN_PUSH = "dpnPush";

    @Resource
    Environment env;

    @Bean
    public ConnectionListenerImpl connectionListener(){
        return new ConnectionListenerImpl();
    }

    @Bean
    public IngestProperties ingestProperties() {
        IngestProperties properties = new IngestProperties(
                env.getProperty(PROPERTIES_NODE_NAME),
                env.getProperty(PROPERTIES_STAGE),
                env.getProperty(PROPERTIES_EXCHANGE),
                env.getProperty(PROPERTIES_INBOUND_ROUTING_KEY),
                env.getProperty(PROPERTIES_INGEST_BROADCAST_ROUTING_KEY),
                env.getProperty(PROPERTIES_TOKEN_STAGE),
                env.getProperty(PROPERTIES_IMS_HOST_NAME),
                env.getProperty(PROPERTIES_STORAGE_SERVER),
                env.getProperty(PROPERTIES_EXTERNAL_USER),
                env.getProperty(PROPERTIES_DPN_PUSH, Boolean.class));

        return properties;
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        // Move to properties
        connectionFactory.setRequestedHeartbeat(60);
        connectionFactory.setConnectionTimeout(300);
        connectionFactory.setVirtualHost("chronopolis");
        return connectionFactory;
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitConnectionFactory());

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        List<ConnectionListener> connectionListenerList = new ArrayList<>();
        connectionListenerList.add(connectionListener());

        connectionFactory.setConnectionListeners(connectionListenerList);
        connectionFactory.setAddresses("adapt-mq.umiacs.umd.edu");

        return connectionFactory;
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
        return new PackageReadyProcessor(producer(), ingestProperties(), messageFactory());
    }

    @Bean
    public CollectionInitCompleteProcessor collectionInitCompleteProcessor() {
        return new CollectionInitCompleteProcessor(producer(), messageFactory());
    }

    @Bean
    public PackageIngestStatusQueryProcessor packageIngestStatusQueryProcessor() {
        return new PackageIngestStatusQueryProcessor(producer());
    }

    @Bean
    public MessageListener messageListener() {
        return new IngestMessageListener(packageIngestStatusQueryProcessor(), packageReadyProcessor(), collectionInitCompleteProcessor());
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("chronopolis-control");
    }

    @Bean
    Queue testQueue() {
        return new Queue(env.getProperty("queue.test.name"), true);
    }

    @Bean
    Queue broadcastQueue() {
        return new Queue(env.getProperty("queue.broadcast.name"), true);
    }

    @Bean
    Queue directIngestQueue() {
        return new Queue(env.getProperty("queue.direct-ingest.name"), true);
    }


    @Bean
    Binding testBinding() {
        return BindingBuilder.bind(testQueue()).to(topicExchange()).with(env.getProperty("queue.test.pattern"));
    }

    @Bean
    Binding broadcastBinding() {
        return BindingBuilder.bind(broadcastQueue()).to(topicExchange()).with(env.getProperty("queue.broadcast.pattern"));
    }

    @Bean
    Binding directIngestBinding() {
        return BindingBuilder.bind(directIngestQueue()).to(topicExchange()).with(env.getProperty("queue.direct-ingest.pattern"));
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        // our exchange
        admin.declareExchange(topicExchange());
        // our queues
        admin.declareQueue(testQueue());
        admin.declareQueue(broadcastQueue());
        admin.declareQueue(directIngestQueue());
        // our bindings
        admin.declareBinding(testBinding());
        admin.declareBinding(broadcastBinding());
        admin.declareBinding(directIngestBinding());
        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer() {
        String testQueueName = env.getProperty("queue.test.name");
        String testBName = env.getProperty("queue.broadcast.name");
        String testIName = env.getProperty("queue.direct-ingest.name");
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(testQueueName, testBName, testIName);
        container.setMessageListener(messageListener());
        return container;
    }


}

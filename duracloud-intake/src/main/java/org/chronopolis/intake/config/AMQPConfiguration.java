package org.chronopolis.intake.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.amqp.error.ErrorHandlerImpl;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.intake.processor.CollectionRestoreReplyProcessor;
import org.chronopolis.intake.processor.PackageIngestCompleteProcessor;
import org.chronopolis.intake.rest.DuracloudMessageListener;
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

/**
 * Created by shake on 8/6/14.
 */
@Configuration
public class AMQPConfiguration {
    private final Logger log = LoggerFactory.getLogger(AMQPConfiguration.class);

    @Autowired
    AMQPSettings amqpSettings;

    @Autowired
    ChronopolisSettings settings;

    @Bean
    GenericProperties properties() {
        // TODO: Deprecate use of GenericProperties
        return new GenericProperties(settings.getNode(),
                settings.getBagStage(),
                amqpSettings.getExchange(),
                settings.getNode() + "-dura-inbound",
                RoutingKey.INGEST_BROADCAST.asRoute());
    }

    @Bean
    MessageFactory messageFactory(GenericProperties properties) {
        return new MessageFactory(properties);
    }

    @Bean
    TopicProducer topicProducer(RabbitTemplate rabbitTemplate) {
        return new TopicProducer(rabbitTemplate);
    }

    @Bean
    ConnectionListener connectionListener() {
        return new ConnectionListenerImpl();
    }

    @Bean
    ConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setRequestedHeartbeat(60);
        connectionFactory.setConnectionTimeout(300);

        connectionFactory.setVirtualHost(amqpSettings.getVirtualHost());

        return connectionFactory;
    }

    @Bean
    CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitConnectionFactory());

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        connectionFactory.addConnectionListener(connectionListener());
        connectionFactory.setAddresses(amqpSettings.getAddress());

        return connectionFactory;
    }

    @Bean
    RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange(amqpSettings.getExchange());
        template.setConnectionFactory(connectionFactory());
        template.setMandatory(true);
        return template;
    }

    @Bean
    ChronProducer producer() {
        return new TopicProducer(rabbitTemplate());
    }

    @Bean
    CollectionRestoreReplyProcessor collectionRestoreReplyProcessor() {
        return new CollectionRestoreReplyProcessor();
    }

    @Bean
    PackageIngestCompleteProcessor packageIngestCompleteProcessor() {
        return new PackageIngestCompleteProcessor();
    }

    @Bean
    MessageListener messageListener() {
        return new DuracloudMessageListener(collectionRestoreReplyProcessor(),
                packageIngestCompleteProcessor());
    }

    @Bean
    ErrorHandlerImpl errorHandler() {
        return new ErrorHandlerImpl();
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange(amqpSettings.getExchange());
    }

    /////////////////////////////
    // Rabbit MQ Queues/Bindings
    /////////////////////////////

    @Bean
    Queue directQueue() {
        return new Queue(settings.getNode() + "-dura-inbound", true);
    }

    @Bean
    Binding directBinding() {
        return BindingBuilder.bind(directQueue())
                             .to(topicExchange())
                             .with(settings.getNode() + "-dura-inbound");
    }

    /////////////////////////////
    // Rabbit MQ Management Beans
    /////////////////////////////

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        admin.declareExchange(topicExchange());

        admin.declareQueue(directQueue());
        admin.declareBinding(directBinding());

        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer() {
        String directQueueName = settings.getNode() + "-dura-inbound";
        String directQueueBinding = settings.getNode() + "-dura-inbound";

        log.info("Direct queue {} bound to {}", directQueueName, directQueueBinding);

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setErrorHandler(errorHandler());
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(directQueueName);
        container.setMessageListener(messageListener());
        return container;
    }

}

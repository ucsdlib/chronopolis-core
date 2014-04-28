package org.chronopolis.intake.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.intake.IntakeMessageListener;
import org.chronopolis.intake.processor.PackageIngestCompleteProcessor;
import org.chronopolis.intake.processor.PackageIngestStatusResponseProcessor;
import org.chronopolis.intake.processor.PackageReadyReplyProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

import static org.chronopolis.common.properties.GenericProperties.*;

/**
 * Created by shake on 4/16/14.
 */
@Configuration
@PropertySource({"file:intake.properties"})
public class IntakeConfig {

    @Resource
    Environment env;

    @Bean
    public GenericProperties properties() {
        return new GenericProperties(
                env.getProperty(PROPERTIES_NODE_NAME),
                env.getProperty(PROPERTIES_STAGE),
                env.getProperty(PROPERTIES_EXCHANGE),
                env.getProperty(PROPERTIES_INBOUND_ROUTING_KEY),
                env.getProperty(PROPERTIES_BROADCAST_ROUTING_KEY));
    }

    @Bean
    public MessageFactory messageFactory() {
        return new MessageFactory(properties());
    }

    @Bean
    public ConnectionListenerImpl connectionListener() {
        return new ConnectionListenerImpl();
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
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

        connectionFactory.addConnectionListener(connectionListener());
        connectionFactory.setAddresses("adapt-mq.umiacs.umd.edu");

        return connectionFactory;
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
    TopicExchange topicExchange() {
        return new TopicExchange("chronopolis-control");
    }

    // Declare our processors and MessageListener

    @Bean
    public PackageIngestCompleteProcessor packageIngestCompleteProcessor() {
        return new PackageIngestCompleteProcessor(producer());
    }

    @Bean
    public PackageIngestStatusResponseProcessor packageIngestStatusResponseProcessor() {
        return new PackageIngestStatusResponseProcessor(producer());
    }

    @Bean
    public PackageReadyReplyProcessor packageReadyReplyProcessor() {
        return new PackageReadyReplyProcessor();
    }

    @Bean
    MessageListener messageListener() {
        return new IntakeMessageListener(
                packageIngestCompleteProcessor(),
                packageIngestStatusResponseProcessor(),
                packageReadyReplyProcessor());
    }

    // Our processor queues + bindings

    // And finish off with the rabbit admin and container used by spring
    /*
    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        admin.declareExchange(topicExchange());

        //admin.declareQueue();

        //admin.declareBinding();

        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        // container.setQueueNames();
        container.setMessageListener(messageListener());
        return container;
    }
    */
}

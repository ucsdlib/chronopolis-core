package org.chronopolis.intake.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.common.settings.AMQPSettings;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.intake.IntakeMessageListener;
import org.chronopolis.intake.processor.PackageIngestCompleteProcessor;
import org.chronopolis.intake.processor.PackageIngestStatusResponseProcessor;
import org.chronopolis.intake.processor.PackageReadyReplyProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by shake on 4/16/14.
 */
@Configuration
public class IntakeConfig {

    @Bean
    public MessageFactory messageFactory(ChronopolisSettings chronopolisSettings) {
        return new MessageFactory(chronopolisSettings);
    }

    @Bean
    public ConnectionListenerImpl connectionListener() {
        return new ConnectionListenerImpl();
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory(AMQPSettings amqpSettings) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setRequestedHeartbeat(60);
        connectionFactory.setConnectionTimeout(300);
        connectionFactory.setVirtualHost(amqpSettings.getVirtualHost());

        return connectionFactory;
    }

    @Bean
    public CachingConnectionFactory connectionFactory(ConnectionFactory rabbitConnectionFactory,
                                                      AMQPSettings settings) {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(rabbitConnectionFactory);

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        connectionFactory.addConnectionListener(connectionListener());
        connectionFactory.setAddresses(settings.getServer());

        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory,
                                         AMQPSettings settings) {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange(settings.getExchange());
        template.setConnectionFactory(connectionFactory);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public TopicProducer producer(RabbitTemplate rabbitTemplate) {
        return new TopicProducer(rabbitTemplate);
    }

    @Bean
    TopicExchange topicExchange(AMQPSettings settings) {
        return new TopicExchange(settings.getExchange());
    }

    // Declare our processors and MessageListener

    @Bean
    public PackageIngestCompleteProcessor packageIngestCompleteProcessor(TopicProducer producer) {
        return new PackageIngestCompleteProcessor(producer);
    }

    @Bean
    public PackageIngestStatusResponseProcessor packageIngestStatusResponseProcessor(TopicProducer producer) {
        return new PackageIngestStatusResponseProcessor(producer);
    }

    @Bean
    public PackageReadyReplyProcessor packageReadyReplyProcessor() {
        return new PackageReadyReplyProcessor();
    }

    @Bean
    MessageListener messageListener(PackageIngestCompleteProcessor packageIngestCompleteProcessor,
                                    PackageIngestStatusResponseProcessor packageIngestStatusResponseProcessor,
                                    PackageReadyReplyProcessor packageReadyReplyProcessor) {
        return new IntakeMessageListener(
                packageIngestCompleteProcessor,
                packageIngestStatusResponseProcessor,
                packageReadyReplyProcessor);
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

package org.chronopolis.replicate.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.amqp.error.ErrorHandlerImpl;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicateMessageListener;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.processor.CollectionInitProcessor;
import org.chronopolis.replicate.processor.FileQueryProcessor;
import org.chronopolis.replicate.processor.FileQueryResponseProcessor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

import static org.chronopolis.replicate.ReplicationProperties.*;

/**
 * Created by shake on 4/16/14.
 */
@Configuration
@PropertySource({"file:replication.properties"})
public class ReplicationConfig {
    public static final String PROPERTIES_SMTP_FROM = "smtp.from";
    public static final String PROPERTIES_SMTP_TO = "smtp.to";
    public static final String PROPERTIES_SMTP_HOST = "smtp.host";

    @Resource
    Environment env;

    @Bean
    ReplicationProperties properties() {
        return new ReplicationProperties(
                env.getProperty(PROPERTIES_NODE_NAME),
                env.getProperty(PROPERTIES_STAGE),
                env.getProperty(PROPERTIES_EXCHANGE),
                env.getProperty(PROPERTIES_INBOUND_ROUTING_KEY),
                env.getProperty(PROPERTIES_BROADCAST_ROUTING_KEY),
                env.getProperty(PROPERTIES_ACE_FQDN),
                env.getProperty(PROPERTIES_ACE_PATH),
                env.getProperty(PROPERTIES_ACE_USER),
                env.getProperty(PROPERTIES_ACE_PASS),
                env.getProperty(PROPERTIES_ACE_PORT, Integer.class));
    }

    @Bean
    MailUtil mailUtil() {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(env.getProperty(PROPERTIES_SMTP_FROM));
        mailUtil.setSmtpTo(env.getProperty(PROPERTIES_SMTP_TO));
        mailUtil.setSmtpHost(env.getProperty(PROPERTIES_SMTP_HOST));
        return mailUtil;
    }

    @Bean
    MessageFactory messageFactory() {
        return new MessageFactory(properties());
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
        connectionFactory.setVirtualHost("chronopolis");

        return connectionFactory;
    }

    @Bean
    CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitConnectionFactory());

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        connectionFactory.addConnectionListener(connectionListener());
        connectionFactory.setAddresses("adapt-mq.umiacs.umd.edu");

        return connectionFactory;
    }

    @Bean
    RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange("chronopolis-control");
        template.setConnectionFactory(connectionFactory());
        template.setMandatory(true);


        return template;
    }

    @Bean
    TopicProducer producer() {
        return new TopicProducer(rabbitTemplate());
    }

    @Bean
    FileQueryProcessor fileQueryProcessor() {
        return new FileQueryProcessor(producer());
    }

    @Bean
    FileQueryResponseProcessor fileQueryResponseProcessor() {
        return new FileQueryResponseProcessor(producer());
    }

    @Bean
    CollectionInitProcessor collectionInitProcessor() {
        return new CollectionInitProcessor(producer(), messageFactory(), properties(), mailUtil());
    }

    @Bean
    MessageListener messageListener() {
        return new ReplicateMessageListener(
                fileQueryProcessor(),
                fileQueryResponseProcessor(),
                collectionInitProcessor());
    }

    @Bean
    ErrorHandlerImpl errorHandler() {
        return new ErrorHandlerImpl();
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
       return new Queue(properties().getBroadcastQueueName(), true);
    }

    @Bean
    Binding broadcastBinding() {
        return BindingBuilder.bind(broadcastQueue())
                             .to(topicExchange())
                             .with(properties().getBroadcastQueueBinding());
    }


    @Bean
    Queue directQueue() {
       return new Queue(properties().getDirectQueueName(), true);
    }

    @Bean
    Binding directBinding() {
        return BindingBuilder.bind(directQueue())
                             .to(topicExchange())
                             .with(properties().getDirectQueueBinding());
    }

    @Bean
    RabbitAdmin rabbitAdmin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        admin.declareExchange(topicExchange());

        // admin.declareQueue(testQueue());
        admin.declareQueue(broadcastQueue());
        admin.declareQueue(directQueue());

        // admin.declareBinding(testBinding());
        admin.declareBinding(broadcastBinding());
        admin.declareBinding(directBinding());

        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer() {
        // String testQueueName = env.getProperty(PROPERTIES_RABBIT_TEST_QUEUE_NAME);
        String broadcastQueueName = properties().getBroadcastQueueName();
        String directQueueName = properties().getDirectQueueName();

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setErrorHandler(errorHandler());
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(broadcastQueueName, directQueueName);
        container.setMessageListener(messageListener());
        return container;
    }

}

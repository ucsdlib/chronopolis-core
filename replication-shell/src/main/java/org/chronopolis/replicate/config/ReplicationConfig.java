package org.chronopolis.replicate.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.amqp.error.ErrorHandlerImpl;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.CredentialRequestInterceptor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicateMessageListener;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.jobs.AceRegisterJobListener;
import org.chronopolis.replicate.jobs.BagDownloadJobListener;
import org.chronopolis.replicate.jobs.TokenStoreDownloadJobListener;
import org.chronopolis.replicate.processor.CollectionInitProcessor;
import org.chronopolis.replicate.processor.FileQueryProcessor;
import org.chronopolis.replicate.processor.FileQueryResponseProcessor;
import org.chronopolis.replicate.util.URIUtil;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
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
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import retrofit.RestAdapter;

import javax.annotation.Resource;

/**
 * Created by shake on 4/16/14.
 */
@Configuration
@PropertySource({"file:replication.properties"})
@Import(PropConfig.class)
public class ReplicationConfig {
    public final Logger log = LoggerFactory.getLogger(ReplicationConfig.class);

    public static final String PROPERTIES_SMTP_FROM = "smtp.from";
    public static final String PROPERTIES_SMTP_TO = "smtp.to";
    public static final String PROPERTIES_SMTP_HOST = "smtp.host";

    @Resource
    Environment env;

    @Autowired
    ReplicationProperties properties;

    @Bean
    AceService aceService() {
        String endpoint = URIUtil.buildAceUri(properties.getAceFqdn(),
                properties.getAcePort(),
                properties.getAcePath()).toString();

        CredentialRequestInterceptor interceptor = new CredentialRequestInterceptor(
                properties.getAceUser(),
                properties.getAcePass());

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setRequestInterceptor(interceptor)
                .build();

        return restAdapter.create(AceService.class);
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
        return new MessageFactory(properties);
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

        String virtualHost = env.getProperty("node.virtual.host");
        if (virtualHost == null) {
            virtualHost = "chronopolis";
        }

        connectionFactory.setVirtualHost(virtualHost);

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

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    Scheduler scheduler() {
        try {
            return StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not create scheduler", e);
        }
    }

    @Bean
    AceRegisterJobListener aceRegisterJobListener() {
        AceRegisterJobListener jobListener = new AceRegisterJobListener(
                "ace-register",
                scheduler(),
                producer(),
                messageFactory(),
                properties,
                mailUtil());

        try {
            scheduler().getListenerManager().addJobListener(jobListener,
                    GroupMatcher.<JobKey>groupEquals("AceRegister"));
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not register listener", e);
        }

        return jobListener;
    }

    @Bean
    BagDownloadJobListener bagDownloadJobListener() {
        BagDownloadJobListener jobListener = new BagDownloadJobListener(
                "bag-download",
                scheduler(),
                properties,
                mailUtil(),
                messageFactory(),
                producer());

        try {
            scheduler().getListenerManager().addJobListener(jobListener,
                    GroupMatcher.<JobKey>groupEquals("BagDownload"));
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not register listener", e);
        }

        return jobListener;
    }

    @Bean
    TokenStoreDownloadJobListener tokenStoreDownloadJobListener() {
        TokenStoreDownloadJobListener jobListener = new TokenStoreDownloadJobListener(
                "token-store-download",
                scheduler(),
                properties,
                mailUtil(),
                messageFactory(),
                producer());

        try {
            scheduler().getListenerManager().addJobListener(jobListener,
                    GroupMatcher.<JobKey>groupEquals("TokenDownload"));
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not register listener", e);
        }

        return jobListener;
    }

    @Bean
    @DependsOn({"tokenStoreDownloadJobListener",
                "bagDownloadJobListener",
                "aceRegisterJobListener"})
    CollectionInitProcessor collectionInitProcessor() {
        return new CollectionInitProcessor(producer(),
                messageFactory(),
                properties,
                mailUtil(),
                scheduler());
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
       return new Queue(properties.getBroadcastQueueName(), true);
    }

    @Bean
    Binding broadcastBinding() {
        return BindingBuilder.bind(broadcastQueue())
                             .to(topicExchange())
                             .with(properties.getBroadcastQueueBinding());
    }


    @Bean
    Queue directQueue() {
       return new Queue(properties.getDirectQueueName(), true);
    }

    @Bean
    Binding directBinding() {
        return BindingBuilder.bind(directQueue())
                             .to(topicExchange())
                             .with(properties.getDirectQueueBinding());
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
        String broadcastQueueName = properties.getBroadcastQueueName();
        String directQueueName = properties.getDirectQueueName();

        log.info("Broadcast queue {} bound to {}", broadcastQueueName, properties.getBroadcastQueueBinding());
        log.info("Direct queue {} bound to {}", directQueueName, properties.getDirectQueueBinding());

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setErrorHandler(errorHandler());
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(broadcastQueueName, directQueueName);
        container.setMessageListener(messageListener());
        return container;
    }

}

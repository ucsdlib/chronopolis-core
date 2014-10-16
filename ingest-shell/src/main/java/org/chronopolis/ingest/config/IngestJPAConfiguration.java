package org.chronopolis.ingest.config;

import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.db.ingest.IngestDB;
import org.chronopolis.db.ingest.ReplicationFlowTable;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by shake on 4/10/14.
 */
@Configuration
@EnableJpaRepositories(basePackages = "org.chronopolis.db",
        includeFilters = @ComponentScan.Filter(value = {IngestDB.class,
                                                        ReplicationFlowTable.class,
                                                        RestoreRepository.class},
                                               type = FilterType.ASSIGNABLE_TYPE))
@EnableTransactionManagement
public class IngestJPAConfiguration {
    private static final String PROPERTIES_JDBC_DRIVER = "jdbc.driver";
    private static final String PROPERTIES_JDBC_URL = "jdbc.url";
    private static final String PROPERTIES_JDBC_USERNAME = "jdbc.username";
    private static final String PROPERTIES_JDBC_PASSWORD = "jdbc.password";

    @Bean
    public DataSource dataSource(IngestDBSettings settings) throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(settings.getDriverClass());
        dataSource.setUrl(settings.getURL());
        dataSource.setUsername(settings.getUsername());
        dataSource.setPassword(settings.getPassword());
        return dataSource;
    }

    @Bean
    public DatabaseManager databaseManager() {
        return new DatabaseManager();
    }

    /*
    @Bean
    public EntityManagerFactory entityManagerFactory() throws SQLException {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabase(Database.H2);
        vendorAdapter.setShowSql(true);

        LocalContainerEntityManagerFactoryBean factory =
                new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.chronopolis.db.model",
                "org.chronopolis.db.common.model");
        factory.setDataSource(dataSource());
        factory.setJpaDialect(vendorAdapter.getJpaDialect());
        factory.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        factory.afterPropertiesSet();

        return factory.getObject();
    }

    @Bean
    public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.createEntityManager();
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws SQLException {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory());
        return txManager;
    }

    @Bean
    public HibernateExceptionTranslator hibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }
    */

}

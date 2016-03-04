package org.chronopolis.ingest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by shake on 4/10/14.
 */
@Configuration
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

    /*
    @Bean
    public DatabaseManager databaseManager() {
        return new DatabaseManager();
    }

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

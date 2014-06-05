/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.db;

import org.chronopolis.db.ingest.IngestDB;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.SQLException;


/**
 *
 * @author shake
 */
@Configuration
@EnableJpaRepositories(basePackages = "org.chronopolis.db",
        includeFilters = @ComponentScan.Filter(value = {IngestDB.class},
                                               type = FilterType.ASSIGNABLE_TYPE))
@EnableTransactionManagement
public class JPAConfiguration {

    @Bean
    public DataSource dataSource() throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("test-h2");
        dataSource.setUsername("h2");
        dataSource.setPassword("h2");
        return dataSource;
    }

    @Bean
    public DatabaseManager databaseManager() {
        return new DatabaseManager();
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() throws SQLException {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factory =
                new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.chronopolis.db.model");
        factory.setDataSource(dataSource());
        factory.setJpaDialect(vendorAdapter.getJpaDialect());
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

}


package com.taxrecordsportal.tax_records_portal_backend.common.config;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    /**
     * Ensures Flyway migrations run before Hibernate schema validation.
     * Spring will initialize the "flyway" bean (and run migrate via initMethod)
     * before creating the EntityManagerFactory.
     */
    @Bean
    public static org.springframework.beans.factory.config.BeanFactoryPostProcessor flywayDependencyPostProcessor() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                beanFactory.getBeanDefinition("entityManagerFactory")
                        .setDependsOn(new String[]{"flyway"});
            }
        };
    }
}

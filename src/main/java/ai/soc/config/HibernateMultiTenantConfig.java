package ai.soc.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class HibernateMultiTenantConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MultiTenantConnectionProvider<String> multiTenantConnectionProvider;

    @Autowired
    private CurrentTenantIdentifierResolver tenantIdentifierResolver;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder) {

        Map<String, Object> hibernateProps = new HashMap<>();
        hibernateProps.put("hibernate.multiTenancy", "SCHEMA");
        hibernateProps.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        hibernateProps.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        hibernateProps.put(AvailableSettings.HBM2DDL_AUTO, "none");
        hibernateProps.put(AvailableSettings.SHOW_SQL, true);

        return builder
                .dataSource(dataSource)
                .packages("ai.soc.entity") // adjust to your entity package
                .properties(hibernateProps)
                .build();
    }
}
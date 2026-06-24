package com.shipmate.api.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("docker")
public class FlywayDockerMigrationConfig {
    @Bean
    ApplicationRunner flywayMigrationRunner(DataSource dataSource) {
        return args -> Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .load()
                .migrate();
    }
}

package com.storagehub.entity;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.mapping.Column;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The entity side of the V1__init_schema.sql activity_logs contract (story
 * 1.2), checked WITHOUT a database so it runs on every machine: Hibernate's
 * physical column names for ActivityLog under
 * PhysicalNamingStrategyStandardImpl must equal the frozen PascalCase DDL
 * names verbatim, and application.yml must keep the strategy configured -
 * Boot's default camelCase->snake_case strategy would rename EntityType to
 * entity_type, which only surfaces as a ddl-auto: validate failure on the
 * next real boot. The env-gated persistence tests stay the deeper layer.
 */
class ActivityLogColumnMappingTests {

    @Test
    void physicalColumnNamesMatchTheFrozenV1Ddl() {
        try (StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                // Explicit dialect: metadata binding must not open a connection.
                .applySetting(AvailableSettings.DIALECT, MySQLDialect.class.getName())
                .build()) {
            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(ActivityLog.class)
                    .getMetadataBuilder()
                    .applyPhysicalNamingStrategy(PhysicalNamingStrategyStandardImpl.INSTANCE)
                    .build();

            List<String> columnNames = metadata.getEntityBinding(ActivityLog.class.getName())
                    .getTable().getColumns().stream()
                    .map(Column::getName)
                    .toList();

            assertThat(columnNames).containsExactlyInAnyOrder(
                    "LogID", "ActorID", "EntityType", "EntityID", "Action",
                    "FromValue", "ToValue", "Reason");
        }
    }

    @Test
    void applicationYmlKeepsTheVerbatimPhysicalNamingStrategy() {
        assertThat(readApplicationYml()).contains(
                "physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
    }

    private static String readApplicationYml() {
        try (InputStream in = ActivityLogColumnMappingTests.class.getClassLoader()
                .getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml on the test classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed reading application.yml from the classpath", ex);
        }
    }
}

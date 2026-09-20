package ru.npyatak.itemsStorage.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @author natalapatak
 * @since 20.09.2026
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.isBlank()) {
            // Локальная разработка
            return DataSourceBuilder.create()
                    .url("jdbc:postgresql://localhost:5432/dbname")
                    .username("username")
                    .password("password")
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }

        // Формат: postgresql://user:password@host:port/database?params
        String withoutScheme = databaseUrl.replace("postgresql://", "");

        // user:password  |  host:port/database?params
        int atIndex = withoutScheme.indexOf('@');
        String credentials = withoutScheme.substring(0, atIndex);
        String hostAndRest = withoutScheme.substring(atIndex + 1);

        int colonIndex = credentials.indexOf(':');
        String username = credentials.substring(0, colonIndex);
        String password = credentials.substring(colonIndex + 1);

        // Отрезаем query-параметры
        int queryIndex = hostAndRest.indexOf('?');
        String hostDb = queryIndex >= 0
                ? hostAndRest.substring(0, queryIndex)
                : hostAndRest;

        // host:port  |  database
        int slashIndex = hostDb.indexOf('/');
        String hostPort = slashIndex >= 0
                ? hostDb.substring(0, slashIndex)
                : hostDb;
        String database = slashIndex >= 0
                ? hostDb.substring(slashIndex + 1)
                : "";

        int portColon = hostPort.indexOf(':');
        String host = portColon >= 0
                ? hostPort.substring(0, portColon)
                : hostPort;
        String port = portColon >= 0
                ? hostPort.substring(portColon + 1)
                : "5432";

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}

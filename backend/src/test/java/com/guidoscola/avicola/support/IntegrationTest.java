package com.guidoscola.avicola.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base class for tests that run against the local PostgreSQL test database.
 * The schema is rebuilt once per context (see {@link TestDatabaseConfig}) and data is wiped before each test.
 */
@SpringBootTest
public abstract class IntegrationTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void wipeData() {
        jdbc.execute("TRUNCATE TABLE users, roles CASCADE");
    }
}

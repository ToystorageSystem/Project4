package com.toystorage.backend.dev;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
@ConditionalOnProperty(
        name = "app.dev-seed.enabled",
        havingValue = "true"
)
public class FullDatabaseDevSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev-seed.reset:true}")
    private boolean reset;

    public FullDatabaseDevSeeder(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            PasswordEncoder passwordEncoder
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        System.out.println();
        System.out.println("====================================================");
        System.out.println(" TOYSTORAGE FULL DEV DATABASE SEEDER");
        System.out.println("====================================================");

        /*
         * Patch schema trước.
         *
         * Ví dụ:
         * discrepancy_reports.reference_type
         * cần hỗ trợ STORE_RETURN.
         */
        patchDevSchema();

        /*
         * Nếu reset = true:
         * xóa sạch dữ liệu tất cả table.
         */
        if (reset) {
            resetAllTables();
        }

        /*
         * Seed toàn bộ database.
         */
        runSeedScript();

        /*
         * Mã hóa password 123456 cho user DEV.
         */
        encodeDevPasswords();

        /*
         * Kiểm tra tất cả table có dữ liệu.
         */
        verifyAllTablesHaveData();

        System.out.println("====================================================");
        System.out.println(" FULL DATABASE SEED COMPLETED");
        System.out.println(" Default DEV password: 123456");
        System.out.println("====================================================");
        System.out.println();
    }


    // =====================================================
    // PATCH DEV SCHEMA
    // =====================================================

    private void patchDevSchema() {

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator();

        populator.setContinueOnError(false);

        populator.addScript(
                new ClassPathResource(
                        "dev/schema-dev-patch.sql"
                )
        );

        populator.execute(dataSource);

        System.out.println(
                "Development schema compatibility patch applied."
        );
    }


    // =====================================================
    // RESET ALL TABLES
    // =====================================================

    private void resetAllTables() {

        /*
         * QUAN TRỌNG:
         *
         * FOREIGN_KEY_CHECKS là SESSION VARIABLE của MySQL.
         *
         * Vì vậy phải dùng CÙNG MỘT CONNECTION
         * cho:
         *
         * SET FOREIGN_KEY_CHECKS = 0
         * TRUNCATE...
         * SET FOREIGN_KEY_CHECKS = 1
         *
         * Không nên gọi jdbcTemplate.execute()
         * từng câu riêng biệt.
         */

        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            List<String> tables =
                    new ArrayList<>();


            // =================================================
            // GET ALL TABLES
            // =================================================

            try (
                    ResultSet resultSet =
                            statement.executeQuery(
                                    """
                                    SELECT table_name
                                    FROM information_schema.tables
                                    WHERE table_schema = DATABASE()
                                      AND table_type = 'BASE TABLE'
                                    ORDER BY table_name
                                    """
                            )
            ) {

                while (
                        resultSet.next()
                ) {

                    tables.add(
                            resultSet.getString(
                                    "table_name"
                            )
                    );
                }
            }


            System.out.println(
                    "Found "
                            + tables.size()
                            + " tables."
            );


            // =================================================
            // DISABLE FK
            // =================================================

            statement.execute(
                    "SET FOREIGN_KEY_CHECKS = 0"
            );


            try {

                for (
                        String table : tables
                ) {

                    /*
                     * Safety check.
                     */
                    if (
                            !table.matches(
                                    "[A-Za-z0-9_]+"
                            )
                    ) {

                        throw new IllegalStateException(
                                "Unsafe table name: "
                                        + table
                        );
                    }


                    System.out.println(
                            "TRUNCATE -> "
                                    + table
                    );


                    statement.execute(
                            "TRUNCATE TABLE `"
                                    + table
                                    + "`"
                    );
                }

            } finally {

                // =============================================
                // ALWAYS ENABLE FK AGAIN
                // =============================================

                statement.execute(
                        "SET FOREIGN_KEY_CHECKS = 1"
                );
            }


            System.out.println(
                    "Reset complete: "
                            + tables.size()
                            + " tables truncated."
            );

        } catch (
                Exception e
        ) {

            throw new IllegalStateException(
                    "Failed to reset development database",
                    e
            );
        }
    }


    // =====================================================
    // RUN FULL SEED SQL
    // =====================================================

    private void runSeedScript() {

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator();


        populator.setContinueOnError(
                false
        );


        populator.setIgnoreFailedDrops(
                false
        );


        populator.addScript(
                new ClassPathResource(
                        "dev/full-seed.sql"
                )
        );


        populator.execute(
                dataSource
        );


        System.out.println(
                "Seed SQL executed."
        );
    }


    // =====================================================
    // PASSWORD
    // =====================================================

    private void encodeDevPasswords() {

        String encoded =
                passwordEncoder.encode(
                        "123456"
                );


        int updated =
                jdbcTemplate.update(
                        """
                        UPDATE users
                        SET password = ?
                        WHERE user_code LIKE 'DEV%'
                        """,
                        encoded
                );


        System.out.println(
                "Encoded password updated for "
                        + updated
                        + " DEV users."
        );
    }


    // =====================================================
    // VERIFY
    // =====================================================

    private void verifyAllTablesHaveData() {

        List<String> tables =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_type = 'BASE TABLE'
                        ORDER BY table_name
                        """,
                        String.class
                );


        int populated =
                0;


        List<String> emptyTables =
                new ArrayList<>();


        for (
                String table : tables
        ) {

            if (
                    !table.matches(
                            "[A-Za-z0-9_]+"
                    )
            ) {

                continue;
            }


            Long count =
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM `"
                                    + table
                                    + "`",
                            Long.class
                    );


            if (
                    count != null
                            &&
                            count > 0
            ) {

                populated++;

            } else {

                emptyTables.add(
                        table
                );
            }
        }


        System.out.println();
        System.out.println(
                "================ SEED VERIFY ================"
        );


        System.out.println(
                "Seed verification: "
                        + populated
                        + "/"
                        + tables.size()
                        + " tables contain data."
        );


        if (
                !emptyTables.isEmpty()
        ) {

            System.out.println(
                    "EMPTY TABLES:"
            );


            for (
                    String table : emptyTables
            ) {

                System.out.println(
                        " - "
                                + table
                );
            }

        } else {

            System.out.println(
                    "OK: All tables contain seed data."
            );
        }


        System.out.println(
                "============================================="
        );

        System.out.println();
    }
}
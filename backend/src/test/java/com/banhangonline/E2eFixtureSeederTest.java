package com.banhangonline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("e2e")
@EnabledIfSystemProperty(named = "runLiveDbTests", matches = "true")
class E2eFixtureSeederTest {
    private static final String FIXTURE_PASSWORD = "E2ePassw0rd!2026";

    private static final List<String> RESET_TABLES = List.of(
            "sessions",
            "password_reset_tokens",
            "verification_codes",
            "user_addresses",
            "audit_logs",
            "notifications",
            "payments",
            "pickup",
            "order_status_history",
            "order_items",
            "orders",
            "cart_items",
            "carts",
            "inventory",
            "store_staff_permissions",
            "store_staff",
            "store_registration_requests",
            "user_roles",
            "products",
            "categories",
            "stores",
            "users");

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void resetsAndSeedsOnlyTheDedicatedE2eDatabase() {
        assertIsolatedDatabase();
        RESET_TABLES.forEach(table -> jdbc.update("DELETE FROM " + table));

        long ownerId = insertUser("owner-a", "owner-a@e2e.invalid", "0900000001", "OWNER");
        long staffId = insertUser("staff-a", "staff-a@e2e.invalid", "0900000002", "STAFF");
        long limitedStaffId = insertUser("staff-limited", "staff-limited@e2e.invalid", "0900000003", "STAFF");
        insertUser("customer-a", "customer-a@e2e.invalid", "0900000004", "CUSTOMER");
        insertUser("customer-b", "customer-b@e2e.invalid", "0900000005", "CUSTOMER");

        long storeA = insertStore(ownerId, "E2E Store A", "0901000001");
        long storeB = insertStore(ownerId, "E2E Store B", "0901000002");
        long categoryA = insertCategory("E2E Phones", "e2e-phones");
        long categoryB = insertCategory("E2E Computers", "e2e-computers");
        long categoryC = insertCategory("E2E Audio", "e2e-audio");

        long[] productIds = {
                insertProduct(categoryA, "E2E-PHONE-001", "E2E Phone One", "e2e-phone-one", 1000),
                insertProduct(categoryA, "E2E-PHONE-002", "E2E Phone Two", "e2e-phone-two", 2500),
                insertProduct(categoryB, "E2E-LAPTOP-001", "E2E Laptop One", "e2e-laptop-one", 5000),
                insertProduct(categoryB, "E2E-LAPTOP-002", "E2E Laptop Two", "e2e-laptop-two", 7500),
                insertProduct(categoryC, "E2E-AUDIO-001", "E2E Audio One", "e2e-audio-one", 1200)
        };

        for (int index = 0; index < productIds.length; index++) {
            int stockA = index == 3 ? 2 : index == 4 ? 0 : 12;
            insertInventory(storeA, productIds[index], stockA, index == 3 ? 4 : 2);
            insertInventory(storeB, productIds[index], 20, 3);
        }

        long staffMembership = insertStaffMembership(storeA, staffId);
        long limitedMembership = insertStaffMembership(storeA, limitedStaffId);
        grant(staffMembership, ownerId, "VIEW_PRODUCTS");
        grant(staffMembership, ownerId, "MANAGE_PRODUCTS");
        grant(staffMembership, ownerId, "VIEW_INVENTORY");
        grant(staffMembership, ownerId, "VIEW_ORDERS");
        grant(limitedMembership, ownerId, "VIEW_PRODUCTS");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stores WHERE status='ACTIVE'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM categories", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory WHERE store_id=? AND quantity=0", Integer.class, storeA))
                .isEqualTo(1);
    }

    private void assertIsolatedDatabase() {
        Map<String, Object> identity = jdbc.queryForMap(
                "SELECT DATABASE() AS database_name, @@port AS server_port, @@version_comment AS server_version");
        String jdbcUrl;
        try (var connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify E2E database connection identity", exception);
        }

        assertThat(identity.get("database_name")).isEqualTo("banhangonline_e2e");
        assertThat(((Number) identity.get("server_port")).intValue()).isEqualTo(3307);
        assertThat(identity.get("server_version").toString()).containsIgnoringCase("MariaDB");
        assertThat(jdbcUrl).startsWith("jdbc:mysql://127.0.0.1:3307/banhangonline_e2e");
    }

    private long insertUser(String username, String email, String phone, String roleName) {
        long userId = insertAndReturnId(
                "INSERT INTO users (full_name,username,email,phone,password_hash,status) VALUES (?,?,?,?,?,'ACTIVE')",
                username,
                username,
                email,
                phone,
                passwordEncoder.encode(FIXTURE_PASSWORD));
        long roleId = Objects.requireNonNull(
                jdbc.queryForObject("SELECT id FROM roles WHERE name=?", Long.class, roleName));
        jdbc.update("INSERT INTO user_roles (user_id,role_id) VALUES (?,?)", userId, roleId);
        return userId;
    }

    private long insertStore(long ownerId, String name, String phone) {
        return insertAndReturnId(
                "INSERT INTO stores (owner_user_id,name,phone,province,district,ward,address_detail,status) "
                        + "VALUES (?,?,?,'Ha Noi','Ba Dinh','Phuc Xa','E2E Test Address','ACTIVE')",
                ownerId,
                name,
                phone);
    }

    private long insertCategory(String name, String slug) {
        return insertAndReturnId(
                "INSERT INTO categories (name,slug,status) VALUES (?,?,'ACTIVE')",
                name,
                slug);
    }

    private long insertProduct(long categoryId, String sku, String name, String slug, int price) {
        return insertAndReturnId(
                "INSERT INTO products (category_id,sku,name,slug,price,currency,status) "
                        + "VALUES (?,?,?,?,?,'VND','ACTIVE')",
                categoryId,
                sku,
                name,
                slug,
                price);
    }

    private void insertInventory(long storeId, long productId, int quantity, int reorderLevel) {
        jdbc.update(
                "INSERT INTO inventory (store_id,product_id,quantity,reserved_quantity,reorder_level,status) "
                        + "VALUES (?,?,?,0,?,'ACTIVE')",
                storeId,
                productId,
                quantity,
                reorderLevel);
    }

    private long insertStaffMembership(long storeId, long staffUserId) {
        return insertAndReturnId(
                "INSERT INTO store_staff (store_id,user_id,status) VALUES (?,?,'ACTIVE')",
                storeId,
                staffUserId);
    }

    private void grant(long membershipId, long grantorId, String permissionName) {
        long permissionId = Objects.requireNonNull(
                jdbc.queryForObject("SELECT id FROM permissions WHERE name=?", Long.class, permissionName));
        jdbc.update(
                "INSERT INTO store_staff_permissions (store_staff_id,permission_id,granted_by_user_id) "
                        + "VALUES (?,?,?)",
                membershipId,
                permissionId,
                grantorId);
    }

    private long insertAndReturnId(String sql, Object... values) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            return statement;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}

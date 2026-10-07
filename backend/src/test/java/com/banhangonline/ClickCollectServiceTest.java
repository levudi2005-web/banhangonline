package com.banhangonline;

import com.banhangonline.order.dto.CartItemRequest;
import com.banhangonline.order.dto.CheckoutRequest;
import com.banhangonline.order.service.ClickCollectService;
import com.banhangonline.store.service.StorePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClickCollectServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ClickCollectService service =
            new ClickCollectService(jdbc, mock(StorePermissionService.class));

    @Test
    @SuppressWarnings("unchecked")
    void addingToCartPreservesStoredPriceSnapshotWhenProductPriceChanges() throws Exception {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(7L))).thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            RowMapper<Object> mapper = invocation.getArgument(1);
            ResultSet row = mock(ResultSet.class);
            if (sql.contains("SELECT id FROM carts")) {
                when(row.getLong(1)).thenReturn(11L);
            } else if (sql.contains("SELECT p.price,p.currency")) {
                when(row.getBigDecimal("price")).thenReturn(new BigDecimal("200.00"));
                when(row.getString("currency")).thenReturn("VND");
                when(row.getInt("available")).thenReturn(20);
            } else if (sql.contains("SELECT quantity, unit_price, currency")) {
                when(row.getInt("quantity")).thenReturn(1);
                when(row.getBigDecimal("unit_price")).thenReturn(new BigDecimal("100.00"));
                when(row.getString("currency")).thenReturn("VND");
            } else if (sql.contains("SELECT ci.id AS cart_item_id")) {
                when(row.getLong("cart_item_id")).thenReturn(21L);
                when(row.getLong("product_id")).thenReturn(9L);
                when(row.getString("name")).thenReturn("Test product");
                when(row.getString("sku")).thenReturn("TEST-9");
                when(row.getString("image_url")).thenReturn("https://images.example.test/test-9.jpg");
                when(row.getInt("quantity")).thenReturn(2);
                when(row.getBigDecimal("unit_price")).thenReturn(new BigDecimal("100.00"));
                when(row.getString("currency")).thenReturn("VND");
            } else {
                throw new AssertionError("Unexpected query: " + sql);
            }
            return List.of(mapper.mapRow(row, 0));
        });

        var cart = service.addItem(5L, new CartItemRequest(7L, 9L, 1));

        assertThat(cart.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.imageUrl()).isEqualTo("https://images.example.test/test-9.jpg");
                    assertThat(item.quantity()).isEqualTo(2);
                    assertThat(item.unitPrice()).isEqualByComparingTo("100.00");
                });
        assertThat(cart.subtotal()).isEqualByComparingTo("200.00");
        verify(jdbc).update(
                org.mockito.ArgumentMatchers.startsWith("UPDATE cart_items SET quantity=?"),
                eq(2), eq(new BigDecimal("100.00")), eq("VND"), eq(11L), eq(9L));
    }

    @Test
    void checkoutRejectsNonCustomerBeforeAccessingDatabase() {
        assertThatThrownBy(() ->
                service.checkout(5L, Set.of("STAFF"), new CheckoutRequest(7L, null)))
                .isInstanceOf(com.banhangonline.common.exception.ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkoutMarksCartAsCheckedOutAfterSuccessfulOrderCreation() {
        when(jdbc.queryForObject(eq("SELECT COUNT(*) FROM stores WHERE id=? AND status='ACTIVE'"), eq(Integer.class), eq(7L)))
                .thenReturn(1);
        when(jdbc.query(eq("SELECT id FROM carts WHERE user_id=? AND store_id=? AND status='ACTIVE'"), any(RowMapper.class), eq(5L), eq(7L)))
                .thenReturn(List.of(21L));
        when(jdbc.query(eq("SELECT id FROM carts WHERE id=? AND status='ACTIVE' FOR UPDATE"), any(RowMapper.class), eq(21L)))
                .thenReturn(List.of(21L));

        when(jdbc.query(eq("""
                SELECT ci.product_id, p.name, p.sku, ci.quantity, ci.unit_price, ci.currency,
                       i.id AS inventory_id, i.quantity AS stock_quantity, i.reserved_quantity
                FROM cart_items ci
                JOIN products p ON p.id=ci.product_id
                JOIN inventory i ON i.product_id=p.id AND i.store_id=?
                WHERE ci.cart_id=? AND i.status='ACTIVE' AND p.status='ACTIVE'
                ORDER BY p.id
                """), any(RowMapper.class), eq(7L), eq(21L)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = mock(ResultSet.class);
                    when(row.getLong("product_id")).thenReturn(9L);
                    when(row.getString("name")).thenReturn("Test product");
                    when(row.getString("sku")).thenReturn("TEST-9");
                    when(row.getInt("quantity")).thenReturn(1);
                    when(row.getBigDecimal("unit_price")).thenReturn(new BigDecimal("100.00"));
                    when(row.getString("currency")).thenReturn("VND");
                    when(row.getLong("inventory_id")).thenReturn(55L);
                    when(row.getInt("stock_quantity")).thenReturn(20);
                    when(row.getInt("reserved_quantity")).thenReturn(0);
                    return List.of(mapper.mapRow(row, 0));
                });
        when(jdbc.queryForObject(eq("SELECT COUNT(*) FROM cart_items WHERE cart_id=?"), eq(Integer.class), eq(21L)))
                .thenReturn(1);
        when(jdbc.query(eq("SELECT quantity-reserved_quantity FROM inventory WHERE id=? FOR UPDATE"), any(RowMapper.class), eq(55L)))
                .thenReturn(List.of(10));
        when(jdbc.update(eq("DELETE FROM cart_items WHERE cart_id=?"), eq(21L)))
                .thenReturn(1);
        when(jdbc.update(eq("UPDATE carts SET status='CHECKED_OUT', updated_at=NOW(6) WHERE id=? AND status='ACTIVE'"), eq(21L)))
                .thenReturn(1);
        when(jdbc.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("GENERATED_KEY", 91L));
            return 1;
        });

        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        when(jdbc.update(anyString(), any(Object[].class), any(Object[].class))).thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(91L))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            RowMapper<Object> mapper = invocation.getArgument(1);
            if (sql.contains("FROM orders o JOIN stores s")) {
                ResultSet row = mock(ResultSet.class);
                when(row.getLong("id")).thenReturn(91L);
                when(row.getString("order_code")).thenReturn("BHO-TEST");
                when(row.getLong("store_id")).thenReturn(7L);
                when(row.getString("store_name")).thenReturn("Test Store");
                when(row.getString("customer_name")).thenReturn("Test User");
                when(row.getString("status")).thenReturn("PENDING");
                when(row.getBigDecimal("subtotal")).thenReturn(new BigDecimal("100.00"));
                when(row.getBigDecimal("total_amount")).thenReturn(new BigDecimal("100.00"));
                when(row.getString("currency")).thenReturn("VND");
                when(row.getString("note")).thenReturn(null);
                when(row.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
                return List.of(mapper.mapRow(row, 0));
            }
            if (sql.contains("FROM order_items WHERE order_id=?")) {
                ResultSet itemRow = mock(ResultSet.class);
                when(itemRow.getLong("product_id")).thenReturn(9L);
                when(itemRow.getString("product_name")).thenReturn("Test product");
                when(itemRow.getString("sku")).thenReturn("TEST-9");
                when(itemRow.getInt("quantity")).thenReturn(1);
                when(itemRow.getBigDecimal("unit_price")).thenReturn(new BigDecimal("100.00"));
                when(itemRow.getBigDecimal("line_total")).thenReturn(new BigDecimal("100.00"));
                when(itemRow.getString("currency")).thenReturn("VND");
                return List.of(mapper.mapRow(itemRow, 0));
            }
            if (sql.contains("FROM order_status_history")) {
                ResultSet historyRow = mock(ResultSet.class);
                when(historyRow.getString("status")).thenReturn("PENDING");
                when(historyRow.getString("note")).thenReturn("Đơn hàng đã được tạo");
                when(historyRow.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
                return List.of(mapper.mapRow(historyRow, 0));
            }
            return List.of();
        });

        service.checkout(5L, Set.of("CUSTOMER"), new CheckoutRequest(7L, null));

        verify(jdbc).update(eq("UPDATE carts SET status='CHECKED_OUT', updated_at=NOW(6) WHERE id=? AND status='ACTIVE'"), eq(21L));
    }
}

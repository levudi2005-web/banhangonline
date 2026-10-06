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
import java.util.List;
import java.util.Set;

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
}

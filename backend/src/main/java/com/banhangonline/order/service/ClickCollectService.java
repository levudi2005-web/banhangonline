package com.banhangonline.order.service;

import com.banhangonline.auth.security.TokenUtil;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.order.dto.*;
import com.banhangonline.store.service.StorePermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class ClickCollectService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final StorePermissionService access;

    public ClickCollectService(JdbcTemplate jdbc, StorePermissionService access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public CartView cart(Long userId, Long storeId) {
        requireActiveStore(storeId);
        Long cartId = findActiveCart(userId, storeId);
        List<CartItemView> items = cartId == null ? List.of() : jdbc.query("""
                SELECT ci.id AS cart_item_id, p.id AS product_id, p.name, p.sku, p.image_url,
                       ci.quantity, ci.unit_price, ci.currency
                FROM cart_items ci
                JOIN products p ON p.id = ci.product_id
                JOIN carts c ON c.id = ci.cart_id
                WHERE c.id = ? AND c.user_id = ? AND c.status = 'ACTIVE'
                ORDER BY p.name
                """, cartItemMapper, cartId, userId);
        Set<String> currencies = items.stream().map(CartItemView::currency).collect(Collectors.toSet());
        if (currencies.size() > 1) {
            throw ApiException.validation("Giỏ hàng không thể chứa nhiều loại tiền tệ");
        }
        String currency = currencies.stream().findFirst().orElse("VND");
        BigDecimal subtotal = items.stream().map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(storeId, cartId, items, subtotal, currency);
    }

    @Transactional
    public CartView addItem(Long userId, CartItemRequest request) {
        requireActiveStore(request.storeId());
        Long cartId = ensureActiveCart(userId, request.storeId());
        StockRow stock = availableStock(request.storeId(), request.productId(), true);
        List<CartItemSnapshot> existing = jdbc.query("""
                SELECT quantity, unit_price, currency
                FROM cart_items
                WHERE cart_id=? AND product_id=? FOR UPDATE
                """, (rs, row) -> new CartItemSnapshot(rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                rs.getString("currency")), cartId, request.productId());
        CartItemSnapshot snapshot = existing.stream().findFirst().orElse(null);
        int newQuantity = request.quantity() + (snapshot == null ? 0 : snapshot.quantity());
        if (newQuantity > stock.available()) {
            throw ApiException.conflict("INSUFFICIENT_STOCK", "Số lượng sản phẩm còn lại không đủ");
        }
        BigDecimal lockedPrice = snapshot == null || snapshot.unitPrice() == null ? stock.price() : snapshot.unitPrice();
        String lockedCurrency = snapshot == null || snapshot.currency() == null ? stock.currency() : snapshot.currency();
        if (snapshot == null) {
            jdbc.update("""
                    INSERT INTO cart_items (cart_id, product_id, quantity, unit_price, currency)
                    VALUES (?, ?, ?, ?, ?)
                    """, cartId, request.productId(), newQuantity, lockedPrice, lockedCurrency);
        } else {
            jdbc.update("""
                    UPDATE cart_items SET quantity=?, unit_price=?, currency=?, updated_at=NOW(6)
                    WHERE cart_id=? AND product_id=?
                    """, newQuantity, lockedPrice, lockedCurrency, cartId, request.productId());
        }
        return cart(userId, request.storeId());
    }

    @Transactional
    public CartView setQuantity(Long userId, Long cartItemId, int quantity) {
        CartItemRef item = lockCartItem(userId, cartItemId);
        StockRow stock = availableStock(item.storeId(), item.productId(), true);
        if (quantity > stock.available()) {
            throw ApiException.conflict("INSUFFICIENT_STOCK", "Số lượng sản phẩm còn lại không đủ");
        }
        BigDecimal lockedPrice = item.unitPrice() == null ? stock.price() : item.unitPrice();
        String lockedCurrency = item.currency() == null ? stock.currency() : item.currency();
        jdbc.update("""
                UPDATE cart_items ci JOIN carts c ON c.id=ci.cart_id
                SET ci.quantity=?, ci.unit_price=?, ci.currency=?, ci.updated_at=NOW(6)
                WHERE ci.id=? AND c.user_id=? AND c.status='ACTIVE'
                """, quantity, lockedPrice, lockedCurrency, cartItemId, userId);
        return cart(userId, item.storeId());
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        lockCartItem(userId, cartItemId);
        int changed = jdbc.update("""
                DELETE ci FROM cart_items ci JOIN carts c ON c.id=ci.cart_id
                WHERE ci.id=? AND c.user_id=? AND c.status='ACTIVE'
                """, cartItemId, userId);
        if (changed == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Không tìm thấy sản phẩm trong giỏ");
        }
    }

    @Transactional
    public CheckoutResponse checkout(Long userId, Set<String> roles, CheckoutRequest request) {
        if (!roles.contains("CUSTOMER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CUSTOMER_REQUIRED", "Chỉ khách hàng mới được đặt hàng");
        }
        requireActiveStore(request.storeId());
        Long cartId = findActiveCart(userId, request.storeId());
        if (cartId == null) {
            throw ApiException.validation("Giỏ hàng đang trống");
        }
        List<Long> lockedCart = jdbc.query("SELECT id FROM carts WHERE id=? AND status='ACTIVE' FOR UPDATE",
                (rs, row) -> rs.getLong(1), cartId);
        if (lockedCart.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "CART_ALREADY_CHECKED_OUT", "Giỏ hàng đã được đặt hàng");
        }
        List<CheckoutLine> lines = jdbc.query("""
                SELECT ci.product_id, p.name, p.sku, ci.quantity, ci.unit_price, ci.currency,
                       i.id AS inventory_id, i.quantity AS stock_quantity, i.reserved_quantity
                FROM cart_items ci
                JOIN products p ON p.id=ci.product_id
                JOIN inventory i ON i.product_id=p.id AND i.store_id=?
                WHERE ci.cart_id=? AND i.status='ACTIVE' AND p.status='ACTIVE'
                ORDER BY p.id
                """, (rs, row) -> new CheckoutLine(rs.getLong("product_id"), rs.getString("name"),
                rs.getString("sku"), rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                rs.getString("currency"), rs.getLong("inventory_id"), rs.getInt("stock_quantity"),
                rs.getInt("reserved_quantity")), request.storeId(), cartId);
        if (lines.isEmpty()) {
            throw ApiException.validation("Giỏ hàng đang trống hoặc sản phẩm không còn được bán");
        }
        Integer itemCount = jdbc.queryForObject("SELECT COUNT(*) FROM cart_items WHERE cart_id=?",
                Integer.class, cartId);
        if (itemCount == null || itemCount != lines.size()) {
            throw ApiException.conflict("CART_HAS_UNAVAILABLE_ITEMS",
                    "Giỏ hàng có sản phẩm ngừng bán hoặc hết hàng; hãy cập nhật giỏ trước khi đặt");
        }

        String currency = lines.getFirst().currency();
        BigDecimal total = BigDecimal.ZERO;
        for (CheckoutLine line : lines) {
            if (!currency.equals(line.currency())) {
                throw ApiException.validation("Giỏ hàng không thể chứa nhiều loại tiền tệ");
            }
            List<Integer> available = jdbc.query(
                    "SELECT quantity-reserved_quantity FROM inventory WHERE id=? FOR UPDATE",
                    (rs, row) -> rs.getInt(1), line.inventoryId());
            if (available.isEmpty() || available.getFirst() < line.quantity()) {
                throw ApiException.conflict("INSUFFICIENT_STOCK", "Số lượng sản phẩm còn lại không đủ");
            }
            total = total.add(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        String code = "BHO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        String pickupCode = newPickupCode();
        Instant expiresAt = Instant.now().plus(48, ChronoUnit.HOURS);
        BigDecimal orderTotal = total;
        Long orderId = insert("""
                INSERT INTO orders (order_code,user_id,store_id,status,subtotal,total_amount,currency,note)
                VALUES (?,?,?,'PENDING',?,?,?,?)
                """, statement -> {
            statement.setString(1, code);
            statement.setLong(2, userId);
            statement.setLong(3, request.storeId());
            statement.setBigDecimal(4, orderTotal);
            statement.setBigDecimal(5, orderTotal);
            statement.setString(6, currency);
            statement.setString(7, clean(request.note()));
        });
        for (CheckoutLine line : lines) {
            BigDecimal lineTotal = line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
            jdbc.update("""
                    INSERT INTO order_items (order_id,product_id,product_name,sku,quantity,unit_price,line_total,currency)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, orderId, line.productId(), line.name(), line.sku(), line.quantity(),
                    line.unitPrice(), lineTotal, currency);
            jdbc.update("UPDATE inventory SET reserved_quantity=reserved_quantity+?, updated_at=NOW(6) WHERE id=?",
                    line.quantity(), line.inventoryId());
        }
        jdbc.update("INSERT INTO order_status_history (order_id,status,changed_by_user_id,note) VALUES (?,'PENDING',?,?)",
                orderId, userId, "Đơn hàng đã được tạo");
        jdbc.update("""
                INSERT INTO pickup (order_id,pickup_code_hash,status,expires_at)
                VALUES (?,?,'WAITING',?)
                """, orderId, TokenUtil.sha256(pickupCode), Timestamp.from(expiresAt));
        jdbc.update("""
                INSERT INTO payments (order_id,provider,status,amount,currency)
                VALUES (?,'CASH_AT_PICKUP','PENDING',?,?)
                """, orderId, orderTotal, currency);
        int clearedItems = jdbc.update("DELETE FROM cart_items WHERE cart_id=?", cartId);
        if (clearedItems != itemCount) {
            throw ApiException.conflict("CART_CHANGED_DURING_CHECKOUT",
                    "Giỏ hàng đã thay đổi trong lúc đặt hàng. Vui lòng kiểm tra lại.");
        }
        int closed = jdbc.update("UPDATE carts SET status='CHECKED_OUT', updated_at=NOW(6) WHERE id=? AND status='ACTIVE'",
                cartId);
        if (closed != 1) {
            throw ApiException.conflict("CART_ALREADY_CHECKED_OUT",
                    "Giỏ hàng đã được đặt hàng");
        }
        notifyUser(userId, "ORDER_CREATED", "Đặt hàng thành công",
                "Đơn hàng " + code + " đã được tiếp nhận.", "ORDER", orderId);
        return new CheckoutResponse(order(orderId), pickupCode);
    }

    @Transactional(readOnly = true)
    public List<OrderView> customerOrders(Long userId) {
        List<Long> ids = jdbc.query("SELECT id FROM orders WHERE user_id=? ORDER BY created_at DESC, id DESC",
                (rs, row) -> rs.getLong(1), userId);
        return ids.stream().map(this::order).toList();
    }

    @Transactional(readOnly = true)
    public OrderView customerOrder(Long userId, Long orderId) {
        requireOrderForCustomer(userId, orderId);
        return order(orderId);
    }

    @Transactional
    public OrderView cancelByCustomer(Long userId, Long orderId) {
        OrderRow order = requireOrderForCustomer(userId, orderId, true);
        if (!"PENDING".equals(order.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_CANNOT_CANCEL",
                    "Chỉ đơn hàng đang chờ xác nhận mới có thể được khách hủy");
        }
        cancelOrder(order, userId, "Khách hàng đã hủy đơn hàng");
        return order(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderView> storeOrders(Long storeId, Long userId, Set<String> roles) {
        access.require(storeId, userId, roles, "VIEW_ORDERS");
        List<Long> ids = jdbc.query("SELECT id FROM orders WHERE store_id=? ORDER BY created_at DESC, id DESC",
                (rs, row) -> rs.getLong(1), storeId);
        return ids.stream().map(this::order).toList();
    }

    @Transactional
    public OrderView updateStoreOrder(Long storeId, Long orderId, Long userId, Set<String> roles,
                                      OrderStatusRequest request) {
        access.require(storeId, userId, roles, "MANAGE_ORDERS");
        OrderRow order = requireOrderForStore(storeId, orderId);
        String next = request.status();
        if ("CANCELLED".equals(next)) {
            if (!Set.of("PENDING", "CONFIRMED", "PREPARING").contains(order.status())) {
                throw new ApiException(HttpStatus.CONFLICT, "ORDER_CANNOT_CANCEL", "Đơn hàng không thể hủy ở trạng thái này");
            }
            cancelOrder(order, userId, clean(request.note()) == null ? "Cửa hàng đã hủy đơn hàng" : request.note().trim());
            return order(orderId);
        }
        String expected = switch (order.status()) {
            case "PENDING" -> "CONFIRMED";
            case "CONFIRMED" -> "PREPARING";
            case "PREPARING" -> "READY_FOR_PICKUP";
            default -> null;
        };
        if (!next.equals(expected)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_ORDER_TRANSITION",
                    "Trạng thái đơn hàng không thể chuyển theo yêu cầu");
        }
        setOrderStatus(orderId, next, userId, clean(request.note()));
        if ("READY_FOR_PICKUP".equals(next)) {
            notifyUser(order.userId(), "ORDER_READY", "Đơn hàng đã sẵn sàng",
                    "Đơn hàng " + order.orderCode() + " đã sẵn sàng nhận tại cửa hàng.", "ORDER", orderId);
        } else {
            notifyUser(order.userId(), "ORDER_STATUS", "Đơn hàng được cập nhật",
                    "Đơn hàng " + order.orderCode() + " đang ở trạng thái " + next + ".", "ORDER", orderId);
        }
        return order(orderId);
    }

    @Transactional
    public OrderView confirmPickup(Long storeId, Long orderId, Long userId, Set<String> roles,
                                   PickupConfirmationRequest request) {
        access.require(storeId, userId, roles, "CONFIRM_PICKUP");
        OrderRow order = requireOrderForStore(storeId, orderId);
        if (!"READY_FOR_PICKUP".equals(order.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_READY", "Đơn hàng chưa sẵn sàng nhận");
        }
        List<PickupRow> pickups = jdbc.query("""
                SELECT pickup_code_hash,status,expires_at FROM pickup WHERE order_id=? FOR UPDATE
                """, (rs, row) -> new PickupRow(rs.getString("pickup_code_hash"), rs.getString("status"),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant()), orderId);
        if (pickups.isEmpty() || !"WAITING".equals(pickups.getFirst().status())) {
            throw new ApiException(HttpStatus.CONFLICT, "PICKUP_UNAVAILABLE", "Mã nhận hàng đã được sử dụng hoặc không tồn tại");
        }
        PickupRow pickup = pickups.getFirst();
        if (pickup.expiresAt() != null && pickup.expiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "PICKUP_CODE_EXPIRED", "Mã nhận hàng đã hết hạn");
        }
        byte[] expected = pickup.codeHash().getBytes(StandardCharsets.US_ASCII);
        byte[] supplied = TokenUtil.sha256(request.pickupCode()).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PICKUP_CODE_INVALID", "Mã nhận hàng không chính xác");
        }
        List<StockConsumption> consumption = jdbc.query("""
                SELECT i.id AS inventory_id, oi.quantity
                FROM order_items oi
                JOIN inventory i ON i.store_id=? AND i.product_id=oi.product_id
                WHERE oi.order_id=?
                """, (rs, row) -> new StockConsumption(rs.getLong("inventory_id"), rs.getInt("quantity")),
                storeId, orderId);
        for (StockConsumption item : consumption) {
            int changed = jdbc.update("""
                    UPDATE inventory SET quantity=quantity-?, reserved_quantity=reserved_quantity-?, updated_at=NOW(6)
                    WHERE id=? AND quantity>=? AND reserved_quantity>=?
                    """, item.quantity(), item.quantity(), item.inventoryId(), item.quantity(), item.quantity());
            if (changed != 1) {
                throw new ApiException(HttpStatus.CONFLICT, "INVENTORY_INCONSISTENT",
                        "Tồn kho không khớp với đơn hàng; cần kiểm tra trước khi xác nhận");
            }
        }
        jdbc.update("""
                UPDATE pickup SET status='PICKED_UP', picked_up_at=NOW(6), confirmed_by_user_id=?, updated_at=NOW(6)
                WHERE order_id=?
                """, userId, orderId);
        setOrderStatus(orderId, "COMPLETED", userId, "Đã bàn giao và thu tiền tại cửa hàng");
        jdbc.update("UPDATE payments SET status='PAID', paid_at=NOW(6), updated_at=NOW(6) WHERE order_id=? AND status='PENDING'",
                orderId);
        notifyUser(order.userId(), "ORDER_COMPLETED", "Đơn hàng đã hoàn tất",
                "Đơn hàng " + order.orderCode() + " đã được nhận tại cửa hàng.", "ORDER", orderId);
        return order(orderId);
    }

    private void cancelOrder(OrderRow order, Long actorId, String reason) {
        List<StockConsumption> reservations = jdbc.query("""
                SELECT i.id AS inventory_id, oi.quantity
                FROM order_items oi
                JOIN inventory i ON i.store_id=? AND i.product_id=oi.product_id
                WHERE oi.order_id=?
                """, (rs, row) -> new StockConsumption(rs.getLong("inventory_id"), rs.getInt("quantity")),
                order.storeId(), order.id());
        for (StockConsumption item : reservations) {
            int changed = jdbc.update("""
                    UPDATE inventory SET reserved_quantity=reserved_quantity-?, updated_at=NOW(6)
                    WHERE id=? AND reserved_quantity>=?
                    """, item.quantity(), item.inventoryId(), item.quantity());
            if (changed != 1) {
                throw new ApiException(HttpStatus.CONFLICT, "INVENTORY_INCONSISTENT",
                        "Tồn kho đã giữ không khớp với đơn hàng");
            }
        }
        setOrderStatus(order.id(), "CANCELLED", actorId, reason);
        jdbc.update("UPDATE pickup SET status='CANCELLED', updated_at=NOW(6) WHERE order_id=? AND status='WAITING'",
                order.id());
        jdbc.update("UPDATE payments SET status='CANCELLED', updated_at=NOW(6) WHERE order_id=? AND status='PENDING'",
                order.id());
        notifyUser(order.userId(), "ORDER_CANCELLED", "Đơn hàng đã bị hủy",
                "Đơn hàng " + order.orderCode() + " đã bị hủy.", "ORDER", order.id());
    }

    private void setOrderStatus(Long orderId, String status, Long actorId, String note) {
        jdbc.update("UPDATE orders SET status=?, updated_at=NOW(6) WHERE id=?", status, orderId);
        jdbc.update("INSERT INTO order_status_history (order_id,status,changed_by_user_id,note) VALUES (?,?,?,?)",
                orderId, status, actorId, note);
    }

    private void notifyUser(Long userId, String type, String title, String message, String referenceType, Long referenceId) {
        jdbc.update("""
                INSERT INTO notifications (user_id,type,title,message,reference_type,reference_id,is_read)
                VALUES (?,?,?,?,?,?,0)
                """, userId, type, title, message, referenceType, referenceId);
    }

    private OrderView order(Long orderId) {
        List<OrderView> found = jdbc.query("""
                SELECT o.id,o.order_code,o.store_id,s.name AS store_name,u.full_name AS customer_name,
                       o.status,o.subtotal,o.total_amount,o.currency,o.note,o.created_at
                FROM orders o JOIN stores s ON s.id=o.store_id JOIN users u ON u.id=o.user_id WHERE o.id=?
                """, (rs, row) -> {
            List<OrderItemView> items = jdbc.query("""
                    SELECT product_id,product_name,sku,quantity,unit_price,line_total,currency
                    FROM order_items WHERE order_id=? ORDER BY id
                    """, (itemRs, itemRow) -> new OrderItemView(itemRs.getLong("product_id"),
                    itemRs.getString("product_name"), itemRs.getString("sku"), itemRs.getInt("quantity"),
                    itemRs.getBigDecimal("unit_price"), itemRs.getBigDecimal("line_total"),
                    itemRs.getString("currency")), orderId);
            List<OrderHistoryView> history = jdbc.query("""
                    SELECT status,note,created_at FROM order_status_history
                    WHERE order_id=? ORDER BY created_at,id
                    """, (historyRs, historyRow) -> new OrderHistoryView(historyRs.getString("status"),
                    historyRs.getString("note"), historyRs.getTimestamp("created_at").toInstant()), orderId);
            return new OrderView(rs.getLong("id"), rs.getString("order_code"), rs.getLong("store_id"),
                    rs.getString("store_name"), rs.getString("customer_name"), rs.getString("status"), rs.getBigDecimal("subtotal"),
                    rs.getBigDecimal("total_amount"), rs.getString("currency"), rs.getString("note"),
                    rs.getTimestamp("created_at").toInstant(), items, history);
        }, orderId);
        if (found.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng");
        }
        return found.getFirst();
    }

    private OrderRow requireOrderForCustomer(Long userId, Long orderId) {
        return requireOrderForCustomer(userId, orderId, false);
    }

    private OrderRow requireOrderForCustomer(Long userId, Long orderId, boolean lock) {
        String sql = """
                SELECT id,order_code,user_id,store_id,status FROM orders WHERE id=? AND user_id=?
                """ + (lock ? " FOR UPDATE" : "");
        List<OrderRow> found = jdbc.query(sql, orderRowMapper, orderId, userId);
        if (found.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng");
        }
        return found.getFirst();
    }

    private OrderRow requireOrderForStore(Long storeId, Long orderId) {
        List<OrderRow> found = jdbc.query("""
                SELECT id,order_code,user_id,store_id,status FROM orders WHERE id=? AND store_id=? FOR UPDATE
                """, orderRowMapper, orderId, storeId);
        if (found.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng");
        }
        return found.getFirst();
    }

    private StockRow availableStock(Long storeId, Long productId, boolean lock) {
        String sql = """
                SELECT p.price,p.currency,i.quantity-i.reserved_quantity AS available
                FROM products p JOIN inventory i ON i.product_id=p.id
                JOIN stores s ON s.id=i.store_id
                WHERE p.id=? AND i.store_id=? AND p.status='ACTIVE'
                  AND i.status='ACTIVE' AND s.status='ACTIVE'
                """ + (lock ? " FOR UPDATE" : "");
        List<StockRow> rows = jdbc.query(sql, (rs, row) ->
                new StockRow(rs.getBigDecimal("price"), rs.getString("currency"), rs.getInt("available")),
                productId, storeId);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_AVAILABLE",
                    "Sản phẩm không có tại cửa hàng này");
        }
        return rows.getFirst();
    }

    private void requireActiveStore(Long storeId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM stores WHERE id=? AND status='ACTIVE'",
                Integer.class, storeId);
        if (count == null || count == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng đang hoạt động");
        }
    }

    private Long findActiveCart(Long userId, Long storeId) {
        List<Long> ids = jdbc.query("SELECT id FROM carts WHERE user_id=? AND store_id=? AND status='ACTIVE'",
                (rs, row) -> rs.getLong(1), userId, storeId);
        return ids.stream().findFirst().orElse(null);
    }

    private Long ensureActiveCart(Long userId, Long storeId) {
        jdbc.update("INSERT IGNORE INTO carts (user_id,store_id,status) VALUES (?,?,'ACTIVE')",
                userId, storeId);
        List<Long> ids = jdbc.query("""
                SELECT id FROM carts
                WHERE user_id=? AND store_id=? AND status='ACTIVE'
                FOR UPDATE
                """, (rs, row) -> rs.getLong(1), userId, storeId);
        return ids.stream().findFirst().orElseThrow(() ->
                new ApiException(HttpStatus.CONFLICT, "CART_UNAVAILABLE", "Không thể tạo giỏ hàng"));
    }

    private CartItemRef lockCartItem(Long userId, Long cartItemId) {
        List<CartItemRef> items = jdbc.query("""
                SELECT c.id AS cart_id,c.store_id,ci.product_id,ci.unit_price,ci.currency
                FROM cart_items ci JOIN carts c ON c.id=ci.cart_id
                WHERE ci.id=? AND c.user_id=? AND c.status='ACTIVE'
                FOR UPDATE
                """, (rs, row) -> new CartItemRef(rs.getLong("cart_id"), rs.getLong("store_id"),
                rs.getLong("product_id"), rs.getBigDecimal("unit_price"), rs.getString("currency")), cartItemId, userId);
        return items.stream().findFirst().orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Không tìm thấy sản phẩm trong giỏ"));
    }

    private Long insert(String sql, SqlBinder binder) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            binder.bind(statement);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return the generated ID");
        }
        return key.longValue();
    }

    private String newPickupCode() {
        return String.format("%010d", RANDOM.nextInt(1_000_000_000));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private final RowMapper<CartItemView> cartItemMapper = (rs, row) -> new CartItemView(
            rs.getLong("cart_item_id"), rs.getLong("product_id"), rs.getString("name"), rs.getString("sku"),
            rs.getString("image_url"), rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
            rs.getString("currency"));
    private final RowMapper<OrderRow> orderRowMapper = (rs, row) -> new OrderRow(
            rs.getLong("id"), rs.getString("order_code"), rs.getLong("user_id"),
            rs.getLong("store_id"), rs.getString("status"));

    private record OrderRow(Long id, String orderCode, Long userId, Long storeId, String status) {}
    private record CartItemSnapshot(int quantity, BigDecimal unitPrice, String currency) {}
    private record CartItemRef(Long cartId, Long storeId, Long productId, BigDecimal unitPrice, String currency) {}
    private record StockRow(BigDecimal price, String currency, int available) {}
    private record CheckoutLine(Long productId, String name, String sku, int quantity, BigDecimal unitPrice,
                                String currency, Long inventoryId, int stockQuantity, int reservedQuantity) {}
    private record StockConsumption(Long inventoryId, int quantity) {}
    private record PickupRow(String codeHash, String status, Instant expiresAt) {}
    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement statement) throws java.sql.SQLException; }
}

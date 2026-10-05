(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const itemsNode = document.querySelector("#cart-items");
  const status = document.querySelector("#cart-status");
  const totals = document.querySelector("#cart-total");
  const storeSelect = document.querySelector("#cart-store");
  const confirmation = document.querySelector("#checkout-result");
  const clearButton = document.querySelector("#cart-clear");

  async function request(path, options = {}) {
    const response = await fetch(`${API}${path}`, {
      credentials: "include",
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.method && options.method !== "GET" ? { "X-Requested-With": "fetch" } : {}),
        ...(options.headers || {})
      }
    });
    const result = await response.json().catch(() => null);
    if (response.status === 401) {
      const returnUrl = `/pages/customer/cart.html?store=${encodeURIComponent(storeSelect.value)}`;
      location.assign(AppRoutes.goToLogin("CUSTOMER", returnUrl));
      throw new Error("Đang chuyển tới trang đăng nhập.");
    }
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể hoàn tất yêu cầu.");
    }
    return result.data;
  }

  function money(value, currency) {
    return `${Number(value).toLocaleString("vi-VN")} ${currency}`;
  }

  async function loadCart(preserveConfirmation = false) {
    const storeId = storeSelect.value;
    itemsNode.replaceChildren();
    if (!preserveConfirmation) confirmation.hidden = true;
    clearButton.hidden = true;
    totals.textContent = "0 VND";
    status.className = "dash-status";
    if (!storeId) {
      status.textContent = "Chọn cửa hàng để xem giỏ hàng.";
      return;
    }
    try {
      const cart = await request(`/api/customer/cart/${encodeURIComponent(storeId)}`);
      if (!cart.items.length) {
        const empty = document.createElement("li");
        empty.className = "dash-empty";
        empty.textContent = "Giỏ hàng đang trống.";
        itemsNode.append(empty);
      }
      clearButton.hidden = !cart.items.length;
      cart.items.forEach(item => {
        const row = document.createElement("li");
        const name = document.createElement("strong");
        name.textContent = item.name;
        const amount = document.createElement("span");
        amount.className = "dash-muted";
        amount.textContent = `${item.sku} · ${money(item.unitPrice, item.currency)}`;
        const controls = document.createElement("div");
        controls.className = "dash-inline";
        const quantity = document.createElement("span");
        quantity.textContent = `Số lượng: ${item.quantity}`;
        const minus = document.createElement("button");
        minus.type = "button";
        minus.className = "dash-button secondary";
        minus.textContent = "−";
        minus.setAttribute("aria-label", `Giảm ${item.name}`);
        minus.addEventListener("click", async () => {
          try {
            if (item.quantity === 1) await request(`/api/customer/cart/items/${item.cartItemId}`, { method: "DELETE" });
            else await request(`/api/customer/cart/items/${item.cartItemId}`, {
              method: "PATCH", body: JSON.stringify({ quantity: item.quantity - 1 })
            });
            await loadCart();
          } catch (error) { showError(error); }
        });
        const plus = document.createElement("button");
        plus.type = "button";
        plus.className = "dash-button secondary";
        plus.textContent = "+";
        plus.setAttribute("aria-label", `Tăng ${item.name}`);
        plus.addEventListener("click", async () => {
          try {
            await request(`/api/customer/cart/items/${item.cartItemId}`, {
              method: "PATCH", body: JSON.stringify({ quantity: item.quantity + 1 })
            });
            await loadCart();
          } catch (error) { showError(error); }
        });
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "dash-button secondary";
        remove.textContent = "Xóa";
        remove.setAttribute("aria-label", `Xóa ${item.name} khỏi giỏ`);
        remove.addEventListener("click", async () => {
          remove.disabled = true;
          try {
            await request(`/api/customer/cart/items/${item.cartItemId}`, { method: "DELETE" });
            await loadCart();
          } catch (error) {
            showError(error);
            remove.disabled = false;
          }
        });
        controls.append(minus, quantity, plus, remove);
        row.append(name, amount, controls);
        itemsNode.append(row);
      });
      totals.textContent = money(cart.subtotal, cart.currency);
      status.textContent = `${cart.items.length} sản phẩm trong giỏ. Giá được kiểm tra lại tại thời điểm đặt hàng.`;
    } catch (error) {
      showError(error);
    }
  }

  clearButton.addEventListener("click", async () => {
    clearButton.disabled = true;
    try {
      const cart = await request(`/api/customer/cart/${encodeURIComponent(storeSelect.value)}`);
      for (const item of cart.items) {
        await request(`/api/customer/cart/items/${item.cartItemId}`, { method: "DELETE" });
      }
      await loadCart();
    } catch (error) {
      showError(error);
      await loadCart();
      showError(error);
    } finally {
      clearButton.disabled = false;
    }
  });

  function showError(error) {
    status.textContent = error.message;
    status.className = "dash-status error";
  }

  async function loadStores() {
    const response = await fetch(`${API}/api/stores`);
    const result = await response.json().catch(() => null);
    if (!response.ok || !result || !result.success) throw new Error("Không thể tải cửa hàng.");
    storeSelect.replaceChildren();
    result.data.forEach(store => {
      const option = document.createElement("option");
      option.value = store.id;
      option.textContent = store.name;
      storeSelect.append(option);
    });
    const preferred = new URLSearchParams(location.search).get("store")
      || localStorage.getItem("selectedStoreId");
    if (preferred && result.data.some(store => String(store.id) === preferred)) storeSelect.value = preferred;
    if (storeSelect.value) localStorage.setItem("selectedStoreId", storeSelect.value);
    await loadCart();
  }

  document.querySelector("#checkout-form").addEventListener("submit", async event => {
    event.preventDefault();
    const button = event.currentTarget.querySelector("button[type=submit]");
    if (!storeSelect.value) {
      status.textContent = "Chọn cửa hàng trước khi đặt.";
      status.className = "dash-status error";
      return;
    }
    button.disabled = true;
    try {
      const note = document.querySelector("#order-note").value.trim();
      const data = await request("/api/customer/orders", {
        method: "POST",
        body: JSON.stringify({ storeId: Number(storeSelect.value), note: note || null })
      });
      confirmation.replaceChildren();
      const heading = document.createElement("h2");
      heading.textContent = "Đặt hàng thành công";
      const orderCode = document.createElement("p");
      orderCode.textContent = `Mã đơn: ${data.order.orderCode}`;
      const pickupCode = document.createElement("p");
      pickupCode.className = "dash-alert";
      pickupCode.textContent = `Mã nhận hàng (lưu lại, mã chỉ hiển thị một lần): ${data.pickupCode}`;
      const details = document.createElement("p");
      details.textContent = "Thanh toán tiền mặt khi nhận hàng tại cửa hàng.";
      confirmation.append(heading, orderCode, pickupCode, details);
      confirmation.hidden = false;
      status.textContent = "";
      await loadCart(true);
    } catch (error) {
      showError(error);
    } finally {
      button.disabled = false;
    }
  });

  storeSelect.addEventListener("change", async () => {
    localStorage.setItem("selectedStoreId", storeSelect.value);
    await loadCart();
  });
  document.querySelector("#cart-back").href = AppRoutes.ROUTES.customer.home;
  AppRoutes.requireAuth("CUSTOMER").then(user => {
    if (!user) return;
    document.querySelector("#customer-name").textContent = user.fullName || user.username;
    loadStores().catch(showError);
  }).catch(showError);
})();

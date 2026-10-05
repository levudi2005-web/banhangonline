(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const status = document.querySelector("#store-status");
  const storeSelect = document.querySelector("#store-select");
  const list = document.querySelector("#product-list");

  async function loadProducts(storeId) {
    list.replaceChildren();
    if (!storeId) return;
    const response = await fetch(`${API}/api/catalog/stores/${encodeURIComponent(storeId)}/products`);
    const result = await response.json().catch(() => null);
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể tải sản phẩm.");
    }
    if (!result.data.length) {
      const empty = document.createElement("li");
      empty.className = "dash-empty";
      empty.textContent = "Cửa hàng hiện chưa có sản phẩm còn hàng.";
      list.append(empty);
      return;
    }
    result.data.forEach(product => {
      const item = document.createElement("li");
      const name = document.createElement("strong");
      name.textContent = product.name;
      const details = document.createElement("span");
      details.className = "dash-muted";
      details.textContent = `${product.categoryName} · ${product.sku} · Còn ${product.quantity - product.reservedQuantity}`;
      const price = document.createElement("p");
      price.textContent = `${Number(product.price).toLocaleString("vi-VN")} ${product.currency}`;
      const add = document.createElement("button");
      add.type = "button";
      add.className = "dash-button";
      add.textContent = "Thêm vào giỏ";
      add.addEventListener("click", async () => {
        add.disabled = true;
        try {
          const response = await fetch(`${API}/api/customer/cart/items`, {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json", "X-Requested-With": "fetch" },
            body: JSON.stringify({ storeId: Number(storeId), productId: product.id, quantity: 1 })
          });
          const result = await response.json().catch(() => null);
          if (response.status === 401) {
            const returnUrl = `/pages/customer/index.html?store=${encodeURIComponent(storeId)}`;
            location.assign(AppRoutes.goToLogin("CUSTOMER", returnUrl));
            return;
          }
          if (!response.ok || !result || !result.success) {
            throw new Error(result && result.message || "Không thể thêm sản phẩm.");
          }
          status.textContent = "Đã thêm vào giỏ hàng.";
          status.className = "dash-status success";
        } catch (error) {
          status.textContent = error.message;
          status.className = "dash-status error";
        } finally {
          add.disabled = false;
        }
      });
      item.append(name, details, price, add);
      list.append(item);
    });
  }

  async function load() {
    try {
      const response = await fetch(`${API}/api/stores`, { credentials: "include" });
      const result = await response.json().catch(() => null);
      if (!response.ok || !result || !result.success) {
        throw new Error(result && result.message || "Không thể tải danh sách cửa hàng.");
      }
      const stores = result.data;
      storeSelect.replaceChildren();
      const first = document.createElement("option");
      first.value = "";
      first.textContent = stores.length ? "Chọn cửa hàng" : "Chưa có cửa hàng hoạt động";
      storeSelect.append(first);
      if (!result.data.length) {
        status.textContent = "Hiện chưa có cửa hàng nào được kích hoạt.";
      }
      stores.forEach(store => {
        const option = document.createElement("option");
        option.value = store.id;
        option.textContent = `${store.name} — ${store.addressDetail}, ${store.ward}, ${store.district}`;
        storeSelect.append(option);
      });
      const requested = new URLSearchParams(location.search).get("store");
      const selected = stores.find(store => String(store.id) === requested)
        || stores.find(store => String(store.id) === localStorage.getItem("selectedStoreId"))
        || stores[0];
      if (selected) {
        storeSelect.value = String(selected.id);
        localStorage.setItem("selectedStoreId", String(selected.id));
        status.textContent = `${stores.length} cửa hàng đang hoạt động.`;
        await loadProducts(storeSelect.value);
      }
    } catch (error) {
      status.textContent = error.message;
      status.classList.add("error");
    }
  }

  storeSelect.addEventListener("change", async () => {
    localStorage.setItem("selectedStoreId", storeSelect.value);
    status.className = "dash-status";
    try {
      await loadProducts(storeSelect.value);
    } catch (error) {
      status.textContent = error.message;
      status.className = "dash-status error";
    }
  });

  load();
})();

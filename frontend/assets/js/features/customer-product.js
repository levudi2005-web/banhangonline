(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  const params = new URLSearchParams(location.search);
  const storeId = params.get("store");
  const productId = params.get("product");
  const status = document.querySelector("#product-status");
  const detail = document.querySelector("#product-detail");
  const quantityInput = document.querySelector("#product-quantity");
  const addButton = document.querySelector("#product-add");
  const storeLine = document.querySelector("#product-store");
  const cartLink = document.querySelector("#product-cart-link");
  let product = null;

  quantityInput.addEventListener("input", () => quantityInput.setCustomValidity(""));

  function money(value, currency) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency,
      maximumFractionDigits: currency === "VND" ? 0 : 2
    }).format(Number(value));
  }

  async function getJson(path) {
    const response = await fetch(`${API}${path}`, { credentials: "include" });
    const result = await response.json().catch(() => null);
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể tải thông tin sản phẩm.");
    }
    return result.data;
  }

  function showError(error) {
    status.textContent = error instanceof TypeError
      ? "Không thể kết nối máy chủ. Vui lòng thử lại sau."
      : error.message;
    status.className = "dash-status error";
    detail.hidden = true;
  }

  function render(productData, store) {
    product = productData;
    const categoryName = product.categoryName || "Sản phẩm";
    document.title = `${product.name} — BanHangOnline`;
    document.querySelector("#product-category-crumb").textContent = categoryName;
    document.querySelector("#product-category").textContent = categoryName;
    document.querySelector("#product-name").textContent = product.name;
    document.querySelector("#product-sku").textContent = `Mã sản phẩm: ${product.sku}`;
    document.querySelector("#product-description").textContent =
      product.description || "Sản phẩm được chuẩn bị sẵn để bạn nhận tại cửa hàng.";
    document.querySelector("#product-price").textContent = money(product.price, product.currency);

    const available = Number(product.quantity) || 0;
    const availability = document.querySelector("#product-availability");
    availability.textContent = available > 0 ? `Còn ${available} sản phẩm` : "Tạm hết hàng";
    availability.className = `product-availability${available > 0 ? " in-stock" : " out-of-stock"}`;
    quantityInput.max = String(available);
    quantityInput.disabled = available < 1;
    addButton.disabled = available < 1;
    if (available < 1) addButton.textContent = "Tạm hết hàng";

    storeLine.textContent = [store.name, store.addressDetail, store.ward, store.district]
      .filter(Boolean).join(" · ");
    cartLink.href = AppRoutes.getRoute("customer.cart", { store: storeId });

    const media = document.querySelector("#product-media");
    media.replaceChildren();
    if (product.imageUrl) {
      const image = document.createElement("img");
      image.src = product.imageUrl;
      image.alt = product.name;
      image.loading = "eager";
      image.referrerPolicy = "no-referrer";
      image.addEventListener("error", () => {
        image.remove();
        media.classList.add("product-detail-fallback");
        media.textContent = categoryName;
      }, { once: true });
      media.append(image);
    } else {
      media.classList.add("product-detail-fallback");
      media.textContent = categoryName;
    }

    detail.hidden = false;
    detail.setAttribute("aria-busy", "false");
    status.textContent = "Thông tin và giá được tải từ cửa hàng đã chọn.";
    status.className = "dash-status";
  }

  document.querySelector("#product-cart-form").addEventListener("submit", async event => {
    event.preventDefault();
    if (!product) return;
    const quantity = Number(quantityInput.value);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > Number(product.quantity)) {
      quantityInput.setCustomValidity("Số lượng phải nằm trong mức hàng còn tại cửa hàng.");
      quantityInput.reportValidity();
      return;
    }
    quantityInput.setCustomValidity("");
    addButton.disabled = true;
    try {
      const response = await fetch(`${API}/api/customer/cart/items`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-Requested-With": "fetch" },
        body: JSON.stringify({ storeId: Number(storeId), productId: product.id, quantity })
      });
      const result = await response.json().catch(() => null);
      if (response.status === 401) {
        const returnUrl = `${location.pathname}${location.search}`;
        location.assign(AppRoutes.goToLogin("CUSTOMER", returnUrl));
        return;
      }
      if (!response.ok || !result || !result.success) {
        throw new Error(result && result.message || "Không thể thêm sản phẩm vào giỏ.");
      }
      status.textContent = `Đã thêm ${quantity} × ${product.name} vào giỏ hàng.`;
      status.className = "dash-status success";
    } catch (error) {
      showError(error);
    } finally {
      addButton.disabled = Number(product.quantity) < 1;
    }
  });

  async function load() {
    if (!/^\d+$/.test(storeId || "") || !/^\d+$/.test(productId || "")) {
      showError(new Error("Đường dẫn sản phẩm không hợp lệ. Hãy quay lại cửa hàng."));
      return;
    }
    try {
      const stores = await getJson("/api/stores");
      const store = stores.find(item => String(item.id) === storeId);
      if (!store) throw new Error("Cửa hàng này hiện không hoạt động.");
      const item = await getJson(
        `/api/catalog/stores/${encodeURIComponent(storeId)}/products/${encodeURIComponent(productId)}`
      );
      render(item, store);
    } catch (error) {
      showError(error);
    }
  }

  load();
})();

(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  const params = new URLSearchParams(location.search);
  let storeId = params.get("store");
  const productId = params.get("product");
  const productSlug = location.pathname.startsWith("/san-pham/")
    ? location.pathname.slice("/san-pham/".length)
    : "";
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

  function render(productData, store, images, galleryError) {
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
    const thumbnails = document.querySelector("#product-thumbnails");
    media.replaceChildren();
    const gallery = images.length ? images : product.imageUrl ? [{ imageUrl: product.imageUrl }] : [];
    if (gallery.length) {
      const image = document.createElement("img");
      image.src = gallery[0].imageUrl;
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
    thumbnails.replaceChildren();
    if (gallery.length > 1) {
      gallery.forEach((item, index) => {
        const thumbnail = document.createElement("button");
        thumbnail.type = "button";
        thumbnail.className = "product-gallery-thumbnail";
        thumbnail.setAttribute("aria-label", `Xem ảnh ${index + 1} của ${gallery.length}`);
        thumbnail.setAttribute("aria-pressed", String(index === 0));
        const thumbnailImage = document.createElement("img");
        thumbnailImage.src = item.imageUrl;
        thumbnailImage.alt = "";
        thumbnailImage.loading = "lazy";
        thumbnailImage.addEventListener("error", () => {
          thumbnail.disabled = true;
          thumbnail.setAttribute("aria-label", `Không tải được ảnh ${index + 1}`);
        }, { once: true });
        thumbnail.addEventListener("click", () => {
          const mainImage = media.querySelector("img");
          if (!mainImage) return;
          mainImage.src = item.imageUrl;
          thumbnails.querySelectorAll("button").forEach(button =>
            button.setAttribute("aria-pressed", String(button === thumbnail)));
        });
        thumbnail.append(thumbnailImage);
        thumbnails.append(thumbnail);
      });
    }

    detail.hidden = false;
    detail.setAttribute("aria-busy", "false");
    status.textContent = galleryError
      ? `Thông tin sản phẩm đã tải; không thể tải thư viện ảnh: ${galleryError}`
      : "Thông tin, giá và ảnh được tải từ cửa hàng đã chọn.";
    status.className = galleryError ? "dash-status error" : "dash-status";
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
    try {
      const stores = await getJson("/api/stores");
      let store = stores.find(item => String(item.id) === storeId);
      let item;
      if (productSlug) {
        const candidates = store ? [store] : stores;
        for (const candidate of candidates) {
          const catalog = await getJson(
            `/api/catalog/stores/${encodeURIComponent(candidate.id)}/products`
          );
          item = catalog.find(product => product.slug === productSlug);
          if (item) {
            store = candidate;
            storeId = String(candidate.id);
            break;
          }
        }
      } else if (/^\d+$/.test(storeId || "") && /^\d+$/.test(productId || "")) {
        item = await getJson(
          `/api/catalog/stores/${encodeURIComponent(storeId)}/products/${encodeURIComponent(productId)}`
        );
      }
      if (!store || !item) throw new Error("Không tìm thấy sản phẩm tại cửa hàng đang hoạt động.");
      localStorage.setItem("selectedStoreId", storeId);
      let images = item.imageUrl ? [{ imageUrl: item.imageUrl }] : [];
      let galleryError = "";
      try {
        images = await getJson(
          `/api/catalog/stores/${encodeURIComponent(storeId)}/products/${encodeURIComponent(item.id)}/images`
        );
      } catch (error) {
        galleryError = error instanceof TypeError
          ? "Không thể kết nối máy chủ."
          : error.message;
      }
      render(item, store, images, galleryError);
    } catch (error) {
      showError(error);
    }
  }

  load();
})();

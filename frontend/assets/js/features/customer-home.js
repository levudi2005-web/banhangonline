(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const status = document.querySelector("#store-status");
  const storeSelect = document.querySelector("#store-select");
  const list = document.querySelector("#product-list");
  const search = document.querySelector("#product-search");
  const categorySelect = document.querySelector("#category-filter");
  const sortSelect = document.querySelector("#product-sort");
  const productCount = document.querySelector("#product-count");
  let products = [];

  function money(value, currency) {
    return `${Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 })} ${currency}`;
  }

  function renderProducts() {
    const query = search.value.trim().toLocaleLowerCase("vi");
    const category = categorySelect.value;
    const visible = products.filter(product => {
      const matchesText = `${product.name} ${product.description || ""} ${product.sku}`
        .toLocaleLowerCase("vi").includes(query);
      return matchesText && (!category || String(product.categoryId) === category);
    });
    if (sortSelect.value === "price-asc") visible.sort((a, b) => Number(a.price) - Number(b.price));
    if (sortSelect.value === "price-desc") visible.sort((a, b) => Number(b.price) - Number(a.price));
    if (sortSelect.value === "name") visible.sort((a, b) => a.name.localeCompare(b.name, "vi"));

    list.replaceChildren();
    productCount.textContent = `${visible.length} / ${products.length} sản phẩm`;
    if (!visible.length) {
      const empty = document.createElement("div");
      empty.className = "dash-empty shop-empty";
      empty.textContent = products.length
        ? "Không tìm thấy sản phẩm phù hợp. Thử đổi từ khóa hoặc danh mục."
        : "Cửa hàng chưa có sản phẩm còn hàng. Vui lòng quay lại sau.";
      list.append(empty);
      return;
    }

    visible.forEach(product => {
      const card = document.createElement("article");
      card.className = "shop-product-card";
      const productUrl = AppRoutes.getRoute("customer.product", {
        store: storeSelect.value,
        product: product.id
      });
      const art = document.createElement("a");
      art.className = "shop-product-art";
      art.href = productUrl;
      art.setAttribute("aria-label", `Xem chi tiết ${product.name}`);
      if (product.imageUrl) {
        const image = document.createElement("img");
        image.src = product.imageUrl;
        image.alt = product.name;
        image.loading = "lazy";
        image.referrerPolicy = "no-referrer";
        image.addEventListener("error", () => {
          image.remove();
          art.classList.add("shop-product-art-fallback");
          art.textContent = product.categoryName || "Sản phẩm";
        }, { once: true });
        art.append(image);
      } else {
        art.classList.add("shop-product-art-fallback");
        art.textContent = product.categoryName || "Sản phẩm";
      }

      const content = document.createElement("div");
      content.className = "shop-product-content";
      const category = document.createElement("span");
      category.className = "dash-badge";
      category.textContent = product.categoryName || "Sản phẩm";
      const name = document.createElement("h3");
      const nameLink = document.createElement("a");
      nameLink.href = productUrl;
      nameLink.textContent = product.name;
      name.append(nameLink);
      const description = document.createElement("p");
      description.className = "shop-product-description";
      description.textContent = product.description || "Sản phẩm được chuẩn bị tại cửa hàng.";
      const sku = document.createElement("span");
      sku.className = "dash-muted";
      sku.textContent = `Mã hàng ${product.sku}`;
      const price = document.createElement("strong");
      price.className = "shop-product-price";
      price.textContent = money(product.price, product.currency);
      const stock = document.createElement("span");
      stock.className = "shop-stock";
      stock.textContent = `Còn ${product.quantity}`;
      content.append(category, name, description, sku);

      const footer = document.createElement("div");
      footer.className = "shop-product-footer";
      const add = document.createElement("button");
      add.type = "button";
      add.className = "dash-button";
      add.textContent = "Thêm vào giỏ";
      add.setAttribute("aria-label", `Thêm ${product.name} vào giỏ`);
      add.disabled = Number(product.quantity) < 1;
      add.addEventListener("click", async () => {
        add.disabled = true;
        try {
          const response = await fetch(`${API}/api/customer/cart/items`, {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json", "X-Requested-With": "fetch" },
            body: JSON.stringify({
              storeId: Number(storeSelect.value),
              productId: product.id,
              quantity: 1
            })
          });
          const result = await response.json().catch(() => null);
          if (response.status === 401) {
            const returnUrl = `/pages/customer/index.html?store=${encodeURIComponent(storeSelect.value)}`;
            location.assign(AppRoutes.goToLogin("CUSTOMER", returnUrl));
            return;
          }
          if (!response.ok || !result || !result.success) {
            throw new Error(result && result.message || "Không thể thêm sản phẩm.");
          }
          status.textContent = `Đã thêm ${product.name} vào giỏ hàng.`;
          status.className = "dash-status success";
        } catch (error) {
          status.textContent = error.message;
          status.className = "dash-status error";
        } finally {
          add.disabled = false;
        }
      });
      const details = document.createElement("a");
      details.className = "dash-button secondary shop-detail-link";
      details.href = productUrl;
      details.textContent = "Chi tiết";
      footer.append(price, stock, details, add);
      content.append(footer);
      card.append(art, content);
      list.append(card);
    });
  }

  async function loadProducts(storeId) {
    list.replaceChildren();
    products = [];
    renderProducts();
    if (!storeId) return;
    const response = await fetch(`${API}/api/catalog/stores/${encodeURIComponent(storeId)}/products`);
    const result = await response.json().catch(() => null);
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể tải sản phẩm.");
    }
    products = result.data;
    renderProducts();
  }

  async function loadCategories() {
    const response = await fetch(`${API}/api/catalog/categories`);
    const result = await response.json().catch(() => null);
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể tải danh mục sản phẩm.");
    }
    categorySelect.replaceChildren();
    const all = document.createElement("option");
    all.value = "";
    all.textContent = "Tất cả danh mục";
    categorySelect.append(all);
    result.data.forEach(category => {
      const option = document.createElement("option");
      option.value = category.id;
      option.textContent = category.name;
      categorySelect.append(option);
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
        await loadCategories();
        await loadProducts(storeSelect.value);
        status.textContent = `${stores.length} cửa hàng đang hoạt động.`;
      } else {
        status.textContent = "Hiện chưa có cửa hàng nào được kích hoạt.";
        status.className = "dash-status";
      }
    } catch (error) {
      status.textContent = error.message;
      status.className = "dash-status error";
    }
  }

  storeSelect.addEventListener("change", async () => {
    localStorage.setItem("selectedStoreId", storeSelect.value);
    status.className = "dash-status";
    try {
      await loadProducts(storeSelect.value);
      status.textContent = storeSelect.value
        ? "Danh sách sản phẩm đã được cập nhật."
        : "Chọn cửa hàng để xem sản phẩm.";
    } catch (error) {
      status.textContent = error.message;
      status.className = "dash-status error";
    }
  });
  search.addEventListener("input", renderProducts);
  categorySelect.addEventListener("change", renderProducts);
  sortSelect.addEventListener("change", renderProducts);

  load();
})();

(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const status = document.querySelector("#store-status");
  const storeSelect = document.querySelector("#store-select");
  const list = document.querySelector("#product-list");
  const search = document.querySelector("#product-search");
  const categorySelect = document.querySelector("#category-filter");
  const categoryNav = document.querySelector("#shop-category-nav");
  const sortSelect = document.querySelector("#product-sort");
  const productCount = document.querySelector("#product-count");
  const hotline = document.querySelector("#shop-hotline");
  const nearbyButton = document.querySelector("#find-nearby-stores");
  const nearbyStatus = document.querySelector("#nearby-store-status");
  const nearbyList = document.querySelector("#nearby-store-list");
  let products = [];
  let categories = [];
  let stores = [];
  let customerLocation = null;

  function setStoreHotline(store) {
    if (!hotline) return;
    const phone = (store && store.phone || "").trim();
    const dialablePhone = phone.replace(/[^\d+]/g, "");
    if (!phone || !/^\+?\d{6,15}$/.test(dialablePhone)) {
      hotline.hidden = true;
      hotline.removeAttribute("href");
      return;
    }
    hotline.href = `tel:${dialablePhone}`;
    hotline.querySelector("strong").textContent = phone;
    hotline.hidden = false;
  }

  function money(value, currency) {
    return `${Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 })} ${currency}`;
  }

  function distanceKm(store) {
    if (!customerLocation || store.latitude == null || store.longitude == null) return null;
    const radians = degrees => degrees * Math.PI / 180;
    const lat1 = radians(customerLocation.latitude);
    const lat2 = radians(Number(store.latitude));
    const latDelta = radians(Number(store.latitude) - customerLocation.latitude);
    const lonDelta = radians(Number(store.longitude) - customerLocation.longitude);
    const a = Math.min(1, Math.sin(latDelta / 2) ** 2
      + Math.cos(lat1) * Math.cos(lat2) * Math.sin(lonDelta / 2) ** 2);
    return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  function storeDistanceLabel(store) {
    const distance = distanceKm(store);
    return distance == null ? "" : ` · ${formatDistance(distance)}`;
  }

  function formatDistance(distance) {
    return distance < 1
      ? `${Math.round(distance * 1000)} m`
      : `${distance.toFixed(1)} km`;
  }

  function renderNearbyStores() {
    if (!nearbyList) return;
    nearbyList.replaceChildren();
    const nearby = stores.map(store => ({ store, distance: distanceKm(store) }))
      .filter(item => item.distance != null)
      .sort((a, b) => a.distance - b.distance);
    if (!nearby.length) {
      nearbyList.hidden = false;
      const empty = document.createElement("li");
      empty.className = "dash-empty";
      empty.textContent = "Chưa có cửa hàng nào lưu tọa độ bản đồ.";
      nearbyList.append(empty);
      return;
    }
    nearby.slice(0, 5).forEach(({ store, distance }) => {
      const item = document.createElement("li");
      item.className = "shop-nearby-item";
      const details = document.createElement("div");
      const name = document.createElement("strong");
      name.textContent = store.name;
      const address = document.createElement("span");
      address.className = "dash-muted";
      address.textContent = `${store.addressDetail}, ${store.ward}, ${store.district}, ${store.province}`;
      const range = document.createElement("span");
      range.className = "shop-nearby-distance";
      range.textContent = formatDistance(distance);
      details.append(name, address);
      const choose = document.createElement("button");
      choose.type = "button";
      choose.className = "dash-button secondary";
      choose.textContent = "Chọn cửa hàng";
      choose.addEventListener("click", () => {
        storeSelect.value = String(store.id);
        storeSelect.dispatchEvent(new Event("change", { bubbles: true }));
        storeSelect.scrollIntoView({ behavior: "smooth", block: "center" });
      });
      item.append(details, range, choose);
      nearbyList.append(item);
    });
    nearbyList.hidden = false;
  }

  function renderStoreOptions(selectedId) {
    storeSelect.replaceChildren();
    const first = document.createElement("option");
    first.value = "";
    first.textContent = stores.length ? "Chọn cửa hàng" : "Chưa có cửa hàng hoạt động";
    storeSelect.append(first);
    const sorted = [...stores].sort((a, b) => {
      const aDistance = distanceKm(a);
      const bDistance = distanceKm(b);
      if (aDistance == null && bDistance == null) return a.name.localeCompare(b.name, "vi");
      if (aDistance == null) return 1;
      if (bDistance == null) return -1;
      return aDistance - bDistance;
    });
    sorted.forEach(store => {
      const option = document.createElement("option");
      option.value = store.id;
      option.textContent = `${store.name} — ${store.addressDetail}, ${store.ward}, ${store.district}${storeDistanceLabel(store)}`;
      option.dataset.phone = store.phone || "";
      storeSelect.append(option);
    });
    if (selectedId && stores.some(store => String(store.id) === String(selectedId))) {
      storeSelect.value = String(selectedId);
    }
  }

  function renderProducts() {
    const query = search.value.trim().toLocaleLowerCase("vi");
    const category = categorySelect.value;
    const visible = products.filter(product => {
      const matchesText = `${product.name} ${product.description || ""} ${product.sku}`
        .toLocaleLowerCase("vi").includes(query);
      const matchesCategory = !category || String(product.categoryId) === category;
      return matchesText && matchesCategory;
    });
    if (sortSelect.value === "price-asc") visible.sort((a, b) => Number(a.price) - Number(b.price));
    if (sortSelect.value === "price-desc") visible.sort((a, b) => Number(b.price) - Number(a.price));
    if (sortSelect.value === "name") visible.sort((a, b) => a.name.localeCompare(b.name, "vi"));

    list.replaceChildren();
    list.setAttribute("aria-busy", "false");
    productCount.textContent = `${visible.length} / ${products.length} sản phẩm`;
    categoryNav?.querySelectorAll("a").forEach(link => {
      const selectedCategory = link.dataset.categoryId || "";
      link.setAttribute("aria-current", selectedCategory === category ? "page" : "false");
    });
    if (!visible.length) {
      const empty = document.createElement("div");
      empty.className = "dash-empty shop-empty";
      empty.textContent = !storeSelect.value
        ? "Chọn cửa hàng nhận hàng để xem sản phẩm."
        : products.length
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
        slug: product.slug
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
      const categoryData = categories.find(item => String(item.id) === String(product.categoryId));
      const category = document.createElement(categoryData ? "a" : "span");
      category.className = "dash-badge";
      if (categoryData) {
        category.href = AppRoutes.getRoute("customer.category", {
          slug: categoryData.slug,
          store: storeSelect.value
        });
      }
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
      const available = Number(product.quantity) || 0;
      stock.className = `shop-stock${available > 0 ? " in-stock" : " out-of-stock"}`;
      stock.textContent = available > 0 ? `Còn ${available}` : "Tạm hết hàng";
      content.append(category, name, description, sku);

      const footer = document.createElement("div");
      footer.className = "shop-product-footer";
      const add = document.createElement("button");
      add.type = "button";
      add.className = "dash-button";
      add.textContent = "Thêm vào giỏ";
      add.setAttribute("aria-label", `Thêm ${product.name} vào giỏ`);
      add.disabled = available < 1;
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
    list.setAttribute("aria-busy", "true");
    productCount.textContent = "";
    if (!storeId) {
      renderProducts();
      return;
    }
    for (let index = 0; index < 4; index++) {
      const skeleton = document.createElement("div");
      skeleton.className = "shop-product-skeleton";
      skeleton.setAttribute("aria-hidden", "true");
      list.append(skeleton);
    }
    try {
      const response = await fetch(`${API}/api/catalog/stores/${encodeURIComponent(storeId)}/products`);
      const result = await response.json().catch(() => null);
      if (!response.ok || !result || !result.success) {
        throw new Error(result && result.message || "Không thể tải sản phẩm.");
      }
      products = result.data;
      renderProducts();
    } catch (error) {
      list.replaceChildren();
      list.setAttribute("aria-busy", "false");
      const errorState = document.createElement("div");
      errorState.className = "dash-empty shop-empty shop-load-error";
      errorState.textContent = error.message;
      list.append(errorState);
      throw error;
    }
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
    categories = result.data;
    categories.forEach(category => {
      const option = document.createElement("option");
      option.value = category.id;
      option.textContent = category.name;
      option.dataset.slug = category.slug;
      categorySelect.append(option);
    });
    const pathSlug = location.pathname.startsWith("/danh-muc/")
      ? location.pathname.slice("/danh-muc/".length)
      : "";
    const pathCategory = result.data.find(category => category.slug === pathSlug);
    if (pathCategory) categorySelect.value = String(pathCategory.id);
    if (categoryNav) {
      categoryNav.replaceChildren();
      const allLink = document.createElement("a");
      allLink.href = storeSelect.value
        ? AppRoutes.getRoute("customer.home", { store: storeSelect.value })
        : AppRoutes.getRoute("customer.home");
      allLink.textContent = "Tất cả sản phẩm";
      allLink.dataset.categoryId = "";
      allLink.setAttribute("aria-current", pathCategory ? "false" : "page");
      categoryNav.append(allLink);
      categories.forEach(category => {
        const link = document.createElement("a");
        link.href = AppRoutes.getRoute("customer.category", {
          slug: category.slug,
          store: storeSelect.value
        });
        link.textContent = category.name;
        link.dataset.categoryId = String(category.id);
        categoryNav.append(link);
      });
    }
    search.value = new URLSearchParams(location.search).get("q") || "";
  }

  async function load() {
    try {
      const response = await fetch(`${API}/api/stores`, { credentials: "include" });
      const result = await response.json().catch(() => null);
      if (!response.ok || !result || !result.success) {
        throw new Error(result && result.message || "Không thể tải danh sách cửa hàng.");
      }
      stores = result.data;
      const requested = new URLSearchParams(location.search).get("store");
      const selected = stores.find(store => String(store.id) === requested)
        || stores.find(store => String(store.id) === localStorage.getItem("selectedStoreId"))
        || stores[0];
      renderStoreOptions(selected && selected.id);
      if (nearbyButton) {
        nearbyButton.addEventListener("click", () => {
          if (!navigator.geolocation) {
            nearbyStatus.textContent = "Trình duyệt này không hỗ trợ xác định vị trí.";
            nearbyStatus.className = "dash-status error";
            return;
          }
          nearbyButton.disabled = true;
          nearbyStatus.textContent = "Đang xác định vị trí hiện tại…";
          nearbyStatus.className = "dash-status";
          navigator.geolocation.getCurrentPosition(position => {
            customerLocation = {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude
            };
            renderStoreOptions(storeSelect.value);
            renderNearbyStores();
            const mappedCount = stores.filter(store => distanceKm(store) != null).length;
            nearbyStatus.textContent = mappedCount
              ? `Tìm thấy ${mappedCount} cửa hàng có vị trí. Khoảng cách là đường chim bay.`
              : "Chưa có cửa hàng nào lưu vị trí bản đồ để tính khoảng cách.";
            nearbyStatus.className = mappedCount ? "dash-status success" : "dash-status";
            nearbyButton.disabled = false;
          }, error => {
            nearbyStatus.textContent = error.code === error.PERMISSION_DENIED
              ? "Bạn chưa cho phép truy cập vị trí. Bạn có thể chọn cửa hàng thủ công."
              : "Không xác định được vị trí hiện tại. Vui lòng thử lại.";
            nearbyStatus.className = "dash-status error";
            nearbyButton.disabled = false;
          }, { enableHighAccuracy: false, maximumAge: 300000, timeout: 10000 });
        });
      }
      if (selected) {
        storeSelect.value = String(selected.id);
        localStorage.setItem("selectedStoreId", String(selected.id));
        setStoreHotline(selected);
        await loadCategories();
        await loadProducts(storeSelect.value);
        status.textContent = `${stores.length} cửa hàng đang hoạt động.`;
      } else {
        setStoreHotline(null);
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
      const selectedStore = Array.from(storeSelect.options)
        .filter(option => option.value)
        .map(option => ({ id: option.value, phone: option.dataset.phone }))
        .find(store => String(store.id) === storeSelect.value);
      setStoreHotline(selectedStore);
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

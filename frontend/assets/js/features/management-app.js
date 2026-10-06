(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  const root = document.querySelector("#management-app");
  if (!root) return;

  const role = location.pathname.startsWith("/quan-ly/owner/")
    || location.pathname === "/quan-ly/owner"
    || location.pathname.startsWith("/pages/owner/") ? "OWNER" : "STAFF";
  const area = role === "OWNER" ? "owner" : "staff";
  const page = root.dataset.managementPage;
  const labels = {
    dashboard: "Tổng quan",
    store: "Cửa hàng",
    staff: "Nhân viên",
    permissions: "Phân quyền",
    categories: "Danh mục",
    products: "Sản phẩm",
    inventory: "Tồn kho",
    orders: "Đơn hàng",
    notifications: "Thông báo"
  };
  const permissionLabels = {
    VIEW_ORDERS: "Xem đơn hàng",
    MANAGE_ORDERS: "Xử lý đơn hàng",
    CONFIRM_PICKUP: "Xác nhận khách nhận hàng",
    VIEW_PRODUCTS: "Xem sản phẩm",
    MANAGE_PRODUCTS: "Thêm, sửa, ẩn sản phẩm và giá",
    VIEW_INVENTORY: "Xem tồn kho",
    MANAGE_INVENTORY: "Điều chỉnh số lượng tồn kho",
    VIEW_STAFF: "Xem nhân viên",
    MANAGE_STAFF: "Quản lý nhân viên",
    VIEW_STORES: "Xem cửa hàng",
    MANAGE_STORES: "Quản lý cửa hàng",
    VIEW_REPORTS: "Xem báo cáo",
    MANAGE_SETTINGS: "Quản lý cài đặt"
  };
  const editablePermissions = [
    "VIEW_ORDERS", "MANAGE_ORDERS", "CONFIRM_PICKUP",
    "VIEW_PRODUCTS", "MANAGE_PRODUCTS", "VIEW_INVENTORY", "MANAGE_INVENTORY"
  ];
  const storeFormFields = [
    ["name", "Tên cửa hàng", "text", true],
    ["phone", "Điện thoại", "tel", true],
    ["email", "Email", "email", false],
    ["province", "Tỉnh / thành phố", "text", true],
    ["district", "Quận / huyện", "text", true],
    ["ward", "Phường / xã", "text", true],
    ["addressDetail", "Địa chỉ chi tiết", "text", true],
    ["postalCode", "Mã bưu chính", "text", false],
    ["description", "Mô tả", "textarea", false]
  ];
  let context;
  let statusNode;
  let contentNode;

  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined && text !== null) node.textContent = text;
    if (className) node.className = className;
    return node;
  }

  function navigationIcon(name) {
    const paths = {
      dashboard: "M3 3h7v7H3zM14 3h7v4h-7zM14 10h7v11h-7zM3 14h7v7H3z",
      store: "M3 10h18M5 10v10h14V10M4 10l1.5-6h13L20 10M9 20v-6h6v6",
      staff: "M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M20 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75",
      permissions: "M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11zM9 12l2 2 4-4",
      categories: "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z",
      products: "m7 4 5-2 5 2 4 2-2 5v10H5V11L3 6zM3 6l9 4 9-4M12 10v11",
      inventory: "M4 5h16M4 12h16M4 19h16M7 3v4M17 10v4M9 17v4",
      orders: "M6 3h12v18H6zM9 7h6M9 11h6M9 15h4",
      notifications: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"
    };
    const icon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    icon.setAttribute("viewBox", "0 0 24 24");
    icon.setAttribute("aria-hidden", "true");
    icon.setAttribute("focusable", "false");
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("d", paths[name] || paths.dashboard);
    icon.append(path);
    return icon;
  }

  function setStatus(message, kind = "") {
    if (!statusNode) return;
    statusNode.textContent = message || "";
    statusNode.className = `dash-status ${kind}`.trim();
  }

  async function request(path, options = {}) {
    const isFormData = options.body instanceof FormData;
    const response = await fetch(`${API}${path}`, {
      credentials: "include",
      ...options,
      headers: {
        ...(options.body && !isFormData ? { "Content-Type": "application/json" } : {}),
        ...(options.method && options.method !== "GET" ? { "X-Requested-With": "fetch" } : {}),
        ...(options.headers || {})
      }
    });
    const result = await response.json().catch(() => null);
    if (response.status === 401) {
      AppRoutes.handleSessionExpired(role);
      throw new Error("Phiên đăng nhập đã hết hạn.");
    }
    if (!response.ok || !result || !result.success) {
      const error = new Error(result && result.message || "Không thể hoàn tất yêu cầu.");
      error.code = result && result.code;
      error.status = response.status;
      throw error;
    }
    return result.data;
  }

  function field(labelText, name, type = "text", required = false, value = "") {
    const wrapper = element("div", undefined, "dash-field");
    const inputId = `management-${name}-${Math.random().toString(36).slice(2, 9)}`;
    const label = element("label", labelText);
    label.htmlFor = inputId;
    const control = element(type === "textarea" ? "textarea" : "input");
    control.id = inputId;
    control.name = name;
    if (type !== "textarea") control.type = type;
    control.required = required;
    if (value !== undefined && value !== null) control.value = String(value);
    if (type === "number") {
      control.min = "0";
      control.step = "1";
    }
    wrapper.append(label, control);
    return wrapper;
  }

  function button(text, kind = "secondary", action) {
    const node = element("button", text, `dash-button ${kind}`.trim());
    node.type = "button";
    if (action) node.addEventListener("click", action);
    return node;
  }

  function statusBadge(value) {
    const labels = {
      ACTIVE: "Đang hoạt động",
      CANCELLED: "Đã hủy",
      COMPLETED: "Hoàn tất",
      CONFIRMED: "Đã xác nhận",
      DISABLED: "Đã khóa",
      DRAFT: "Bản nháp",
      INACTIVE: "Tạm ẩn",
      PENDING: "Đang chờ",
      PENDING_REVIEW: "Chờ duyệt",
      PENDING_VERIFICATION: "Chờ xác minh",
      PREPARING: "Đang chuẩn bị",
      READY_FOR_PICKUP: "Sẵn sàng nhận",
      SUSPENDED: "Tạm dừng"
    };
    const node = element("span", labels[value] || value || "Chưa xác định", "dash-badge");
    if (["DRAFT", "PENDING", "PENDING_REVIEW", "PENDING_VERIFICATION", "READY_FOR_PICKUP"].includes(value)) {
      node.classList.add("pending");
    } else if (["DISABLED", "SUSPENDED", "CANCELLED", "INACTIVE"].includes(value)) {
      node.classList.add("disabled");
    }
    return node;
  }

  function loadingNotice(message, tagName = "div") {
    const notice = element(tagName, message, "dash-empty");
    notice.setAttribute("role", "status");
    notice.setAttribute("aria-live", "polite");
    return notice;
  }

  function has(permission) {
    return role === "OWNER" || Boolean(context.permissions && context.permissions.includes(permission));
  }

  function routeKey(view) {
    return `${area}.${view === "dashboard" ? "home" : view}`;
  }

  function links() {
    const items = [{ page: "dashboard", icon: "dashboard" }];
    if (role === "OWNER") {
      items.push({ page: "store", icon: "store" });
      if (context.stores.some(store => store.status === "ACTIVE")) {
        items.push({ page: "staff", icon: "staff" }, { page: "permissions", icon: "permissions" });
      }
      items.push({ page: "categories", icon: "categories" });
    }
    if (has("VIEW_PRODUCTS") || has("MANAGE_PRODUCTS")) items.push({ page: "products", icon: "products" });
    if (has("VIEW_INVENTORY") || has("MANAGE_INVENTORY")) items.push({ page: "inventory", icon: "inventory" });
    if (has("VIEW_ORDERS") || has("MANAGE_ORDERS")) items.push({ page: "orders", icon: "orders" });
    items.push({ page: "notifications", icon: "notifications" });
    return items;
  }

  function buildShell() {
    const layout = element("div", undefined, "management-layout");
    const sidebar = element("aside", undefined, "management-sidebar");
    sidebar.id = "management-sidebar";
    const mobileNavigation = window.matchMedia("(max-width: 960px)");
    sidebar.inert = mobileNavigation.matches;
    sidebar.setAttribute("aria-hidden", String(mobileNavigation.matches));
    const brand = document.createElement("a");
    brand.className = "management-brand";
    brand.href = AppRoutes.getRoute(routeKey("dashboard"));
    brand.append(element("span", "B", "management-brand-mark"),
      element("span", "BanHangOnline", "management-brand-name"));
    const brandSub = element("span", role === "OWNER" ? "KHU VỰC CHỦ CỬA HÀNG" : "KHU VỰC NHÂN VIÊN",
      "management-brand-sub");
    const nav = element("nav", undefined, "management-nav");
    nav.setAttribute("aria-label", "Điều hướng quản lý");
    const sectionTitle = element("span", "QUẢN LÝ", "management-nav-label");
    nav.append(sectionTitle);
    links().forEach(item => {
      const link = document.createElement("a");
      link.className = "management-nav-link";
      link.href = AppRoutes.getRoute(routeKey(item.page));
      link.setAttribute("aria-current", item.page === page ? "page" : "false");
      link.dataset.managementNav = "";
      const icon = element("span", undefined, "management-nav-icon");
      icon.append(navigationIcon(item.icon));
      link.append(icon, element("span", labels[item.page]));
      if (item.page === page) link.classList.add("active");
      link.addEventListener("click", () => {
        if (mobileNavigation.matches) closeSidebar(true);
      });
      nav.append(link);
    });
    const sidebarFoot = element("div", undefined, "management-sidebar-foot");
    const shopLink = document.createElement("a");
    shopLink.href = AppRoutes.getRoute("customer.home");
    shopLink.className = "management-storefront-link";
    shopLink.append(element("span", "↗"), element("span", "Mở trang mua sắm"));
    const logoutLink = document.createElement("a");
    logoutLink.href = AppRoutes.ROUTES[area].logout;
    logoutLink.className = "management-logout-link";
    logoutLink.append(element("span", "↪"), element("span", "Đăng xuất"));
    sidebarFoot.append(shopLink, logoutLink);
    sidebar.append(brand, brandSub, nav, sidebarFoot);

    const main = element("main", undefined, "management-main");
    const topbar = element("header", undefined, "management-topbar");
    const menuButton = button("☰", "secondary management-menu-toggle");
    menuButton.setAttribute("aria-label", "Mở menu điều hướng");
    menuButton.setAttribute("aria-expanded", "false");
    menuButton.setAttribute("aria-controls", sidebar.id);
    function closeSidebar(restoreFocus = false) {
      const wasOpen = layout.classList.contains("sidebar-open");
      layout.classList.remove("sidebar-open");
      menuButton.setAttribute("aria-expanded", "false");
      menuButton.setAttribute("aria-label", "Mở menu điều hướng");
      backdrop.tabIndex = -1;
      sidebar.inert = mobileNavigation.matches;
      sidebar.setAttribute("aria-hidden", String(mobileNavigation.matches));
      if (restoreFocus && wasOpen) menuButton.focus();
    }
    function openSidebar() {
      layout.classList.add("sidebar-open");
      menuButton.setAttribute("aria-expanded", "true");
      menuButton.setAttribute("aria-label", "Đóng menu điều hướng");
      backdrop.tabIndex = 0;
      sidebar.inert = false;
      sidebar.setAttribute("aria-hidden", "false");
      const activeLink = sidebar.querySelector(".management-nav-link.active");
      (activeLink || sidebar.querySelector(".management-nav-link"))?.focus();
    }
    const sidebarClose = button("×", "secondary management-sidebar-close", () => closeSidebar(true));
    sidebarClose.setAttribute("aria-label", "Đóng menu điều hướng");
    sidebar.insertBefore(sidebarClose, sidebar.firstChild);
    menuButton.addEventListener("click", () => {
      if (layout.classList.contains("sidebar-open")) closeSidebar(true);
      else openSidebar();
    });
    const topTitle = element("span", labels[page] || "Quản lý", "management-top-title");
    const storeGroup = element("div", undefined, "management-store-picker");
    if (context.activeStores.length) {
      const storeLabel = element("label", "Cửa hàng", "management-store-label");
      storeLabel.htmlFor = "management-store-select";
      const select = element("select", undefined, "management-store-select");
      select.id = "management-store-select";
      context.activeStores.forEach(store => {
        const storeId = store.id || store.storeId;
        const option = element("option", store.name || store.storeName);
        option.value = storeId;
        option.selected = String(storeId) === String(context.selectedStoreId);
        select.append(option);
      });
      select.addEventListener("change", () => {
        const storageKey = `management-store-${context.user.id}-${role}`;
        localStorage.setItem(storageKey, select.value);
        location.reload();
      });
      storeGroup.append(storeLabel, select);
    } else {
      storeGroup.append(element("span", "Chưa có cửa hàng đang hoạt động", "management-store-label"));
    }
    const userMenu = element("div", undefined, "management-user");
    userMenu.append(element("span", (context.user.fullName || context.user.username || "U").slice(0, 1).toUpperCase(),
      "management-avatar"));
    const identity = element("div", undefined, "management-user-identity");
    identity.append(element("strong", context.user.fullName || context.user.username),
      element("span", role === "OWNER" ? "Chủ cửa hàng" : "Nhân viên"));
    const profile = document.createElement("a");
    profile.href = `${AppRoutes.getRoute("customer.account")}?area=${area}`;
    profile.className = "management-user-profile";
    profile.setAttribute("aria-label", "Thông tin tài khoản");
    profile.textContent = "›";
    userMenu.append(identity, profile);
    topbar.append(menuButton, topTitle, storeGroup, userMenu);

    const pageContent = element("section", undefined, "management-page-content");
    const heading = element("div", undefined, "management-page-heading");
    const headingCopy = element("div");
    headingCopy.append(element("p", "KHÔNG GIAN LÀM VIỆC", "management-eyebrow"),
      element("h1", labels[page] || "Quản lý", "management-page-title"),
      element("p", "", "management-page-description"));
    statusNode = element("div", undefined, "dash-status management-status");
    statusNode.setAttribute("role", "status");
    statusNode.setAttribute("aria-live", "polite");
    heading.append(headingCopy);
    pageContent.append(heading, statusNode);
    contentNode = element("div", undefined, "management-content");
    pageContent.append(contentNode);
    main.append(topbar, pageContent);
    layout.append(sidebar, main);
    const backdrop = element("button", "", "management-backdrop");
    backdrop.type = "button";
    backdrop.setAttribute("aria-label", "Đóng menu");
    backdrop.tabIndex = -1;
    backdrop.addEventListener("click", () => closeSidebar(true));
    document.addEventListener("keydown", event => {
      if (event.key === "Escape" && layout.classList.contains("sidebar-open")) {
        closeSidebar(true);
        return;
      }
      if (event.key !== "Tab" || !layout.classList.contains("sidebar-open")) return;
      const focusable = sidebar.querySelectorAll(
        'a[href], button:not(:disabled), select:not(:disabled), input:not(:disabled), [tabindex]:not([tabindex="-1"])'
      );
      if (!focusable.length) {
        event.preventDefault();
        menuButton.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && (document.activeElement === first || !sidebar.contains(document.activeElement))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (document.activeElement === last || !sidebar.contains(document.activeElement))) {
        event.preventDefault();
        first.focus();
      }
    });
    mobileNavigation.addEventListener("change", event => {
      if (!event.matches) closeSidebar();
      else {
        sidebar.inert = !layout.classList.contains("sidebar-open");
        sidebar.setAttribute("aria-hidden", String(sidebar.inert));
      }
    });
    layout.append(backdrop);
    root.replaceChildren(layout);
  }

  function configureAccess() {
    const ownerOnly = ["store", "staff", "permissions", "categories"].includes(page);
    const permissionByPage = {
      products: ["VIEW_PRODUCTS", "MANAGE_PRODUCTS"],
      inventory: ["VIEW_INVENTORY", "MANAGE_INVENTORY"],
      orders: ["VIEW_ORDERS", "MANAGE_ORDERS"]
    };
    if (ownerOnly && role !== "OWNER") {
      location.replace(AppRoutes.getRoute("forbidden"));
      return false;
    }
    const required = permissionByPage[page];
    if (required && !required.some(has)) {
      location.replace(AppRoutes.getRoute("forbidden"));
      return false;
    }
    return true;
  }

  function addCard(title, subtitle) {
    const card = element("section", undefined, "dash-card management-card");
    const header = element("div", undefined, "management-card-heading");
    header.append(element("h2", title));
    if (subtitle) header.append(element("p", subtitle, "dash-muted"));
    card.append(header);
    contentNode.append(card);
    return card;
  }

  function noStoreNotice() {
    const owner = role === "OWNER";
    const card = addCard(owner ? "Chưa có cửa hàng đang hoạt động" : "Chưa có cửa hàng được phân công",
      owner ? "Các thao tác quản lý chỉ khả dụng sau khi cửa hàng được kích hoạt." :
        "Liên hệ chủ cửa hàng để được phân công vào một cửa hàng đang hoạt động.");
    if (owner) card.append(element("p", "Bạn vẫn có thể cập nhật bản nháp và theo dõi trạng thái tại mục Cửa hàng."));
    if (role === "OWNER") {
      const link = document.createElement("a");
      link.className = "dash-button";
      link.href = AppRoutes.getRoute("owner.store");
      link.textContent = "Thiết lập cửa hàng";
      card.append(link);
    }
  }

  async function renderDashboard() {
    contentNode.classList.add("management-dashboard-content");
    const greeting = `Xin chào, ${context.user.fullName || context.user.username}`;
    document.querySelector(".management-page-title").textContent = greeting;
    document.querySelector(".management-page-description").textContent =
      role === "OWNER" ? "Tổng quan hoạt động của cửa hàng và các lối tắt quản lý." :
        "Công việc được hiển thị theo đúng quyền bạn nhận được trong cửa hàng đã chọn.";
    const metrics = element("div", undefined, "management-metrics");
    contentNode.append(metrics);
    const storeCount = context.activeStores.length;
    const storeMetric = element("article", undefined, "management-metric");
    storeMetric.append(element("span", role === "OWNER" ? "CỬA HÀNG ĐANG HOẠT ĐỘNG" : "CỬA HÀNG ĐƯỢC PHÂN CÔNG"),
      element("strong", String(storeCount)), element("small", role === "OWNER" ? "Trong tài khoản của bạn" : "Đang hoạt động"));
    metrics.append(storeMetric);
    if (role === "OWNER") {
      const draftCount = context.stores.filter(store => ["DRAFT", "PENDING", "PENDING_REVIEW"].includes(store.status)).length;
      const draft = element("article", undefined, "management-metric");
      draft.append(element("span", "BẢN NHÁP / CHỜ DUYỆT"), element("strong", String(draftCount)),
        element("small", "Cửa hàng chưa kích hoạt"));
      metrics.append(draft);
    } else {
      const allowed = element("article", undefined, "management-metric");
      allowed.append(element("span", "QUYỀN CỦA BẠN"), element("strong", String(context.permissions.length)),
        element("small", "Quyền trong cửa hàng đang chọn"));
      metrics.append(allowed);
    }
    if (!context.selectedStoreId) {
      if (role === "OWNER" && context.stores.length) {
        const table = addCard("Cửa hàng của bạn");
        const list = element("div", undefined, "management-store-list");
        context.stores.forEach(store => {
          const row = element("div", undefined, "dash-row");
          const info = element("div");
          info.append(element("strong", store.name), element("span",
            `${store.addressDetail}, ${store.ward}, ${store.district}, ${store.province}`, "dash-muted"));
          row.append(info, statusBadge(store.status));
          list.append(row);
        });
        table.append(list);
      } else if (!storeCount) {
        noStoreNotice();
      }
      return;
    }
    const shortcuts = addCard("Lối tắt", "Các trang khả dụng được lọc theo vai trò và quyền hiện tại.");
    const quick = element("div", undefined, "management-shortcuts");
    links().filter(item => item.page !== "dashboard").forEach(item => {
      const link = document.createElement("a");
      link.className = "management-shortcut";
      link.href = AppRoutes.getRoute(routeKey(item.page));
      const icon = element("span", undefined, "management-shortcut-icon");
      icon.append(navigationIcon(item.icon));
      link.append(icon, element("strong", labels[item.page]), element("span", "→"));
      quick.append(link);
    });
    shortcuts.append(quick);
    const canSeeProducts = has("VIEW_PRODUCTS") || has("MANAGE_PRODUCTS");
    const canSeeOrders = has("VIEW_ORDERS") || has("MANAGE_ORDERS");
    const activityCard = addCard("Hoạt động gần đây", "Đơn nhận tại cửa hàng gần nhất.");
    activityCard.classList.add("management-activity-card");
    let activityLoading;
    if (!canSeeOrders) {
      activityCard.append(element("div", "Hoạt động đơn hàng không hiển thị theo quyền hiện tại.", "dash-empty"));
    } else {
      activityLoading = loadingNotice("Đang tải hoạt động đơn hàng…");
      activityCard.append(activityLoading);
    }
    if (!canSeeProducts && !canSeeOrders) return;
    const metricResults = await Promise.allSettled([
      canSeeProducts ? request(`/api/owner/stores/${context.selectedStoreId}/products`) : Promise.resolve(null),
      canSeeOrders ? request(`/api/stores/${context.selectedStoreId}/orders`) : Promise.resolve(null)
    ]);
    const productResult = metricResults[0];
    const orderResult = metricResults[1];
    if (productResult.status === "fulfilled" && Array.isArray(productResult.value)) {
      const products = productResult.value;
      const card = element("article", undefined, "management-metric");
      card.append(element("span", "SẢN PHẨM"), element("strong", String(products.length)),
        element("small", "Có trong cửa hàng"));
      metrics.append(card);
      if (has("VIEW_INVENTORY") || has("MANAGE_INVENTORY")) {
        const lowStock = products.filter(product => {
          if (product.quantity === null || product.quantity === undefined
              || product.reservedQuantity === null || product.reservedQuantity === undefined
              || product.reorderLevel === null || product.reorderLevel === undefined
              || !Number.isFinite(Number(product.quantity))
              || !Number.isFinite(Number(product.reservedQuantity))
              || !Number.isFinite(Number(product.reorderLevel))) return false;
          const available = Number(product.quantity) - Number(product.reservedQuantity);
          return Number.isFinite(available) && available <= Number(product.reorderLevel);
        }).length;
        const stock = element("article", undefined, "management-metric");
        stock.append(element("span", "CẦN KIỂM TRA TỒN"), element("strong", String(lowStock)),
          element("small", "Số lượng không vượt mức cảnh báo"));
        metrics.append(stock);
      }
    } else if (canSeeProducts) {
      const message = productResult.status === "rejected"
        ? productResult.reason.message : "Máy chủ không trả về danh sách sản phẩm hợp lệ.";
      metrics.append(element("div", "Không thể tải số liệu sản phẩm và tồn kho.", "dash-empty"));
      setStatus(message, "error");
    }
    if (orderResult.status === "fulfilled" && Array.isArray(orderResult.value)) {
      const orders = orderResult.value;
      activityLoading.remove();
      const pendingCount = orders.filter(order => ["PENDING", "CONFIRMED", "PREPARING"].includes(order.status)).length;
      const card = element("article", undefined, "management-metric");
      card.append(element("span", "ĐƠN CẦN XỬ LÝ"), element("strong", String(pendingCount)),
        element("small", `Tổng ${orders.length} đơn`));
      metrics.append(card);
      const recent = element("ul", undefined, "management-activity-list");
      const latestOrders = [...orders]
        .sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt))
        .slice(0, 5);
      if (!latestOrders.length) {
        recent.append(element("li", "Chưa có hoạt động đơn hàng.", "dash-empty"));
      } else {
        latestOrders.forEach(order => {
          const item = element("li", undefined, "management-activity-item");
          const heading = element("div", undefined, "management-activity-heading");
          heading.append(element("strong", order.orderCode), statusBadge(order.status));
          const lines = (order.items || [])
            .map(line => `${line.productName} × ${line.quantity}`)
            .join(" · ");
          item.append(heading,
            element("span", lines || order.storeName || "Đơn nhận tại cửa hàng", "dash-muted"),
            element("time", order.createdAt
              ? new Date(order.createdAt).toLocaleString("vi-VN")
              : "Thời gian chưa có", "dash-muted"));
          recent.append(item);
        });
      }
      activityCard.append(recent);
    } else if (canSeeOrders) {
      const message = orderResult.status === "rejected"
        ? orderResult.reason.message : "Máy chủ không trả về danh sách đơn hàng hợp lệ.";
      activityLoading.remove();
      activityCard.append(element("div", "Không thể tải hoạt động gần đây.", "dash-empty"));
      setStatus(message, "error");
    }
  }

  async function renderStore() {
    document.querySelector(".management-page-description").textContent =
      "Quản lý bản nháp cửa hàng và gửi thông tin để được xét duyệt.";
    const listCard = addCard("Cửa hàng của bạn");
    if (!context.stores.length) {
      listCard.append(element("div", "Chưa có cửa hàng. Hãy tạo bản nháp bằng biểu mẫu bên cạnh.", "dash-empty"));
    } else {
      const list = element("div", undefined, "management-store-list");
      context.stores.forEach(store => {
        const row = element("div", undefined, "dash-row");
        const details = element("div");
        details.append(element("strong", store.name), element("span",
          `${store.phone} · ${store.addressDetail}, ${store.ward}, ${store.district}, ${store.province}`,
          "dash-muted"));
        row.append(details, statusBadge(store.status));
        if (store.status === "DRAFT") {
          row.append(button("Sửa bản nháp", "secondary", () => {
            const form = document.querySelector("#store-editor");
            form.dataset.storeId = store.id;
            storeFormFields.forEach(([name]) => {
              const control = form.elements[name];
              if (control) control.value = store[name] || "";
            });
            document.querySelector("#store-submit").textContent = "Cập nhật bản nháp";
            form.scrollIntoView({ behavior: "smooth", block: "center" });
          }));
        }
        list.append(row);
      });
      listCard.append(list);
    }
    const formCard = addCard("Thiết lập bản nháp", "Cửa hàng chỉ hiển thị cho khách sau khi được quản trị viên kích hoạt.");
    const form = element("form", undefined, "dash-form management-form");
    form.id = "store-editor";
    storeFormFields.forEach(([name, text, type, required]) => {
      const wrapper = field(text, name, type, required);
      if (name === "description") wrapper.classList.add("full");
      form.append(wrapper);
    });
    const save = element("button", "Lưu bản nháp", "dash-button");
    save.type = "submit";
    save.id = "store-submit";
    form.append(save);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      save.disabled = true;
      try {
        const values = Object.fromEntries([...new FormData(form).entries()].filter(([, value]) => value !== ""));
        const storeId = form.dataset.storeId;
        await request(storeId ? `/api/owner/stores/${storeId}` : "/api/owner/stores", {
          method: storeId ? "PUT" : "POST",
          body: JSON.stringify(values)
        });
        setStatus("Đã lưu thông tin cửa hàng.", "success");
        location.reload();
      } catch (error) {
        setStatus(error.message, "error");
        save.disabled = false;
      }
    });
    formCard.append(form);
    const pending = context.stores.find(store => store.status === "DRAFT");
    if (pending) {
      const submit = button("Gửi quản trị viên kiểm duyệt", "secondary", async () => {
        submit.disabled = true;
        try {
          await request(`/api/owner/stores/${pending.id}/submit-review`, { method: "POST" });
          setStatus("Đã gửi cửa hàng để quản trị viên kiểm duyệt.", "success");
          location.reload();
        } catch (error) {
          setStatus(error.message, "error");
          submit.disabled = false;
        }
      });
      formCard.append(submit);
    }
  }

  function permissionChoices(permissions, includeAll = false) {
    const wrapper = element("div", undefined, "dash-checks management-permission-grid");
    (includeAll ? editablePermissions : ["VIEW_ORDERS", "MANAGE_ORDERS", "CONFIRM_PICKUP",
      "VIEW_PRODUCTS", "MANAGE_PRODUCTS", "VIEW_INVENTORY", "MANAGE_INVENTORY"]).forEach(permission => {
      const label = element("label", undefined, "dash-check");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.name = "permissions";
      checkbox.value = permission;
      checkbox.checked = permissions.includes(permission);
      label.append(checkbox, element("span", permissionLabels[permission]));
      wrapper.append(label);
    });
    return wrapper;
  }

  async function renderStaff() {
    document.querySelector(".management-page-description").textContent =
      "Tạo tài khoản nhân viên và quản lý trạng thái tham gia cửa hàng.";
    if (!context.selectedStoreId) return noStoreNotice();
    const createCard = addCard("Thêm nhân viên", "Mật khẩu khởi tạo chỉ hiển thị cho chủ cửa hàng; hãy bàn giao qua kênh an toàn.");
    const form = element("form", undefined, "dash-form management-form");
    form.append(field("Họ và tên", "fullName", "text", true),
      field("Tên đăng nhập", "username", "text", true),
      field("Email", "email", "email", true),
      field("Điện thoại", "phone", "tel", true),
      field("Mật khẩu khởi tạo (ít nhất 8 ký tự)", "initialPassword", "password", true));
    form.elements.username.minLength = 4;
    form.elements.username.maxLength = 30;
    form.elements.fullName.maxLength = 120;
    form.elements.email.maxLength = 190;
    form.elements.phone.maxLength = 20;
    form.elements.initialPassword.minLength = 8;
    const fieldset = element("fieldset", undefined, "full management-fieldset");
    fieldset.append(element("legend", "Quyền ban đầu"));
    const checks = permissionChoices([]);
    fieldset.append(checks);
    form.append(fieldset);
    const submit = element("button", "Tạo tài khoản nhân viên", "dash-button");
    submit.type = "submit";
    form.append(submit);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      submit.disabled = true;
      try {
        const values = Object.fromEntries(new FormData(form).entries());
        values.permissions = [...form.querySelectorAll('input[name="permissions"]:checked')]
          .map(input => input.value);
        await request(`/api/owner/stores/${context.selectedStoreId}/staff`, {
          method: "POST", body: JSON.stringify(values)
        });
        form.reset();
        setStatus("Đã tạo tài khoản nhân viên.", "success");
        await loadStaffList(list);
      } catch (error) {
        setStatus(error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });
    createCard.append(form);
    const staffCard = addCard("Danh sách nhân viên");
    const list = element("ul", undefined, "dash-list management-staff-list");
    staffCard.append(list);
    await loadStaffList(list);
  }

  async function loadStaffList(list) {
    list.setAttribute("aria-busy", "true");
    list.replaceChildren(loadingNotice("Đang tải nhân viên…", "li"));
    try {
      const people = await request(`/api/owner/stores/${context.selectedStoreId}/staff`);
      list.replaceChildren();
      list.setAttribute("aria-busy", "false");
      if (!people.length) list.append(element("li", "Cửa hàng chưa có nhân viên.", "dash-empty"));
      people.forEach(person => {
        const item = element("li", undefined, "management-staff-item");
        const top = element("div", undefined, "management-staff-top");
        const identity = element("div");
        identity.append(element("strong", person.fullName),
          element("span", `${person.username} · ${person.email} · ${person.phone}`, "dash-muted"));
        const controls = element("div", " ", "management-staff-actions");
        controls.append(statusBadge(person.status));
        const toggle = button(person.status === "ACTIVE" ? "Tạm khóa" : "Kích hoạt",
          person.status === "ACTIVE" ? "danger" : "secondary", async () => {
            toggle.disabled = true;
            try {
              await request(`/api/owner/stores/${context.selectedStoreId}/staff/${person.membershipId}/status`, {
                method: "PATCH",
                body: JSON.stringify({ status: person.status === "ACTIVE" ? "DISABLED" : "ACTIVE" })
              });
              await loadStaffList(list);
              setStatus("Đã cập nhật trạng thái nhân viên.", "success");
            } catch (error) {
              setStatus(error.message, "error");
              toggle.disabled = false;
            }
          });
        controls.append(toggle);
        top.append(identity, controls);
        item.append(top);
        list.append(item);
      });
    } catch (error) {
      list.setAttribute("aria-busy", "false");
      list.replaceChildren(element("li", error.message, "dash-empty"));
      setStatus(error.message, "error");
    }
  }

  async function renderPermissions() {
    document.querySelector(".management-page-description").textContent =
      "Cấp đúng những quyền cần thiết cho từng nhân viên trong cửa hàng đang chọn.";
    if (!context.selectedStoreId) return noStoreNotice();
    const card = addCard("Quyền theo nhân viên",
      "Các quyền được máy chủ kiểm tra tại từng API; thay đổi tại đây có hiệu lực trong cửa hàng đang chọn.");
    const list = element("ul", undefined, "dash-list management-permission-list");
    card.append(list);
    list.setAttribute("aria-busy", "true");
    list.append(loadingNotice("Đang tải nhân viên…", "li"));
    try {
      const people = await request(`/api/owner/stores/${context.selectedStoreId}/staff`);
      list.replaceChildren();
      list.setAttribute("aria-busy", "false");
      if (!people.length) list.append(element("li", "Chưa có nhân viên để phân quyền.", "dash-empty"));
      people.forEach(person => {
        const item = element("li", undefined, "management-permission-item");
        const header = element("div", undefined, "management-staff-top");
        const identity = element("div");
        identity.append(element("strong", person.fullName),
          element("span", person.username, "dash-muted"));
        header.append(identity, statusBadge(person.status));
        const form = element("form", undefined, "management-permission-form");
        form.append(permissionChoices(person.permissions || [], true));
        const save = element("button", "Lưu quyền", "dash-button");
        save.type = "submit";
        form.append(save);
        form.addEventListener("submit", async event => {
          event.preventDefault();
          save.disabled = true;
          try {
            const permissions = [...form.querySelectorAll('input[name="permissions"]:checked')]
              .map(input => input.value);
            await request(`/api/owner/stores/${context.selectedStoreId}/staff/${person.membershipId}/permissions`, {
              method: "PUT", body: JSON.stringify({ permissions })
            });
            setStatus(`Đã cập nhật quyền cho ${person.fullName}.`, "success");
          } catch (error) {
            setStatus(error.message, "error");
          } finally {
            save.disabled = false;
          }
        });
        item.append(header, form);
        list.append(item);
      });
    } catch (error) {
      list.setAttribute("aria-busy", "false");
      list.replaceChildren(element("li", error.message, "dash-empty"));
      setStatus(error.message, "error");
    }
  }

  async function renderCategories() {
    contentNode.replaceChildren();
    document.querySelector(".management-page-description").textContent =
      "Danh mục dùng chung trong catalog; thao tác tạo hiện có sẵn trên API.";
    const card = addCard("Danh mục hiện có");
    const list = element("div", undefined, "management-category-list");
    card.append(list);
    list.setAttribute("aria-busy", "true");
    list.append(loadingNotice("Đang tải danh mục…"));
    try {
      const categories = await request("/api/catalog/categories");
      list.replaceChildren();
      list.setAttribute("aria-busy", "false");
      if (!categories.length) list.append(element("div", "Chưa có danh mục.", "dash-empty"));
      categories.forEach(category => {
        const row = element("div", undefined, "management-category-row");
        row.append(element("strong", category.name), element("span", category.slug, "dash-muted"));
        if (category.description) row.append(element("p", category.description));
        list.append(row);
      });
    } catch (error) {
      list.setAttribute("aria-busy", "false");
      list.replaceChildren(element("div", error.message, "dash-empty"));
      setStatus(error.message, "error");
    }
    const formCard = addCard("Tạo danh mục", "Chức năng API hiện hỗ trợ tạo mới; sửa/xóa danh mục chưa được cung cấp.");
    const form = element("form", undefined, "dash-form management-form");
    form.append(field("Tên danh mục", "name", "text", true), field("Slug", "slug", "text", true),
      field("Mô tả", "description", "text"));
    const submit = element("button", "Tạo danh mục", "dash-button");
    submit.type = "submit";
    form.append(submit);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      submit.disabled = true;
      try {
        const values = Object.fromEntries([...new FormData(form).entries()].filter(([, value]) => value !== ""));
        await request("/api/owner/categories", { method: "POST", body: JSON.stringify(values) });
        form.reset();
        setStatus("Đã tạo danh mục.", "success");
        await renderCategories();
      } catch (error) {
        setStatus(error.message, "error");
      } finally {
        submit.disabled = false;
      }
    });
    formCard.append(form);
  }

  async function productData() {
    return request(`/api/owner/stores/${encodeURIComponent(context.selectedStoreId)}/products`);
  }

  function renderProductCard(product, inventoryMode, categories = []) {
    const card = element("article", undefined, "management-product-card");
    if (inventoryMode) card.classList.add("inventory-mode");
    if (product.imageUrl) {
      const image = document.createElement("img");
      image.src = product.imageUrl;
      image.alt = "";
      image.loading = "lazy";
      image.referrerPolicy = "no-referrer";
      card.append(image);
    } else {
      card.append(element("div", product.name.slice(0, 1).toUpperCase(), "management-product-placeholder"));
    }
    const details = element("div", undefined, "management-product-details");
    details.append(element("span", product.categoryName, "management-eyebrow"),
      element("h3", product.name), element("span", `SKU ${product.sku}`, "dash-muted"),
      element("strong", `${Number(product.price).toLocaleString("vi-VN")} ${product.currency}`),
      statusBadge(product.status));
    if (inventoryMode) {
      const hasStockValues = product.quantity !== null && product.quantity !== undefined
        && product.reservedQuantity !== null && product.reservedQuantity !== undefined
        && Number.isFinite(Number(product.quantity))
        && Number.isFinite(Number(product.reservedQuantity));
      const availableQuantity = hasStockValues
        ? Number(product.quantity) - Number(product.reservedQuantity)
        : null;
      const summary = element("div", undefined, "management-inventory-summary");
      [
        ["Tồn hiện tại", product.quantity],
        ["Đã giữ", product.reservedQuantity],
        ["Khả dụng", availableQuantity]
      ].forEach(([label, value]) => {
        const metric = element("span", undefined, "management-inventory-value");
        metric.append(element("small", label),
          element("strong", value === null || value === undefined
            ? "—" : Number(value).toLocaleString("vi-VN")));
        summary.append(metric);
      });
      const hasReorderLevel = product.reorderLevel !== null && product.reorderLevel !== undefined
        && Number.isFinite(Number(product.reorderLevel));
      const reorderLevel = Number(product.reorderLevel);
      const stockState = !hasStockValues || !hasReorderLevel
        ? ["Chưa đủ dữ liệu ngưỡng", "pending"]
        : availableQuantity <= 0
          ? ["Hết hàng", "disabled"]
          : availableQuantity <= reorderLevel
            ? ["Tồn kho thấp", "pending"]
            : ["Đủ hàng", ""];
      const stockBadge = statusBadge(stockState[0]);
      if (stockState[1]) stockBadge.classList.add(stockState[1]);
      summary.append(stockBadge);
      details.append(summary);
    }
    const actions = element("div", undefined, "management-product-actions");
    if (inventoryMode && has("MANAGE_INVENTORY")) {
      actions.append(button("Điều chỉnh tồn", "secondary", () => editInventory(product)));
    }
    if (!inventoryMode && has("MANAGE_PRODUCTS")) {
      actions.append(button("Sửa sản phẩm", "secondary", () => editProduct(product, categories)));
      actions.append(button("Ẩn sản phẩm", "danger", async () => {
        if (!window.confirm(`Ẩn "${product.name}" khỏi cửa hàng này? Lịch sử đơn hàng được giữ nguyên.`)) return;
        try {
          await request(`/api/owner/stores/${context.selectedStoreId}/products/${product.id}`, { method: "DELETE" });
          setStatus("Đã ẩn sản phẩm khỏi cửa hàng.", "success");
          await renderProducts();
        } catch (error) {
          setStatus(error.message, "error");
        }
      }));
    }
    card.append(details, actions);
    return card;
  }

  function openDialog(title, form, className = "", onClose = () => {}) {
    const dialog = document.createElement("dialog");
    dialog.className = `management-dialog ${className}`.trim();
    const formHeader = element("div", undefined, "management-dialog-heading");
    formHeader.append(element("h2", title));
    const close = button("×", "secondary management-dialog-close", () => dialog.close());
    close.setAttribute("aria-label", "Đóng");
    formHeader.append(close);
    dialog.append(formHeader, form);
    document.body.append(dialog);
    dialog.addEventListener("close", () => {
      onClose();
      dialog.remove();
    }, { once: true });
    dialog.showModal();
    return dialog;
  }

  function categorySelect(categories, selectedId) {
    const wrapper = element("div", undefined, "dash-field");
    const id = `management-category-${Math.random().toString(36).slice(2, 9)}`;
    const label = element("label", "Danh mục");
    label.htmlFor = id;
    const select = document.createElement("select");
    select.id = id;
    select.name = "categoryId";
    select.required = true;
    categories.forEach(category => {
      const option = element("option", category.name);
      option.value = category.id;
      option.selected = String(category.id) === String(selectedId);
      select.append(option);
    });
    wrapper.append(label, select);
    return wrapper;
  }

  function uploadProductImage(storeId, file, onProgress) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", `${API}/api/owner/stores/${encodeURIComponent(storeId)}/products/images`);
      xhr.withCredentials = true;
      xhr.setRequestHeader("X-Requested-With", "fetch");
      xhr.upload.addEventListener("progress", event => {
        if (event.lengthComputable) onProgress(Math.round(event.loaded / event.total * 100));
      });
      xhr.addEventListener("error", () => reject(new TypeError("Không thể kết nối máy chủ.")));
      xhr.addEventListener("load", () => {
        let result;
        try {
          result = JSON.parse(xhr.responseText);
        } catch {
          result = null;
        }
        if (xhr.status === 401) {
          AppRoutes.handleSessionExpired(role);
          reject(new Error("Phiên đăng nhập đã hết hạn."));
          return;
        }
        if (xhr.status < 200 || xhr.status >= 300 || !result || !result.success) {
          const error = new Error(result && result.message || "Không thể tải ảnh lên kho lưu trữ.");
          error.code = result && result.code;
          error.status = xhr.status;
          reject(error);
          return;
        }
        resolve(result.data);
      });
      const data = new FormData();
      data.append("file", file);
      xhr.send(data);
    });
  }

  function productImageEditor(initialUrls) {
    const editor = element("section", undefined, "management-image-editor");
    const dropzone = element("div", undefined, "management-image-dropzone");
    dropzone.setAttribute("aria-describedby", "management-image-status");
    const preview = document.createElement("img");
    preview.alt = "Ảnh chính xem trước";
    preview.hidden = true;
    const placeholder = element("div", undefined, "management-image-placeholder");
    placeholder.append(element("strong", "Thư viện ảnh sản phẩm"),
      element("span", "Tối đa 8 ảnh · PNG, JPG hoặc WEBP · tối đa 5 MB mỗi ảnh"));
    dropzone.append(preview, placeholder);

    const controls = element("div", undefined, "management-image-controls");
    const fileInput = document.createElement("input");
    fileInput.type = "file";
    fileInput.accept = "image/png,image/jpeg,image/webp";
    fileInput.multiple = true;
    fileInput.className = "management-file-input";
    fileInput.tabIndex = -1;
    fileInput.setAttribute("aria-label", "Chọn tối đa 8 ảnh sản phẩm");
    const choose = button("Thêm ảnh", "secondary", () => fileInput.click());
    const clear = button("Gỡ tất cả ảnh", "secondary", clearImages);
    const actions = element("div", undefined, "management-image-actions");
    actions.append(choose, clear);
    const imageUrlField = field("Thêm ảnh bằng đường dẫn HTTPS", "galleryImageUrl", "url", false);
    imageUrlField.classList.add("management-image-url");
    const imageUrlInput = imageUrlField.querySelector("input");
    const addUrl = button("Thêm đường dẫn", "secondary", addImageUrl);
    const status = element("p", "Chưa chọn ảnh mới.", "management-image-status dash-muted");
    status.id = "management-image-status";
    status.setAttribute("role", "status");
    status.setAttribute("aria-live", "polite");
    const progress = document.createElement("progress");
    progress.className = "management-image-progress";
    progress.max = 100;
    progress.value = 0;
    progress.hidden = true;
    progress.setAttribute("aria-label", "Tiến độ tải ảnh");
    controls.append(fileInput, actions, imageUrlField, addUrl, status, progress);
    const gallery = element("div", undefined, "management-image-gallery");
    gallery.setAttribute("aria-label", "Thư viện ảnh, ảnh đầu tiên là ảnh chính");
    editor.append(dropzone, controls, gallery);

    const entries = initialUrls.map(url => ({ url, file: null, objectUrl: null }));
    let busy = false;

    function setStatus(message, isError = false) {
      status.textContent = message;
      status.classList.toggle("error", isError);
    }

    function setBusy(isBusy, message) {
      busy = isBusy;
      editor.setAttribute("aria-busy", String(isBusy));
      editor.querySelectorAll("button").forEach(control => { control.disabled = isBusy; });
      fileInput.disabled = isBusy;
      imageUrlInput.disabled = isBusy;
      if (message) setStatus(message);
    }

    function releaseEntry(entry) {
      if (entry.objectUrl) URL.revokeObjectURL(entry.objectUrl);
      entry.objectUrl = null;
    }

    function entrySource(entry) {
      return entry.objectUrl || entry.url;
    }

    function moveEntry(index, offset) {
      if (busy) return;
      const targetIndex = index + offset;
      if (targetIndex < 0 || targetIndex >= entries.length) return;
      [entries[index], entries[targetIndex]] = [entries[targetIndex], entries[index]];
      render();
      setStatus("Đã đổi thứ tự ảnh. Ảnh đầu tiên sẽ là ảnh chính khi lưu.");
    }

    function removeEntry(index) {
      if (busy) return;
      releaseEntry(entries[index]);
      entries.splice(index, 1);
      render();
      setStatus("Ảnh đã được gỡ khỏi danh sách; thay đổi có hiệu lực khi lưu sản phẩm.");
    }

    function render() {
      gallery.replaceChildren();
      entries.forEach((entry, index) => {
        const card = element("article", undefined, "management-image-card");
        const image = document.createElement("img");
        image.src = entrySource(entry);
        image.alt = `Ảnh sản phẩm ${index + 1}`;
        image.loading = "lazy";
        image.addEventListener("error", () => {
          card.classList.add("management-image-card-error");
          image.alt = `Không thể tải ảnh ${index + 1}`;
          setStatus(`Không thể xem trước ảnh ${index + 1}; hãy kiểm tra URL hoặc chọn ảnh khác.`, true);
        }, { once: true });
        const main = button(index === 0 ? "Ảnh chính" : "Đặt làm ảnh chính",
          index === 0 ? "primary" : "secondary", () => moveEntry(index, -index));
        main.setAttribute("aria-pressed", String(index === 0));
        const ordering = element("div", undefined, "management-image-order");
        ordering.append(button("Lên", "secondary", () => moveEntry(index, -1)),
          button("Xuống", "secondary", () => moveEntry(index, 1)));
        ordering.firstElementChild.disabled = index === 0;
        ordering.lastElementChild.disabled = index === entries.length - 1;
        const replace = button("Thay ảnh", "secondary", () => {
          fileInput.dataset.replaceIndex = String(index);
          fileInput.click();
        });
        const remove = button("Xóa", "secondary", () => removeEntry(index));
        [main, ...ordering.children, replace, remove].forEach(control => {
          control.disabled = busy;
        });
        card.append(image, element("span", `${index + 1} / ${entries.length}`,
          index === 0 ? "management-image-main-label" : "management-image-index"),
        main, ordering, replace, remove);
        gallery.append(card);
      });
      const mainEntry = entries[0];
      preview.hidden = !mainEntry;
      placeholder.hidden = Boolean(mainEntry);
      if (mainEntry) {
        preview.src = entrySource(mainEntry);
      } else {
        preview.removeAttribute("src");
      }
      clear.hidden = entries.length === 0;
      choose.disabled = busy || entries.length >= 8;
      clear.disabled = busy || entries.length === 0;
      addUrl.disabled = busy || entries.length >= 8;
      fileInput.disabled = busy;
      imageUrlInput.disabled = busy;
      setStatus(`${entries.length} / 8 ảnh${entries.length ? " · ảnh đầu tiên là ảnh chính" : ""}`);
    }

    function clearImages() {
      if (busy) return;
      entries.forEach(releaseEntry);
      entries.splice(0);
      imageUrlInput.value = "";
      fileInput.value = "";
      render();
      setStatus("Toàn bộ ảnh sẽ được gỡ khi lưu sản phẩm.");
    }

    function validateFile(file) {
      const extension = file.name.split(".").pop().toLowerCase();
      if (!["png", "jpg", "jpeg", "webp"].includes(extension)
          || (file.type && !["image/png", "image/jpeg", "image/webp"].includes(file.type))) {
        return "Chỉ hỗ trợ ảnh PNG, JPG hoặc WEBP.";
      }
      if (file.size > 5 * 1024 * 1024) {
        return "Mỗi ảnh sản phẩm không được vượt quá 5 MB.";
      }
      return "";
    }

    function chooseFiles(files) {
      if (busy) return;
      const replacingIndex = fileInput.dataset.replaceIndex;
      delete fileInput.dataset.replaceIndex;
      const fileList = Array.from(files || []);
      if (!fileList.length) return;
      let selectionError = "";
      if (replacingIndex !== undefined) {
        const file = fileList[0];
        const invalidReason = validateFile(file);
        if (invalidReason) {
          setStatus(`${file.name}: ${invalidReason}`, true);
          return;
        }
        releaseEntry(entries[Number(replacingIndex)]);
        entries[Number(replacingIndex)] = {
          url: "",
          file,
          objectUrl: URL.createObjectURL(file)
        };
        if (fileList.length > 1) {
          selectionError = "Đã thay một ảnh; để thêm ảnh khác, dùng nút Thêm ảnh.";
        }
      } else {
        const rejected = [];
        fileList.forEach(file => {
          const invalidReason = validateFile(file);
          if (invalidReason) {
            rejected.push(`${file.name}: ${invalidReason}`);
            return;
          }
          if (entries.length >= 8) {
            rejected.push(`${file.name}: mỗi sản phẩm chỉ được có tối đa 8 ảnh.`);
            return;
          }
          if (entries.some(entry => entry.file && entry.file.name === file.name
              && entry.file.size === file.size && entry.file.lastModified === file.lastModified)) {
            rejected.push(`${file.name}: ảnh đã có trong danh sách.`);
            return;
          }
          entries.push({ url: "", file, objectUrl: URL.createObjectURL(file) });
        });
        selectionError = rejected.join(" ");
      }
      fileInput.value = "";
      imageUrlInput.value = "";
      render();
      setStatus(selectionError || "Ảnh đã được chọn. Nội dung tải lên khi lưu sản phẩm.",
        Boolean(selectionError));
    }

    function addImageUrl() {
      if (busy) return;
      const value = imageUrlInput.value.trim();
      if (!value) {
        setStatus("Nhập URL ảnh trước khi thêm.", true);
        return;
      }
      try {
        const url = new URL(value);
        if (!["http:", "https:"].includes(url.protocol) || !url.hostname) {
          throw new Error("scheme");
        }
      } catch {
        setStatus("Đường dẫn ảnh phải là URL HTTP hoặc HTTPS hợp lệ.", true);
        return;
      }
      if (entries.length >= 8) {
        setStatus("Mỗi sản phẩm chỉ được có tối đa 8 ảnh.", true);
        return;
      }
      if (entries.some(entry => entry.url === value)) {
        setStatus("Ảnh này đã có trong danh sách.", true);
        return;
      }
      entries.push({ url: value, file: null, objectUrl: null });
      imageUrlInput.value = "";
      render();
      setStatus("Đã thêm URL ảnh. Thay đổi có hiệu lực khi lưu sản phẩm.");
    }

    fileInput.addEventListener("change", () => chooseFiles(fileInput.files));
    dropzone.addEventListener("click", () => fileInput.click());
    dropzone.addEventListener("keydown", event => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        fileInput.click();
      }
    });
    dropzone.tabIndex = 0;
    dropzone.setAttribute("role", "button");
    dropzone.setAttribute("aria-label", "Chọn hoặc thả tối đa 8 ảnh sản phẩm vào đây");
    dropzone.addEventListener("dragover", event => {
      event.preventDefault();
      dropzone.classList.add("dragging");
    });
    dropzone.addEventListener("dragleave", () => dropzone.classList.remove("dragging"));
    dropzone.addEventListener("drop", event => {
      event.preventDefault();
      dropzone.classList.remove("dragging");
      chooseFiles(event.dataTransfer.files);
    });
    preview.addEventListener("error", () => {
      preview.hidden = true;
      setStatus("Không thể hiển thị ảnh chính. Kiểm tra đường dẫn hoặc chọn ảnh khác.", true);
    });

    render();
    return {
      element: editor,
      pendingFiles: () => entries
        .map((entry, index) => entry.file ? { index, file: entry.file } : null)
        .filter(Boolean),
      urls: () => entries.map(entry => entry.url),
      setBusy,
      setError(message) {
        setBusy(false);
        progress.hidden = true;
        progress.value = 0;
        render();
        setStatus(message, true);
      },
      setUploadedUrl(index, url) {
        const entry = entries[index];
        if (!entry) throw new Error("Không tìm thấy ảnh vừa tải lên trong danh sách.");
        releaseEntry(entry);
        entry.url = url;
        entry.file = null;
        progress.hidden = true;
        progress.value = 0;
        render();
      },
      setUploadProgress(index, percent) {
        progress.hidden = false;
        progress.value = percent;
        setStatus(`Đang tải ảnh ${index + 1}/${entries.length}: ${percent}%`);
      },
      destroy() {
        entries.forEach(releaseEntry);
      }
    };
  }

  async function editProduct(product, categories) {
    if (!categories.length) {
      setStatus("Không có danh mục để gán sản phẩm.", "error");
      return;
    }
    let initialImages = [];
    if (product) {
      try {
        const gallery = await request(
          `/api/owner/stores/${context.selectedStoreId}/products/${product.id}/images`
        );
        initialImages = gallery.map(image => image.imageUrl);
      } catch (error) {
        setStatus(`Không thể tải thư viện ảnh: ${error.message}`, "error");
        return;
      }
    }
    const form = element("form", undefined, "dash-form management-dialog-form");
    const imageEditor = productImageEditor(initialImages);
    form.append(imageEditor.element, categorySelect(categories, product && product.categoryId),
      field("SKU", "sku", "text", true, product ? product.sku : ""),
      field("Tên sản phẩm", "name", "text", true, product ? product.name : ""),
      field("Slug", "slug", "text", true, product ? product.slug : ""),
      field("Giá (VND)", "price", "number", true, product ? product.price : ""));
    if (product) {
      form.elements.sku.readOnly = true;
      form.elements.slug.readOnly = true;
    }
    form.elements.price.min = "0.0001";
    form.elements.price.step = "0.0001";
    form.append(field("Mô tả", "description", "textarea", false, product ? product.description : ""));
    if (!product && has("MANAGE_INVENTORY")) {
      form.append(field("Số lượng ban đầu", "quantity", "number", true, 0),
        field("Mức cảnh báo tồn kho", "reorderLevel", "number", true, 0));
    }
    const footer = element("div", undefined, "management-dialog-actions");
    const cancel = button("Hủy", "secondary", () => form.closest("dialog").close());
    const save = element("button", product ? "Lưu thay đổi" : "Tạo sản phẩm", "dash-button");
    save.type = "submit";
    footer.append(cancel, save);
    form.append(footer);
    let originalImageUrls = initialImages.slice();
    let pendingUploadedImageUrls = [];
    let imageSaved = false;
    let productPersisted = false;
    let persistedPrimaryImageUrl = "";
    const isNewProduct = !product;
    form.addEventListener("submit", async event => {
      event.preventDefault();
      save.disabled = true;
      const dialog = form.closest("dialog");
      dialog.querySelectorAll(".management-dialog-close, .management-dialog-actions button")
        .forEach(control => { control.disabled = true; });
      try {
        const files = imageEditor.pendingFiles();
        imageEditor.setBusy(true, files.length
          ? `Đang tải ${files.length} ảnh lên kho lưu trữ…`
          : "Đang lưu sản phẩm…");
        for (const { index, file } of files) {
          const uploaded = await uploadProductImage(context.selectedStoreId, file,
            percent => imageEditor.setUploadProgress(index, percent));
          pendingUploadedImageUrls.push(uploaded.imageUrl);
          imageEditor.setUploadedUrl(index, uploaded.imageUrl);
        }
        const values = Object.fromEntries([...new FormData(form).entries()].filter(([, value]) => value !== ""));
        delete values.galleryImageUrl;
        values.categoryId = Number(values.categoryId);
        values.currency = product ? product.currency : "VND";
        const imageUrls = imageEditor.urls();
        values.imageUrl = imageUrls[0] || "";
        persistedPrimaryImageUrl = values.imageUrl;
        if (values.quantity !== undefined) values.quantity = Number(values.quantity);
        if (values.reorderLevel !== undefined) values.reorderLevel = Number(values.reorderLevel);
        const savedProduct = await request(product
          ? `/api/owner/stores/${context.selectedStoreId}/products/${product.id}`
          : `/api/owner/stores/${context.selectedStoreId}/products`, {
          method: product ? "PUT" : "POST", body: JSON.stringify(values)
        });
        if (savedProduct && savedProduct.id) {
          product = savedProduct;
          form.elements.sku.readOnly = true;
          form.elements.slug.readOnly = true;
        }
        productPersisted = true;
        save.textContent = "Lưu thay đổi";
        if (!product || !product.id) {
          throw new Error("Sản phẩm đã lưu nhưng máy chủ không trả về mã sản phẩm để lưu thư viện ảnh.");
        }
        await request(
          `/api/owner/stores/${context.selectedStoreId}/products/${product.id}/images`, {
            method: "PUT", body: JSON.stringify({ imageUrls })
          }
        );
        imageSaved = true;
        let message = isNewProduct ? "Đã thêm sản phẩm." : "Đã cập nhật sản phẩm.";
        let kind = "success";
        const removedImages = originalImageUrls.filter(url => !imageUrls.includes(url));
        for (const imageUrl of removedImages) {
          try {
            const deleted = await request(
              `/api/owner/stores/${context.selectedStoreId}/products/images`,
              { method: "DELETE", body: JSON.stringify({ imageUrl }) }
            );
            if (!deleted) message += " Một ảnh đã gỡ khỏi thư viện nhưng không thuộc kho ảnh này.";
          } catch (cleanupError) {
            message += ` Ảnh đã gỡ khỏi thư viện nhưng chưa thể xóa khỏi kho: ${cleanupError.message}`;
            kind = "error";
          }
        }
        pendingUploadedImageUrls = [];
        originalImageUrls = imageUrls;
        form.closest("dialog").close();
        setStatus(message, kind);
        await renderProducts();
      } catch (error) {
        const message = error.code === "IMAGE_STORAGE_UNAVAILABLE"
          ? "Kho lưu trữ ảnh chưa được cấu hình. Ảnh chưa được tải lên; hãy cấu hình storage trước khi thử lại."
          : productPersisted
            ? `Sản phẩm đã lưu, nhưng thư viện ảnh chưa được cập nhật: ${error.message}`
            : error.message;
        imageEditor.setError(message);
        setStatus(message, "error");
        save.disabled = false;
        dialog.querySelectorAll(".management-dialog-close, .management-dialog-actions button")
          .forEach(control => { control.disabled = false; });
      }
    });
    const dialog = openDialog(product ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm", form,
      "product-image-dialog", () => {
        imageEditor.destroy();
        if (!imageSaved && pendingUploadedImageUrls.length) {
          const referencedUrl = productPersisted ? persistedPrimaryImageUrl : null;
          pendingUploadedImageUrls
            .filter(imageUrl => imageUrl !== referencedUrl)
            .forEach(imageUrl => {
              request(`/api/owner/stores/${context.selectedStoreId}/products/images`, {
                method: "DELETE",
                body: JSON.stringify({ imageUrl })
              }).catch(error => setStatus(
                `Không thể dọn ảnh vừa tải lên (${imageUrl}): ${error.message}`, "error"));
            });
        }
      });
    dialog.addEventListener("cancel", event => {
      if (save.disabled) event.preventDefault();
    });
  }

  function editInventory(product) {
    const form = element("form", undefined, "dash-form management-dialog-form");
    const quantityField = field("Tồn hiện tại mới", "quantity", "number", true, product.quantity);
    const quantityInput = quantityField.querySelector("input");
    const reorderField = field("Mức cảnh báo nhập thêm", "reorderLevel", "number", true, product.reorderLevel);
    form.append(quantityField, reorderField);
    const wrapper = element("div", undefined, "dash-field");
    const id = "management-inventory-status";
    const label = element("label", "Trạng thái tồn kho");
    label.htmlFor = id;
    const select = document.createElement("select");
    select.id = id;
    select.name = "status";
    [["ACTIVE", "Đang bán"], ["INACTIVE", "Tạm ẩn"]].forEach(([value, text]) => {
      const option = element("option", text);
      option.value = value;
      option.selected = value === product.status;
      select.append(option);
    });
    wrapper.append(label, select);
    const hasStockValues = product.quantity !== null && product.quantity !== undefined
      && product.reservedQuantity !== null && product.reservedQuantity !== undefined
      && Number.isFinite(Number(product.quantity))
      && Number.isFinite(Number(product.reservedQuantity));
    const availableQuantity = hasStockValues
      ? Number(product.quantity) - Number(product.reservedQuantity)
      : null;
    const stockSummary = element("div", undefined, "management-inventory-summary");
    const summaryValue = value => value === null || value === undefined
      || !Number.isFinite(Number(value)) ? "—" : Number(value).toLocaleString("vi-VN");
    [
      ["Tồn hiện tại", product.quantity],
      ["Đã giữ", product.reservedQuantity],
      ["Khả dụng", availableQuantity]
    ].forEach(([labelText, value]) => {
      const metric = element("span", undefined, "management-inventory-value");
      metric.append(element("small", labelText), element("strong", summaryValue(value)));
      stockSummary.append(metric);
    });
    const estimatedAvailableMetric = element("span", undefined, "management-inventory-value");
    const estimatedAvailableLabel = element("small", "Khả dụng nếu lưu (ước tính)");
    const estimatedAvailableValue = element("strong", "—");
    estimatedAvailableMetric.append(estimatedAvailableLabel, estimatedAvailableValue);
    stockSummary.append(estimatedAvailableMetric);
    stockSummary.setAttribute("aria-label", "Số lượng tồn hiện tại, đã giữ và khả dụng");
    form.append(stockSummary);
    const updateAvailableEstimate = () => {
      const newQuantity = quantityInput.valueAsNumber;
      estimatedAvailableValue.textContent = hasStockValues && Number.isSafeInteger(newQuantity)
        && newQuantity >= 0
        ? summaryValue(newQuantity - Number(product.reservedQuantity))
        : "—";
    };
    quantityInput.addEventListener("input", updateAvailableEstimate);
    updateAvailableEstimate();
    form.append(wrapper);
    form.append(element("p",
      "Khả dụng = tồn hiện tại − đã giữ. Số khả dụng sau khi lưu chỉ là ước tính; máy chủ là nguồn xác nhận cuối cùng. Hàng đã giữ do đơn hàng quản lý.",
      "dash-muted full"));
    const footer = element("div", undefined, "management-dialog-actions");
    footer.append(button("Hủy", "secondary", () => form.closest("dialog").close()));
    const save = element("button", "Lưu tồn kho", "dash-button");
    save.type = "submit";
    footer.append(save);
    form.append(footer);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      save.disabled = true;
      try {
        const values = Object.fromEntries(new FormData(form).entries());
        values.quantity = Number(values.quantity);
        values.reorderLevel = Number(values.reorderLevel);
        await request(`/api/owner/stores/${context.selectedStoreId}/inventory/${product.id}`, {
          method: "PATCH", body: JSON.stringify(values)
        });
        form.closest("dialog").close();
        setStatus("Đã cập nhật số lượng và trạng thái tồn kho.", "success");
        await renderInventory();
      } catch (error) {
        setStatus(error.message, "error");
        save.disabled = false;
      }
    });
    openDialog(`Tồn kho · ${product.name}`, form);
  }

  async function renderProducts() {
    contentNode.replaceChildren();
    document.querySelector(".management-page-description").textContent =
      "Quản lý thông tin hiển thị, giá và trạng thái sản phẩm trong cửa hàng đang chọn.";
    if (!context.selectedStoreId) return noStoreNotice();
    const card = addCard("Danh mục sản phẩm");
    const toolbar = element("div", undefined, "management-toolbar");
    const search = field("Tìm tên, SKU hoặc danh mục", "search", "search");
    toolbar.append(search);
    if (has("MANAGE_PRODUCTS")) {
      toolbar.append(button("＋ Thêm sản phẩm", "", async () => {
        try {
          const categories = await request("/api/catalog/categories");
          await editProduct(null, categories);
        } catch (error) {
          setStatus(error.message, "error");
        }
      }));
    }
    card.append(toolbar);
    const grid = element("div", undefined, "management-product-grid");
    card.append(grid);
    grid.setAttribute("aria-busy", "true");
    grid.append(loadingNotice("Đang tải sản phẩm…"));
    try {
      const [products, categories] = await Promise.all([
        productData(),
        has("MANAGE_PRODUCTS") ? request("/api/catalog/categories") : Promise.resolve([])
      ]);
      grid.replaceChildren();
      grid.setAttribute("aria-busy", "false");
      if (!products.length) grid.append(element("div", "Chưa có sản phẩm trong cửa hàng.", "dash-empty"));
      products.forEach(product => grid.append(renderProductCard(product, false, categories)));
      const noMatches = element("div", "Không tìm thấy sản phẩm phù hợp.", "dash-empty");
      noMatches.hidden = true;
      noMatches.setAttribute("role", "status");
      grid.append(noMatches);
      search.querySelector("input").addEventListener("input", event => {
        const term = event.target.value.trim().toLocaleLowerCase("vi");
        let visibleCount = 0;
        [...grid.children].forEach((node, index) => {
          if (node === noMatches) return;
          const product = products[index];
          const hidden = Boolean(term && !`${product.name} ${product.sku} ${product.categoryName}`
            .toLocaleLowerCase("vi").includes(term));
          node.hidden = hidden;
          if (!hidden) visibleCount++;
        });
        noMatches.hidden = !term || visibleCount > 0;
      });
    } catch (error) {
      grid.setAttribute("aria-busy", "false");
      grid.replaceChildren(element("div", error.message, "dash-empty"));
      setStatus(error.message, "error");
    }
  }

  async function renderInventory() {
    contentNode.replaceChildren();
    document.querySelector(".management-page-description").textContent =
      "Theo dõi số lượng, hàng đã giữ cho đơn và ngưỡng cảnh báo nhập thêm.";
    if (!context.selectedStoreId) return noStoreNotice();
    const card = addCard("Tồn kho");
    const list = element("div", undefined, "management-inventory-list");
    card.append(list);
    list.setAttribute("aria-busy", "true");
    list.append(loadingNotice("Đang tải tồn kho…"));
    try {
      const products = await productData();
      list.replaceChildren();
      list.setAttribute("aria-busy", "false");
      if (!products.length) list.append(element("div", "Chưa có sản phẩm để quản lý tồn kho.", "dash-empty"));
      products.forEach(product => list.append(renderProductCard(product, true)));
    } catch (error) {
      list.setAttribute("aria-busy", "false");
      list.replaceChildren(element("div", error.message, "dash-empty"));
      setStatus(error.message, "error");
    }
  }

  async function renderOrders() {
    document.querySelector(".management-page-description").textContent =
      "Xử lý đơn nhận tại cửa hàng theo các chuyển trạng thái do hệ thống quy định.";
    if (!context.selectedStoreId) return noStoreNotice();
    const card = addCard("Đơn hàng của cửa hàng");
    const list = element("ul", undefined, "dash-list management-orders-list");
    card.append(list);
    setStatus("Đang tải đơn hàng…");
    await window.StoreOrders.load(context.selectedStoreId, context.permissions, role === "OWNER", list, statusNode);
  }

  async function renderNotifications() {
    contentNode.replaceChildren();
    document.querySelector(".management-page-description").textContent =
      "Thông báo gắn với tài khoản đang đăng nhập; nội dung khách hàng riêng tư không được hiển thị tại đây.";
    const card = addCard("Thông báo tài khoản");
    const toolbar = element("div", undefined, "management-toolbar");
    const readAll = button("Đánh dấu tất cả đã đọc", "secondary", async () => {
      readAll.disabled = true;
      try {
        await request("/api/notifications/read-all", { method: "PATCH" });
        await renderNotifications();
        setStatus("Đã đánh dấu tất cả thông báo là đã đọc.", "success");
      } catch (error) {
        setStatus(error.message, "error");
      } finally {
        readAll.disabled = false;
      }
    });
    toolbar.append(readAll);
    const list = element("ul", undefined, "dash-list management-notification-list");
    card.append(toolbar, list);
    list.setAttribute("aria-busy", "true");
    list.append(loadingNotice("Đang tải thông báo…", "li"));
    readAll.disabled = true;
    try {
      const notifications = await request("/api/notifications");
      list.replaceChildren();
      list.setAttribute("aria-busy", "false");
      const hasUnread = notifications.some(notification => !notification.read);
      readAll.hidden = !hasUnread;
      readAll.disabled = !hasUnread;
      if (!notifications.length) list.append(element("li", "Chưa có thông báo.", "dash-empty"));
      notifications.forEach(notification => {
        const item = element("li", undefined, notification.read
          ? "management-notification" : "management-notification unread");
        const top = element("div", undefined, "management-notification-top");
        top.append(element("strong", notification.title),
          element("time", new Date(notification.createdAt).toLocaleString("vi-VN")));
        item.append(top, element("p", notification.message));
        if (!notification.read) {
          const mark = button("Đánh dấu đã đọc", "secondary", async () => {
            mark.disabled = true;
            try {
              await request(`/api/notifications/${notification.id}/read`, { method: "PATCH" });
              await renderNotifications();
            } catch (error) {
              setStatus(error.message, "error");
              mark.disabled = false;
            }
          });
          item.append(mark);
        }
        list.append(item);
      });
    } catch (error) {
      list.setAttribute("aria-busy", "false");
      list.replaceChildren(element("li", error.message, "dash-empty"));
      setStatus(error.message, "error");
    }
  }

  async function renderPage() {
    contentNode.replaceChildren();
    const renderers = {
      dashboard: renderDashboard,
      store: renderStore,
      staff: renderStaff,
      permissions: renderPermissions,
      categories: renderCategories,
      products: renderProducts,
      inventory: renderInventory,
      orders: renderOrders,
      notifications: renderNotifications
    };
    if (renderers[page]) await renderers[page]();
  }

  async function initialize() {
    context = { user: await AppRoutes.requireAuth(role), role };
    if (!context.user) return;
    context.stores = role === "OWNER"
      ? await request("/api/owner/stores")
      : await request("/api/staff/stores");
    context.activeStores = role === "OWNER"
      ? context.stores.filter(store => store.status === "ACTIVE")
      : context.stores.filter(store => store.membershipStatus === "ACTIVE");
    const storageKey = `management-store-${context.user.id}-${role}`;
    const requestedStoreId = new URLSearchParams(location.search).get("storeId")
      || localStorage.getItem(storageKey);
    const selected = context.activeStores.find(store => String(store.id || store.storeId) === String(requestedStoreId))
      || context.activeStores[0];
    context.selectedStoreId = selected ? (selected.id || selected.storeId) : null;
    context.permissions = role === "OWNER" ? editablePermissions : selected ? selected.permissions || [] : [];
    if (context.selectedStoreId) localStorage.setItem(storageKey, context.selectedStoreId);
    if (!configureAccess()) return;
    buildShell();
    await renderPage();
  }

  initialize().catch(error => {
    root.replaceChildren(element("main", undefined, "management-load-error"));
    root.firstElementChild.append(element("h1", "Không thể mở khu vực quản lý"),
      element("p", error.message),
      (() => {
        const login = document.createElement("a");
        login.href = AppRoutes.goToLogin(role);
        login.className = "dash-button";
        login.textContent = "Đăng nhập lại";
        return login;
      })());
  });
})();

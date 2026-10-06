(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  const root = document.querySelector("#management-app");
  if (!root) return;

  const role = location.pathname.startsWith("/pages/owner/") ? "OWNER" : "STAFF";
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
      throw new Error(result && result.message || "Không thể hoàn tất yêu cầu.");
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
    const node = element("span", value, "dash-badge");
    if (["DRAFT", "PENDING", "PENDING_REVIEW", "READY_FOR_PICKUP"].includes(value)) {
      node.classList.add("pending");
    } else if (["DISABLED", "SUSPENDED", "CANCELLED", "INACTIVE"].includes(value)) {
      node.classList.add("disabled");
    }
    return node;
  }

  function has(permission) {
    return role === "OWNER" || Boolean(context.permissions && context.permissions.includes(permission));
  }

  function routeKey(view) {
    return `${area}.${view === "dashboard" ? "home" : view}`;
  }

  function links() {
    const items = [{ page: "dashboard", icon: "⌂" }];
    if (role === "OWNER") {
      items.push({ page: "store", icon: "▤" });
      if (context.stores.some(store => store.status === "ACTIVE")) {
        items.push({ page: "staff", icon: "♙" }, { page: "permissions", icon: "◈" });
      }
      items.push({ page: "categories", icon: "▦" });
    }
    if (has("VIEW_PRODUCTS") || has("MANAGE_PRODUCTS")) items.push({ page: "products", icon: "▧" });
    if (has("VIEW_INVENTORY") || has("MANAGE_INVENTORY")) items.push({ page: "inventory", icon: "▥" });
    if (has("VIEW_ORDERS") || has("MANAGE_ORDERS")) items.push({ page: "orders", icon: "▣" });
    items.push({ page: "notifications", icon: "♢" });
    return items;
  }

  function buildShell() {
    const layout = element("div", undefined, "management-layout");
    const sidebar = element("aside", undefined, "management-sidebar");
    sidebar.id = "management-sidebar";
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
      link.append(element("span", item.icon, "management-nav-icon"), element("span", labels[item.page]));
      if (item.page === page) link.classList.add("active");
      nav.append(link);
    });
    const sidebarFoot = element("div", undefined, "management-sidebar-foot");
    const shopLink = document.createElement("a");
    shopLink.href = AppRoutes.getRoute("customer.home");
    shopLink.className = "management-storefront-link";
    shopLink.append(element("span", "↗"), element("span", "Mở trang mua sắm"));
    sidebarFoot.append(shopLink);
    sidebar.append(brand, brandSub, nav, sidebarFoot);

    const main = element("main", undefined, "management-main");
    const topbar = element("header", undefined, "management-topbar");
    const menuButton = button("☰", "secondary management-menu-toggle");
    menuButton.setAttribute("aria-label", "Mở menu điều hướng");
    menuButton.setAttribute("aria-expanded", "false");
    menuButton.addEventListener("click", () => {
      const opened = layout.classList.toggle("sidebar-open");
      menuButton.setAttribute("aria-expanded", String(opened));
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
    profile.href = `/pages/auth/session.html?area=${area}`;
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
    backdrop.addEventListener("click", () => layout.classList.remove("sidebar-open"));
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
      link.append(element("span", item.icon), element("strong", labels[item.page]), element("span", "→"));
      quick.append(link);
    });
    shortcuts.append(quick);
    const canSeeProducts = has("VIEW_PRODUCTS") || has("MANAGE_PRODUCTS");
    const canSeeOrders = has("VIEW_ORDERS") || has("MANAGE_ORDERS");
    if (!canSeeProducts && !canSeeOrders) return;
    try {
      const metricResults = await Promise.all([
        canSeeProducts ? request(`/api/owner/stores/${context.selectedStoreId}/products`) : Promise.resolve(null),
        canSeeOrders ? request(`/api/stores/${context.selectedStoreId}/orders`) : Promise.resolve(null)
      ]);
      const summary = element("div", undefined, "management-metrics");
      if (metricResults[0]) {
        const products = metricResults[0];
        const card = element("article", undefined, "management-metric");
        card.append(element("span", "SẢN PHẨM"), element("strong", String(products.length)),
          element("small", "Có trong cửa hàng"));
        summary.append(card);
        if (has("VIEW_INVENTORY") || has("MANAGE_INVENTORY")) {
          const lowStock = products.filter(product =>
            Number(product.quantity || 0) <= Number(product.reorderLevel || 0)).length;
          const stock = element("article", undefined, "management-metric");
          stock.append(element("span", "CẦN KIỂM TRA TỒN"), element("strong", String(lowStock)),
            element("small", "Số lượng không vượt mức cảnh báo"));
          summary.append(stock);
        }
      }
      if (metricResults[1]) {
        const orders = metricResults[1];
        const pendingCount = orders.filter(order => ["PENDING", "CONFIRMED", "PREPARING"].includes(order.status)).length;
        const card = element("article", undefined, "management-metric");
        card.append(element("span", "ĐƠN CẦN XỬ LÝ"), element("strong", String(pendingCount)),
          element("small", `Tổng ${orders.length} đơn`));
        summary.append(card);
      }
      contentNode.insertBefore(summary, shortcuts);
    } catch (error) {
      setStatus(error.message, "error");
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
    list.replaceChildren(element("li", "Đang tải nhân viên…", "dash-empty"));
    try {
      const people = await request(`/api/owner/stores/${context.selectedStoreId}/staff`);
      list.replaceChildren();
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
    list.append(element("li", "Đang tải nhân viên…", "dash-empty"));
    try {
      const people = await request(`/api/owner/stores/${context.selectedStoreId}/staff`);
      list.replaceChildren();
      if (!people.length) list.append(element("li", "Chưa có nhân viên để phân quyền.", "dash-empty"));
      people.forEach(person => {
        const item = element("li", undefined, "management-permission-item");
        const header = element("div", undefined, "management-staff-top");
        const identity = element("div");
        identity.append(element("strong", person.fullName),
          element("span", `${person.username} · ${person.status}`, "dash-muted"));
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
    try {
      const categories = await request("/api/catalog/categories");
      if (!categories.length) list.append(element("div", "Chưa có danh mục.", "dash-empty"));
      categories.forEach(category => {
        const row = element("div", undefined, "management-category-row");
        row.append(element("strong", category.name), element("span", category.slug, "dash-muted"));
        if (category.description) row.append(element("p", category.description));
        list.append(row);
      });
    } catch (error) {
      list.append(element("div", error.message, "dash-empty"));
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
      details.append(element("span",
        `Tồn ${product.quantity ?? "—"} · Đã giữ ${product.reservedQuantity ?? "—"} · Cảnh báo ${product.reorderLevel ?? "—"}`,
        "dash-muted"));
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

  function productImageEditor(initialUrl) {
    const editor = element("section", undefined, "management-image-editor");
    const dropzone = element("div", undefined, "management-image-dropzone");
    dropzone.tabIndex = 0;
    dropzone.setAttribute("role", "button");
    dropzone.setAttribute("aria-label", "Chọn hoặc thả ảnh sản phẩm vào đây");
    const preview = document.createElement("img");
    preview.alt = "Ảnh xem trước sản phẩm";
    preview.hidden = true;
    const placeholder = element("div", undefined, "management-image-placeholder");
    placeholder.append(element("strong", "Kéo thả / chọn ảnh"),
      element("span", "PNG, JPG hoặc WEBP · tối đa 5 MB"));
    dropzone.append(preview, placeholder);

    const controls = element("div", undefined, "management-image-controls");
    const fileInput = document.createElement("input");
    fileInput.type = "file";
    fileInput.accept = "image/png,image/jpeg,image/webp";
    fileInput.className = "management-file-input";
    fileInput.tabIndex = -1;
    fileInput.setAttribute("aria-label", "Tệp ảnh sản phẩm");
    const choose = button("Chọn ảnh", "secondary", () => fileInput.click());
    const clear = button("Gỡ ảnh", "secondary", clearImage);
    clear.hidden = !initialUrl;
    const actions = element("div", undefined, "management-image-actions");
    actions.append(choose, clear);
    const imageUrlField = field("Hoặc dùng đường dẫn ảnh HTTPS", "imageUrl", "url", false, initialUrl || "");
    imageUrlField.classList.add("management-image-url");
    const imageUrlInput = imageUrlField.querySelector("input");
    const status = element("p", "Chưa chọn ảnh mới.", "management-image-status dash-muted");
    status.setAttribute("role", "status");
    status.setAttribute("aria-live", "polite");
    controls.append(fileInput, actions, imageUrlField, status);
    editor.append(dropzone, controls);

    let selectedFile = null;
    let objectUrl = null;
    let previousImageUrl = initialUrl || "";

    function setStatus(message, isError = false) {
      status.textContent = message;
      status.classList.toggle("error", isError);
    }

    function releaseObjectUrl() {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      objectUrl = null;
    }

    function showPreview(url) {
      preview.hidden = !url;
      placeholder.hidden = Boolean(url);
      if (url) {
        preview.src = url;
      } else {
        preview.removeAttribute("src");
      }
    }

    function clearImage() {
      selectedFile = null;
      releaseObjectUrl();
      previousImageUrl = "";
      imageUrlInput.value = "";
      fileInput.value = "";
      showPreview("");
      clear.hidden = true;
      setStatus("Ảnh hiện tại sẽ được gỡ khi lưu sản phẩm.");
    }

    function chooseFile(file) {
      if (!file) return;
      const extension = file.name.split(".").pop().toLowerCase();
      const supportedExtensions = ["png", "jpg", "jpeg", "webp"];
      if (!supportedExtensions.includes(extension)
          || (file.type && !["image/png", "image/jpeg", "image/webp"].includes(file.type))) {
        setStatus("Chọn tệp PNG, JPG hoặc WEBP.", true);
        selectedFile = null;
        releaseObjectUrl();
        imageUrlInput.value = previousImageUrl;
        showPreview(previousImageUrl);
        clear.hidden = !previousImageUrl;
        fileInput.value = "";
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        setStatus("Ảnh sản phẩm không được vượt quá 5 MB.", true);
        selectedFile = null;
        releaseObjectUrl();
        imageUrlInput.value = previousImageUrl;
        showPreview(previousImageUrl);
        clear.hidden = !previousImageUrl;
        fileInput.value = "";
        return;
      }
      previousImageUrl = imageUrlInput.value.trim() || previousImageUrl;
      releaseObjectUrl();
      selectedFile = file;
      objectUrl = URL.createObjectURL(file);
      imageUrlInput.value = "";
      showPreview(objectUrl);
      clear.hidden = false;
      setStatus(`${file.name} · ${(file.size / 1024 / 1024).toFixed(2)} MB · tải lên khi lưu.`);
    }

    fileInput.addEventListener("change", () => chooseFile(fileInput.files[0]));
    imageUrlInput.addEventListener("input", () => {
      selectedFile = null;
      releaseObjectUrl();
      previousImageUrl = imageUrlInput.value.trim();
      showPreview(imageUrlInput.value.trim());
      clear.hidden = !imageUrlInput.value.trim();
      setStatus(imageUrlInput.value.trim()
        ? "Đường dẫn ảnh sẽ được lưu cùng sản phẩm."
        : "Chưa chọn ảnh mới.");
    });
    dropzone.addEventListener("click", () => fileInput.click());
    dropzone.addEventListener("keydown", event => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        fileInput.click();
      }
    });
    dropzone.addEventListener("dragover", event => {
      event.preventDefault();
      dropzone.classList.add("dragging");
    });
    dropzone.addEventListener("dragleave", () => dropzone.classList.remove("dragging"));
    dropzone.addEventListener("drop", event => {
      event.preventDefault();
      dropzone.classList.remove("dragging");
      chooseFile(event.dataTransfer.files[0]);
    });
    preview.addEventListener("error", () => {
      showPreview("");
      setStatus("Không thể hiển thị ảnh. Kiểm tra đường dẫn hoặc chọn ảnh khác.", true);
    });

    showPreview(initialUrl || "");
    return {
      element: editor,
      file: () => selectedFile,
      imageUrl: imageUrlInput,
      setUploadedUrl(url) {
        selectedFile = null;
        releaseObjectUrl();
        imageUrlInput.value = url;
        showPreview(url);
        clear.hidden = false;
        setStatus("Ảnh đã tải lên. Lưu sản phẩm để áp dụng ảnh mới.");
      },
      destroy: releaseObjectUrl
    };
  }

  async function editProduct(product, categories) {
    if (!categories.length) {
      setStatus("Không có danh mục để gán sản phẩm.", "error");
      return;
    }
    const form = element("form", undefined, "dash-form management-dialog-form");
    const imageEditor = productImageEditor(product ? product.imageUrl : "");
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
    const originalImageUrl = product ? product.imageUrl : "";
    let pendingUploadedImageUrl = "";
    let imageSaved = false;
    form.addEventListener("submit", async event => {
      event.preventDefault();
      save.disabled = true;
      let uploadedImageUrl = "";
      try {
        const file = imageEditor.file();
        if (file) {
          const uploadForm = new FormData();
          uploadForm.append("file", file);
          const uploaded = await request(
            `/api/owner/stores/${context.selectedStoreId}/products/images`,
            { method: "POST", body: uploadForm }
          );
          uploadedImageUrl = uploaded.imageUrl;
          pendingUploadedImageUrl = uploadedImageUrl;
          imageEditor.setUploadedUrl(uploadedImageUrl);
        }
        const values = Object.fromEntries([...new FormData(form).entries()].filter(([, value]) => value !== ""));
        values.categoryId = Number(values.categoryId);
        values.currency = product ? product.currency : "VND";
        if (values.quantity !== undefined) values.quantity = Number(values.quantity);
        if (values.reorderLevel !== undefined) values.reorderLevel = Number(values.reorderLevel);
        await request(product
          ? `/api/owner/stores/${context.selectedStoreId}/products/${product.id}`
          : `/api/owner/stores/${context.selectedStoreId}/products`, {
          method: product ? "PUT" : "POST", body: JSON.stringify(values)
        });
        imageSaved = true;
        form.closest("dialog").close();
        let message = product ? "Đã cập nhật sản phẩm." : "Đã thêm sản phẩm.";
        let kind = "success";
        if (originalImageUrl && originalImageUrl !== values.imageUrl) {
          try {
            const deleted = await request(
              `/api/owner/stores/${context.selectedStoreId}/products/images`,
              { method: "DELETE", body: JSON.stringify({ imageUrl: originalImageUrl }) }
            );
            if (!deleted) {
              message += " Ảnh cũ đã được gỡ khỏi sản phẩm nhưng không thuộc kho ảnh này.";
            }
          } catch (cleanupError) {
            message += ` Ảnh cũ chưa thể xóa khỏi kho: ${cleanupError.message}`;
            kind = "error";
          }
        }
        pendingUploadedImageUrl = "";
        setStatus(message, kind);
        await renderProducts();
      } catch (error) {
        setStatus(error.message, "error");
        save.disabled = false;
      }
    });
    openDialog(product ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm", form,
      "product-image-dialog", () => {
        imageEditor.destroy();
        if (pendingUploadedImageUrl && !imageSaved) {
          request(`/api/owner/stores/${context.selectedStoreId}/products/images`, {
            method: "DELETE",
            body: JSON.stringify({ imageUrl: pendingUploadedImageUrl })
          }).catch(error => setStatus(`Không thể dọn ảnh vừa tải lên: ${error.message}`, "error"));
        }
      });
  }

  function editInventory(product) {
    const form = element("form", undefined, "dash-form management-dialog-form");
    form.append(field("Số lượng đang có", "quantity", "number", true, product.quantity),
      field("Mức cảnh báo nhập thêm", "reorderLevel", "number", true, product.reorderLevel));
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
    form.append(wrapper);
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
    grid.append(element("div", "Đang tải sản phẩm…", "dash-empty"));
    try {
      const [products, categories] = await Promise.all([
        productData(),
        has("MANAGE_PRODUCTS") ? request("/api/catalog/categories") : Promise.resolve([])
      ]);
      grid.replaceChildren();
      if (!products.length) grid.append(element("div", "Chưa có sản phẩm trong cửa hàng.", "dash-empty"));
      products.forEach(product => grid.append(renderProductCard(product, false, categories)));
      search.querySelector("input").addEventListener("input", event => {
        const term = event.target.value.trim().toLocaleLowerCase("vi");
        [...grid.children].forEach((node, index) => {
          const product = products[index];
          node.hidden = Boolean(term && !`${product.name} ${product.sku} ${product.categoryName}`
            .toLocaleLowerCase("vi").includes(term));
        });
      });
    } catch (error) {
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
    list.append(element("div", "Đang tải tồn kho…", "dash-empty"));
    try {
      const products = await productData();
      list.replaceChildren();
      if (!products.length) list.append(element("div", "Chưa có sản phẩm để quản lý tồn kho.", "dash-empty"));
      products.forEach(product => list.append(renderProductCard(product, true)));
    } catch (error) {
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
    try {
      const notifications = await request("/api/notifications");
      list.replaceChildren();
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

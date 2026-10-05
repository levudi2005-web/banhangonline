(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const node = document.querySelector("#staff-stores");
  const status = document.querySelector("#staff-status");
  const ordersList = document.querySelector("#staff-orders");
  const ordersStatus = document.querySelector("#staff-orders-status");
  const catalogPanel = document.querySelector("#staff-catalog-panel");
  const catalogList = document.querySelector("#staff-catalog");
  const catalogStatus = document.querySelector("#staff-catalog-status");
  const permissionLabels = {
    VIEW_ORDERS: "Xem đơn hàng",
    MANAGE_ORDERS: "Xử lý đơn hàng",
    CONFIRM_PICKUP: "Xác nhận khách nhận hàng",
    VIEW_PRODUCTS: "Xem sản phẩm",
    MANAGE_PRODUCTS: "Quản lý sản phẩm",
    VIEW_INVENTORY: "Xem tồn kho",
    MANAGE_INVENTORY: "Điều chỉnh tồn kho",
    VIEW_STAFF: "Xem nhân viên",
    MANAGE_STAFF: "Quản lý nhân viên",
    VIEW_STORES: "Xem cửa hàng",
    MANAGE_STORES: "Quản lý cửa hàng",
    VIEW_REPORTS: "Xem báo cáo",
    MANAGE_SETTINGS: "Quản lý cài đặt"
  };

  async function initialize() {
    const user = await AppRoutes.requireAuth("STAFF");
    if (!user) return;
    document.querySelector("#staff-name").textContent = user.fullName || user.username;
    const response = await fetch(`${API}/api/staff/stores`, { credentials: "include" });
    const result = await response.json().catch(() => null);
    if (!response.ok || !result || !result.success) {
      if (response.status === 401) AppRoutes.handleSessionExpired("STAFF");
      throw new Error(result && result.message || "Không thể tải cửa hàng được phân công.");
    }
    node.replaceChildren();
    if (!result.data.length) {
      const empty = document.createElement("div");
      empty.className = "dash-empty";
      empty.textContent = "Tài khoản chưa được phân công cửa hàng nào.";
      node.append(empty);
      return;
    }
    result.data.forEach(store => {
      const item = document.createElement("li");
      const name = document.createElement("strong");
      name.textContent = store.storeName;
      const permissions = document.createElement("p");
      permissions.className = "dash-muted";
      permissions.textContent = store.permissions.length
        ? `Quyền được cấp: ${store.permissions.map(permission => permissionLabels[permission] || permission).join(", ")}`
        : "Chưa được cấp quyền nghiệp vụ.";
      item.append(name, permissions);
      if (store.permissions.includes("VIEW_ORDERS") || store.permissions.includes("MANAGE_ORDERS")) {
        const openOrders = document.createElement("button");
        openOrders.type = "button";
        openOrders.className = "dash-button secondary";
        openOrders.textContent = "Xem đơn hàng";
        openOrders.addEventListener("click", () => {
          document.querySelector("#staff-orders-panel").hidden = false;
          window.StoreOrders.load(store.storeId, store.permissions, false, ordersList, ordersStatus);
        });
        item.append(openOrders);
      }
      if (["VIEW_PRODUCTS", "MANAGE_PRODUCTS", "VIEW_INVENTORY", "MANAGE_INVENTORY"]
          .some(permission => store.permissions.includes(permission))) {
        const openCatalog = document.createElement("button");
        openCatalog.type = "button";
        openCatalog.className = "dash-button secondary";
        openCatalog.textContent = "Xem sản phẩm / tồn kho";
        openCatalog.addEventListener("click", () => {
          catalogPanel.hidden = false;
          window.StoreCatalog.load(store.storeId, store.permissions, false, catalogList, catalogStatus);
        });
        item.append(openCatalog);
      }
      node.append(item);
    });
  }

  document.querySelector("#staff-logout").href = `${AppRoutes.ROUTES.staff.logout}?area=staff`;
  initialize().catch(error => {
    status.textContent = error.message;
    status.classList.add("error");
  });
})();

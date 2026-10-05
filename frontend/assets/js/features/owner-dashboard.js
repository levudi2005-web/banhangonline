(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  const $ = (selector, root = document) => root.querySelector(selector);
  const storesNode = $("#owner-stores");
  const storeSelect = $("#staff-store");
  const staffNode = $("#staff-list");
  const statusNode = $("#owner-status");
  const storeSetupPanel = $("#store-setup-panel");
  const storeForm = $("#store-form");
  const storeSaveButton = $("#store-save");
  const submitReviewButton = $("#submit-store-review");
  const staffPanel = $("#staff-panel");
  const ordersList = $("#owner-orders");
  const ordersStatus = $("#owner-orders-status");
  const catalogList = $("#owner-catalog");
  const catalogStatus = $("#owner-catalog-status");
  const permissionNames = [
    "VIEW_ORDERS", "MANAGE_ORDERS", "CONFIRM_PICKUP", "VIEW_PRODUCTS",
    "MANAGE_PRODUCTS", "VIEW_INVENTORY", "MANAGE_INVENTORY", "VIEW_STAFF",
    "MANAGE_STAFF", "VIEW_STORES", "MANAGE_STORES", "VIEW_REPORTS", "MANAGE_SETTINGS"
  ];
  let draftStoreId = null;

  function status(message, kind = "") {
    statusNode.textContent = message;
    statusNode.className = `dash-status ${kind}`;
  }

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
    if (!response.ok || !result || !result.success) {
      if (response.status === 401) AppRoutes.handleSessionExpired("OWNER");
      throw new Error(result && result.message || "Không thể tải dữ liệu từ máy chủ.");
    }
    return result.data;
  }

  function badge(value) {
    const element = document.createElement("span");
    const kind = ["PENDING", "PENDING_REVIEW", "DRAFT"].includes(value) ? "pending"
      : ["DISABLED", "SUSPENDED"].includes(value) ? "disabled" : "";
    element.className = `dash-badge ${kind}`;
    element.textContent = value;
    return element;
  }

  function fillStoreForm(store) {
    for (const [field, value] of Object.entries(store)) {
      if (storeForm.elements[field]) storeForm.elements[field].value = value ?? "";
    }
  }

  async function loadStaff(storeId) {
    if (!storeId) {
      staffPanel.classList.add("dash-hidden");
      return;
    }
    staffPanel.classList.remove("dash-hidden");
    staffNode.replaceChildren();
    const loading = document.createElement("p");
    loading.className = "dash-muted";
    loading.textContent = "Đang tải nhân viên…";
    staffNode.append(loading);
    try {
      const staff = await request(`/api/owner/stores/${encodeURIComponent(storeId)}/staff`);
      staffNode.replaceChildren();
      if (!staff.length) {
        const empty = document.createElement("div");
        empty.className = "dash-empty";
        empty.textContent = "Cửa hàng chưa có nhân viên.";
        staffNode.append(empty);
        return;
      }
      staff.forEach(person => {
        const item = document.createElement("li");
        const header = document.createElement("div");
        header.className = "dash-row";
        const details = document.createElement("div");
        const name = document.createElement("strong");
        name.textContent = person.fullName;
        const contact = document.createElement("span");
        contact.className = "dash-muted";
        contact.textContent = `${person.username} · ${person.email} · ${person.phone}`;
        details.append(name, contact);
        const controls = document.createElement("div");
        controls.className = "dash-inline";
        controls.append(badge(person.status));
        const toggle = document.createElement("button");
        toggle.className = `dash-button secondary ${person.status === "ACTIVE" ? "danger" : ""}`;
        toggle.type = "button";
        toggle.textContent = person.status === "ACTIVE" ? "Tạm khóa" : "Kích hoạt";
        toggle.addEventListener("click", async () => {
          toggle.disabled = true;
          try {
            await request(`/api/owner/stores/${storeId}/staff/${person.membershipId}/status`, {
              method: "PATCH",
              body: JSON.stringify({ status: person.status === "ACTIVE" ? "DISABLED" : "ACTIVE" })
            });
            await loadStaff(storeId);
          } catch (error) {
            status(error.message, "error");
            toggle.disabled = false;
          }
        });
        controls.append(toggle);
        header.append(details, controls);
        const permissionsForm = document.createElement("form");
        permissionsForm.className = "dash-form";
        const permissionSet = new Set(person.permissions);
        const permissionFields = document.createElement("fieldset");
        permissionFields.className = "full";
        const legend = document.createElement("legend");
        legend.textContent = "Quyền trong cửa hàng này";
        permissionFields.append(legend);
        const permissionChoices = document.createElement("div");
        permissionChoices.className = "dash-checks";
        permissionNames.forEach(permission => {
          const label = document.createElement("label");
          label.className = "dash-check";
          const checkbox = document.createElement("input");
          checkbox.type = "checkbox";
          checkbox.name = "permissions";
          checkbox.value = permission;
          checkbox.checked = permissionSet.has(permission);
          const name = document.createElement("span");
          name.textContent = permission;
          label.append(checkbox, name);
          permissionChoices.append(label);
        });
        permissionFields.append(permissionChoices);
        const savePermissions = document.createElement("button");
        savePermissions.className = "dash-button secondary";
        savePermissions.type = "submit";
        savePermissions.textContent = "Lưu quyền";
        permissionsForm.append(permissionFields, savePermissions);
        permissionsForm.addEventListener("submit", async event => {
          event.preventDefault();
          savePermissions.disabled = true;
          try {
            const permissions = [...permissionsForm.querySelectorAll('input[name="permissions"]:checked')]
              .map(input => input.value);
            await request(`/api/owner/stores/${storeId}/staff/${person.membershipId}/permissions`, {
              method: "PUT",
              body: JSON.stringify({ permissions })
            });
            status("Đã cập nhật quyền nhân viên.", "success");
            await loadStaff(storeId);
          } catch (error) {
            status(error.message, "error");
            savePermissions.disabled = false;
          }
        });
        item.append(header, permissionsForm);
        staffNode.append(item);
      });
    } catch (error) {
      staffNode.replaceChildren();
      status(error.message, "error");
    }
  }

  async function loadStores() {
    storesNode.textContent = "Đang tải cửa hàng…";
    const stores = await request("/api/owner/stores");
    storesNode.replaceChildren();
    storeSelect.replaceChildren();
    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "Chọn cửa hàng đang hoạt động";
    storeSelect.append(placeholder);
    if (!stores.length) {
      const empty = document.createElement("div");
      empty.className = "dash-empty";
      empty.textContent = "Tài khoản này chưa có cửa hàng. Hoàn tất thiết lập để tạo bản nháp.";
      storesNode.append(empty);
      storeSetupPanel.hidden = false;
      storeForm.reset();
      draftStoreId = null;
      storeSaveButton.textContent = "Lưu bản nháp";
      submitReviewButton.classList.add("dash-hidden");
      $("#store-setup-help").textContent = "Lưu thông tin dưới dạng nháp, kiểm tra lại rồi gửi quản trị viên kiểm duyệt.";
      staffPanel.classList.add("dash-hidden");
      return;
    }
    const draft = stores.find(store => store.status === "DRAFT");
    draftStoreId = draft ? draft.id : null;
    storeSetupPanel.hidden = !draft;
    submitReviewButton.classList.toggle("dash-hidden", !draft);
    if (draft) {
      fillStoreForm(draft);
      storeSaveButton.textContent = "Lưu bản nháp";
      $("#store-setup-help").textContent = "Cửa hàng vẫn là bản nháp; chỉ cửa hàng được kích hoạt mới hiển thị cho khách.";
    }
    stores.forEach(store => {
      const row = document.createElement("div");
      row.className = "dash-row";
      const info = document.createElement("div");
      const name = document.createElement("strong");
      name.textContent = store.name;
      const address = document.createElement("span");
      address.className = "dash-muted";
      address.textContent = `${store.addressDetail}, ${store.ward}, ${store.district}, ${store.province}`;
      info.append(name, address);
      row.append(info, badge(store.status));
      storesNode.append(row);
      if (store.status === "ACTIVE") {
        const option = document.createElement("option");
        option.value = store.id;
        option.textContent = store.name;
        storeSelect.append(option);
      }
    });
    const activeStores = stores.filter(store => store.status === "ACTIVE");
    if (activeStores.length) {
      storeSelect.value = String(activeStores[0].id);
    }
    const pending = stores.some(store => ["PENDING", "PENDING_REVIEW"].includes(store.status));
    $("#store-review-note").classList.toggle("dash-hidden", !pending);
    $("#store-review-note").textContent = "Cửa hàng đang chờ quản trị viên kiểm duyệt và kích hoạt.";
    staffPanel.classList.toggle("dash-hidden", !storeSelect.value);
    if (storeSelect.value) {
      await loadStaff(storeSelect.value);
      window.StoreOrders.load(storeSelect.value, [], true, ordersList, ordersStatus);
      window.StoreCatalog.load(storeSelect.value, [], true, catalogList, catalogStatus);
    }
  }

  async function initialize() {
    const user = await AppRoutes.requireAuth("OWNER");
    if (!user) return;
    $("#owner-name").textContent = user.fullName || user.username;
    await loadStores();
  }

  $("#store-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = $("button[type=submit]", form);
    button.disabled = true;
    try {
      const body = Object.fromEntries([...new FormData(form).entries()].filter(([, value]) => value !== ""));
      const path = draftStoreId ? `/api/owner/stores/${draftStoreId}` : "/api/owner/stores";
      await request(path, { method: draftStoreId ? "PUT" : "POST", body: JSON.stringify(body) });
      status("Đã lưu bản nháp cửa hàng.", "success");
      await loadStores();
    } catch (error) {
      status(error.message, "error");
    } finally {
      button.disabled = false;
    }
  });

  submitReviewButton.addEventListener("click", async () => {
    if (!draftStoreId) return;
    submitReviewButton.disabled = true;
    try {
      await request(`/api/owner/stores/${draftStoreId}/submit-review`, { method: "POST" });
      status("Đã gửi cửa hàng để quản trị viên kiểm duyệt.", "success");
      await loadStores();
    } catch (error) {
      status(error.message, "error");
    } finally {
      submitReviewButton.disabled = false;
    }
  });

  $("#create-staff-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = $("button[type=submit]", form);
    const storeId = storeSelect.value;
    if (!storeId) {
      status("Chọn cửa hàng đã được duyệt trước.", "error");
      return;
    }
    button.disabled = true;
    try {
      const body = Object.fromEntries(new FormData(form).entries());
      body.permissions = [...form.querySelectorAll('input[name="permissions"]:checked')].map(input => input.value);
      await request(`/api/owner/stores/${storeId}/staff`, { method: "POST", body: JSON.stringify(body) });
      form.reset();
      status("Đã tạo tài khoản. Hãy bàn giao mật khẩu khởi tạo cho nhân viên qua kênh an toàn.", "success");
      await loadStaff(storeId);
    } catch (error) {
      status(error.message, "error");
    } finally {
      button.disabled = false;
    }
  });

  storeSelect.addEventListener("change", () => loadStaff(storeSelect.value));
  const permissionContainer = $("#staff-permissions");
  permissionNames.forEach(name => {
    const label = document.createElement("label");
    label.className = "dash-check";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.name = "permissions";
    checkbox.value = name;
    const text = document.createElement("span");
    text.textContent = name;
    label.append(checkbox, text);
    permissionContainer.append(label);
  });
  $("#owner-logout").href = `${AppRoutes.ROUTES.owner.logout}?area=owner`;
  initialize().catch(error => status(error.message, "error"));
})();

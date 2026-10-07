(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const list = document.querySelector("#address-list");
  const status = document.querySelector("#address-status");
  const form = document.querySelector("#address-form");
  const locationPicker = LocationPicker.mount(document.querySelector("#address-location-picker"), {
    latitudeInput: form.elements.latitude,
    longitudeInput: form.elements.longitude,
    addressFields: {
      province: form.elements.province,
      district: form.elements.district,
      ward: form.elements.ward,
      addressLine: form.elements.addressLine
    }
  });
  let editingId = null;

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
      AppRoutes.handleSessionExpired("CUSTOMER");
      throw new Error("Phiên đăng nhập đã hết hạn.");
    }
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể hoàn tất yêu cầu.");
    }
    return result.data;
  }

  async function load() {
    const addresses = await request("/api/users/addresses");
    list.replaceChildren();
    if (!addresses.length) {
      const empty = document.createElement("li");
      empty.className = "dash-empty";
      empty.textContent = "Bạn chưa lưu địa chỉ nào.";
      list.append(empty);
    }
    addresses.forEach(address => {
      const item = document.createElement("li");
      const name = document.createElement("strong");
      name.textContent = address.recipientName;
      const text = document.createElement("p");
      text.textContent = `${address.addressLine}, ${address.ward}, ${address.district}, ${address.province} · ${address.phone}`;
      item.append(name, text);
      if (address.isDefault) {
        const current = document.createElement("span");
        current.className = "dash-badge";
        current.textContent = "Mặc định";
        item.append(current);
      }
      const actions = document.createElement("div");
      actions.className = "dash-inline";
      if (!address.isDefault) {
        const makeDefault = document.createElement("button");
        makeDefault.className = "dash-button secondary";
        makeDefault.type = "button";
        makeDefault.textContent = "Đặt mặc định";
        makeDefault.addEventListener("click", async () => {
          try {
            await request(`/api/users/addresses/${address.id}/default`, { method: "PATCH" });
            await load();
          } catch (error) { setError(error); }
        });
        actions.append(makeDefault);
      }
      const edit = document.createElement("button");
      edit.className = "dash-button secondary";
      edit.type = "button";
      edit.textContent = "Sửa";
      edit.addEventListener("click", () => {
        editingId = address.id;
        Object.entries(address).forEach(([key, value]) => {
          if (form.elements[key]) form.elements[key].value = value ?? "";
        });
        locationPicker.setPosition(address.latitude, address.longitude);
        form.querySelector("button[type=submit]").textContent = "Lưu địa chỉ";
        document.querySelector("#address-recipient").focus();
      });
      const remove = document.createElement("button");
      remove.className = "dash-button secondary danger";
      remove.type = "button";
      remove.textContent = "Xóa";
      remove.addEventListener("click", async () => {
        remove.disabled = true;
        try {
          await request(`/api/users/addresses/${address.id}`, { method: "DELETE" });
          await load();
        } catch (error) { setError(error); remove.disabled = false; }
      });
      actions.append(edit, remove);
      item.append(actions);
      list.append(item);
    });
  }

  function setError(error) {
    status.textContent = error.message;
    status.className = "dash-status error";
  }

  form.addEventListener("submit", async event => {
    event.preventDefault();
    const button = form.querySelector("button[type=submit]");
    button.disabled = true;
    try {
      const body = Object.fromEntries(new FormData(form).entries());
      ["latitude", "longitude"].forEach(key => {
        if (body[key] === "") delete body[key];
        else body[key] = Number(body[key]);
      });
      const method = editingId ? "PUT" : "POST";
      const path = editingId ? `/api/users/addresses/${editingId}` : "/api/users/addresses";
      await request(path, { method, body: JSON.stringify(body) });
      form.reset();
      editingId = null;
      button.textContent = "Thêm địa chỉ";
      status.textContent = "Đã lưu địa chỉ.";
      status.className = "dash-status success";
      await load();
    } catch (error) {
      setError(error);
    } finally {
      button.disabled = false;
    }
  });

  document.querySelector("#address-reset").addEventListener("click", () => {
    editingId = null;
    form.reset();
    locationPicker.setPosition(null, null);
    form.querySelector("button[type=submit]").textContent = "Thêm địa chỉ";
  });
  AppRoutes.requireAuth("CUSTOMER").then(user => {
    if (user) load().catch(setError);
  }).catch(setError);
})();

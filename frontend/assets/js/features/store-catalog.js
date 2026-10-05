(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const allowedPermissions = [
    ["VIEW_PRODUCTS", "Xem sản phẩm"], ["MANAGE_PRODUCTS", "Quản lý sản phẩm"],
    ["VIEW_INVENTORY", "Xem tồn kho"], ["MANAGE_INVENTORY", "Quản lý tồn kho"]
  ];

  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    if (className) node.className = className;
    return node;
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
    if (response.status === 401) {
      AppRoutes.handleSessionExpired("STAFF");
      throw new Error("Phiên đăng nhập đã hết hạn.");
    }
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể hoàn tất yêu cầu.");
    }
    return result.data;
  }

  function field(label, name, type = "text", required = false, value = "") {
    const wrapper = element("div", undefined, "dash-field");
    const inputId = `catalog-${name}`;
    const title = element("label", label);
    title.htmlFor = inputId;
    const input = element(type === "textarea" ? "textarea" : "input");
    input.id = inputId;
    input.name = name;
    if (type !== "textarea") input.type = type;
    input.required = required;
    if (value) input.value = value;
    if (type === "number") input.min = "0";
    wrapper.append(title, input);
    return wrapper;
  }

  async function load(storeId, permissions, isOwner, root, status) {
    root.replaceChildren();
    status.textContent = "Đang tải sản phẩm…";
    const has = name => isOwner || permissions.includes(name);
    try {
      const products = await request(`/api/owner/stores/${encodeURIComponent(storeId)}/products`);
      status.textContent = `${products.length} sản phẩm trong cửa hàng.`;
      if (!products.length) {
        root.append(element("div", "Cửa hàng chưa có sản phẩm.", "dash-empty"));
      }
      products.forEach(product => {
        const card = element("article", undefined, "dash-card");
        const title = element("strong", `${product.name} · ${product.sku}`);
        const detailText = `${product.categoryName} · ${Number(product.price).toLocaleString("vi-VN")} ${product.currency}`;
        const canViewInventory = isOwner
          || permissions.includes("VIEW_INVENTORY")
          || permissions.includes("MANAGE_INVENTORY");
        const detail = element("p", canViewInventory
          ? `${detailText} · Số lượng: ${product.quantity} · Đã giữ: ${product.reservedQuantity}`
          : detailText);
        card.append(title, detail);
        if (has("MANAGE_INVENTORY")) {
          const form = element("form", undefined, "dash-form");
          const quantity = field("Tồn kho", "quantity", "number", true, String(product.quantity));
          const reorder = field("Mức cảnh báo nhập thêm", "reorderLevel", "number", true, String(product.reorderLevel));
          const statusField = element("div", undefined, "dash-field");
          const label = element("label", "Trạng thái tồn kho");
          const select = element("select");
          select.name = "status";
          [["ACTIVE", "Đang bán"], ["INACTIVE", "Tạm ẩn"]].forEach(([value, text]) => {
            const option = element("option", text);
            option.value = value;
            select.append(option);
          });
          select.value = product.status;
          statusField.append(label, select);
          form.append(quantity, reorder, statusField);
          const save = element("button", "Lưu tồn kho", "dash-button");
          save.type = "submit";
          form.append(save);
          form.addEventListener("submit", async event => {
            event.preventDefault();
            save.disabled = true;
            try {
              const values = new FormData(form);
              await request(`/api/owner/stores/${storeId}/inventory/${product.id}`, {
                method: "PATCH",
                body: JSON.stringify({
                  quantity: Number(values.get("quantity")),
                  reorderLevel: Number(values.get("reorderLevel")),
                  status: values.get("status")
                })
              });
              await load(storeId, permissions, isOwner, root, status);
            } catch (error) {
              status.textContent = error.message;
              status.className = "dash-status error";
              save.disabled = false;
            }
          });
          card.append(form);
        } else if (canViewInventory) {
          card.append(element("p", `Mức nhập thêm: ${product.reorderLevel} · ${product.status}`));
        }
        root.append(card);
      });

      if (has("MANAGE_PRODUCTS") && has("MANAGE_INVENTORY")) {
        const categories = await request("/api/catalog/categories");
        const section = element("section", undefined, "dash-card");
        section.append(element("h3", "Thêm sản phẩm"));
        if (!categories.length && isOwner) {
          const categoryForm = element("form", undefined, "dash-form");
          categoryForm.append(field("Tên danh mục", "name", "text", true), field("Slug danh mục", "slug", "text", true));
          const description = field("Mô tả danh mục", "description", "text");
          categoryForm.append(description);
          const createCategory = element("button", "Tạo danh mục", "dash-button secondary");
          createCategory.type = "submit";
          categoryForm.append(createCategory);
          categoryForm.addEventListener("submit", async event => {
            event.preventDefault();
            createCategory.disabled = true;
            try {
              const body = Object.fromEntries([...new FormData(categoryForm).entries()].filter(([, value]) => value !== ""));
              await request("/api/owner/categories", { method: "POST", body: JSON.stringify(body) });
              await load(storeId, permissions, isOwner, root, status);
            } catch (error) {
              status.textContent = error.message;
              status.className = "dash-status error";
              createCategory.disabled = false;
            }
          });
          section.append(categoryForm);
        } else if (!categories.length) {
          section.append(element("p", "Chưa có danh mục. Hãy nhờ chủ cửa hàng tạo danh mục trước."));
        } else {
          const form = element("form", undefined, "dash-form");
          const categoryField = element("div", undefined, "dash-field");
          const label = element("label", "Danh mục");
          const select = element("select");
          select.name = "categoryId";
          select.required = true;
          categories.forEach(category => {
            const option = element("option", category.name);
            option.value = category.id;
            select.append(option);
          });
          categoryField.append(label, select);
          form.append(categoryField,
            field("SKU", "sku", "text", true),
            field("Tên sản phẩm", "name", "text", true),
            field("Slug", "slug", "text", true),
            field("Giá (VND)", "price", "number", true),
            field("Số lượng", "quantity", "number", true),
            field("Mức cảnh báo tồn kho", "reorderLevel", "number", true),
            field("Đường dẫn ảnh HTTPS", "imageUrl"),
            field("Mô tả", "description", "textarea"));
          const submit = element("button", "Tạo sản phẩm", "dash-button");
          submit.type = "submit";
          form.append(submit);
          form.addEventListener("submit", async event => {
            event.preventDefault();
            submit.disabled = true;
            try {
              const values = Object.fromEntries([...new FormData(form).entries()].filter(([, value]) => value !== ""));
              values.categoryId = Number(values.categoryId);
              values.quantity = Number(values.quantity);
              values.reorderLevel = Number(values.reorderLevel);
              values.currency = "VND";
              await request(`/api/owner/stores/${storeId}/products`, {
                method: "POST", body: JSON.stringify(values)
              });
              await load(storeId, permissions, isOwner, root, status);
            } catch (error) {
              status.textContent = error.message;
              status.className = "dash-status error";
              submit.disabled = false;
            }
          });
          section.append(form);
        }
        root.append(section);
      } else {
        const assigned = allowedPermissions.filter(([name]) => has(name)).map(([, label]) => label);
        status.textContent += assigned.length ? ` Quyền: ${assigned.join(", ")}.` : "";
      }
    } catch (error) {
      status.textContent = error.message;
      status.className = "dash-status error";
    }
  }

  window.StoreCatalog = Object.freeze({ load });
})();

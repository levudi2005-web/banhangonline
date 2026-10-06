(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");

  function can(permissions, name, isOwner) {
    return isOwner || permissions.includes(name);
  }

  const statusLabels = {
    CANCELLED: "Đã hủy",
    COMPLETED: "Đã nhận hàng",
    CONFIRMED: "Đã xác nhận",
    PENDING: "Chờ xác nhận",
    PREPARING: "Đang chuẩn bị",
    READY_FOR_PICKUP: "Sẵn sàng nhận"
  };

  function orderStatusBadge(status) {
    const badge = document.createElement("span");
    badge.className = "dash-badge";
    badge.textContent = statusLabels[status] || status;
    if (["PENDING", "READY_FOR_PICKUP"].includes(status)) {
      badge.classList.add("pending");
    } else if (status === "CANCELLED") {
      badge.classList.add("disabled");
    }
    return badge;
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
      AppRoutes.handleSessionExpired(location.pathname.startsWith("/pages/owner/") ? "OWNER" : "STAFF");
      throw new Error("Phiên đăng nhập đã hết hạn.");
    }
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể hoàn tất yêu cầu.");
    }
    return result.data;
  }

  async function load(storeId, permissions, isOwner, list, status) {
    list.replaceChildren();
    list.setAttribute("aria-busy", "true");
    status.textContent = "Đang tải đơn hàng…";
    status.className = "dash-status";
    try {
      const orders = await request(`/api/stores/${encodeURIComponent(storeId)}/orders`);
      list.setAttribute("aria-busy", "false");
      status.textContent = `${orders.length} đơn hàng.`;
      if (!orders.length) {
        const empty = document.createElement("li");
        empty.className = "dash-empty";
        empty.textContent = "Chưa có đơn hàng.";
        list.append(empty);
      }
      orders.forEach(order => {
        const item = document.createElement("li");
        const heading = document.createElement("div");
        heading.className = "management-order-heading";
        const code = document.createElement("strong");
        code.textContent = order.orderCode;
        heading.append(code, orderStatusBadge(order.status));
        const date = document.createElement("p");
        date.className = "dash-muted";
        date.textContent = new Date(order.createdAt).toLocaleString("vi-VN");
        const products = document.createElement("p");
        products.textContent = order.items.map(row => `${row.productName} × ${row.quantity}`).join(" · ");
        const total = document.createElement("p");
        total.textContent = `Tổng ${Number(order.totalAmount).toLocaleString("vi-VN")} ${order.currency} · thanh toán tại cửa hàng`;
        item.append(heading, date, products, total);
        const actions = document.createElement("div");
        actions.className = "dash-inline";
        const next = {
          PENDING: ["CONFIRMED", "Xác nhận"],
          CONFIRMED: ["PREPARING", "Bắt đầu chuẩn bị"],
          PREPARING: ["READY_FOR_PICKUP", "Sẵn sàng nhận"]
        }[order.status];
        if (next && can(permissions, "MANAGE_ORDERS", isOwner)) {
          const button = document.createElement("button");
          button.type = "button";
          button.className = "dash-button";
          button.textContent = next[1];
          button.addEventListener("click", async () => {
            button.disabled = true;
            try {
              await request(`/api/stores/${storeId}/orders/${order.id}/status`, {
                method: "PATCH", body: JSON.stringify({ status: next[0] })
              });
              await load(storeId, permissions, isOwner, list, status);
            } catch (error) {
              list.setAttribute("aria-busy", "false");
              status.textContent = error.message;
              status.className = "dash-status error";
              button.disabled = false;
            }
          });
          actions.append(button);
          if (["PENDING", "CONFIRMED", "PREPARING"].includes(order.status)) {
            const cancel = document.createElement("button");
            cancel.type = "button";
            cancel.className = "dash-button secondary danger";
            cancel.textContent = "Hủy đơn";
            cancel.addEventListener("click", async () => {
              cancel.disabled = true;
              try {
                await request(`/api/stores/${storeId}/orders/${order.id}/status`, {
                  method: "PATCH", body: JSON.stringify({ status: "CANCELLED", note: "Cửa hàng đã hủy đơn" })
                });
                await load(storeId, permissions, isOwner, list, status);
              } catch (error) {
                status.textContent = error.message;
                status.className = "dash-status error";
                cancel.disabled = false;
              }
            });
            actions.append(cancel);
          }
        }
        if (order.status === "READY_FOR_PICKUP" && can(permissions, "CONFIRM_PICKUP", isOwner)) {
          const input = document.createElement("input");
          input.inputMode = "numeric";
          input.maxLength = 10;
          input.pattern = "\\d{10}";
          input.setAttribute("aria-label", `Mã nhận hàng của đơn ${order.orderCode}`);
          input.placeholder = "Mã nhận hàng 10 số";
          const confirm = document.createElement("button");
          confirm.type = "button";
          confirm.className = "dash-button";
          confirm.textContent = "Xác nhận nhận hàng";
          confirm.addEventListener("click", async () => {
            if (!/^\d{10}$/.test(input.value)) {
              status.textContent = "Nhập đủ 10 chữ số của mã nhận hàng.";
              status.className = "dash-status error";
              input.focus();
              return;
            }
            confirm.disabled = true;
            try {
              await request(`/api/stores/${storeId}/orders/${order.id}/pickup-confirmation`, {
                method: "POST", body: JSON.stringify({ pickupCode: input.value })
              });
              await load(storeId, permissions, isOwner, list, status);
            } catch (error) {
              status.textContent = error.message;
              status.className = "dash-status error";
              confirm.disabled = false;
            }
          });
          actions.append(input, confirm);
        }
        if (actions.childElementCount) item.append(actions);
        list.append(item);
      });
    } catch (error) {
      status.textContent = error.message;
      status.className = "dash-status error";
    }
  }

  window.StoreOrders = Object.freeze({ load });
})();

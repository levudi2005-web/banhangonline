(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const list = document.querySelector("#customer-orders");
  const status = document.querySelector("#orders-status");

  async function load() {
    const user = await AppRoutes.requireAuth("CUSTOMER");
    if (!user) return;
    const response = await fetch(`${API}/api/customer/orders`, { credentials: "include" });
    const result = await response.json().catch(() => null);
    if (response.status === 401) {
      AppRoutes.handleSessionExpired("CUSTOMER");
      return;
    }
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể tải đơn hàng.");
    }
    list.replaceChildren();
    if (!result.data.length) {
      const empty = document.createElement("li");
      empty.className = "dash-empty";
      empty.textContent = "Bạn chưa có đơn hàng nào.";
      list.append(empty);
      return;
    }
    result.data.forEach(order => {
      const item = document.createElement("li");
      const title = document.createElement("strong");
      title.textContent = `${order.orderCode} · ${order.storeName}`;
      const state = document.createElement("span");
      state.className = "dash-badge";
      state.textContent = order.status;
      const date = document.createElement("p");
      date.className = "dash-muted";
      date.textContent = new Date(order.createdAt).toLocaleString("vi-VN");
      const rows = document.createElement("p");
      rows.textContent = order.items.map(line => `${line.productName} × ${line.quantity}`).join(" · ");
      const total = document.createElement("p");
      total.textContent = `Tổng: ${Number(order.totalAmount).toLocaleString("vi-VN")} ${order.currency}`;
      item.append(title, state, date, rows, total);
      if (order.status === "PENDING") {
        const cancel = document.createElement("button");
        cancel.className = "dash-button secondary danger";
        cancel.type = "button";
        cancel.textContent = "Hủy đơn";
        cancel.addEventListener("click", async () => {
          cancel.disabled = true;
          try {
            const r = await fetch(`${API}/api/customer/orders/${order.id}/cancel`, {
              method: "POST", credentials: "include",
              headers: { "X-Requested-With": "fetch" }
            });
            const d = await r.json().catch(() => null);
            if (!r.ok || !d || !d.success) throw new Error(d && d.message || "Không thể hủy đơn.");
            await load();
          } catch (error) {
            status.textContent = error.message;
            status.className = "dash-status error";
            cancel.disabled = false;
          }
        });
        item.append(cancel);
      }
      list.append(item);
    });
  }

  load().catch(error => {
    status.textContent = error.message;
    status.classList.add("error");
  });
})();

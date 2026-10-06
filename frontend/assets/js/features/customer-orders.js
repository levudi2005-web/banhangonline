(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const list = document.querySelector("#customer-orders");
  const status = document.querySelector("#orders-status");
  const requestedOrderId = location.pathname.match(/^\/don-hang\/(\d+)$/)?.[1] || "";
  const orderSteps = [
    ["PENDING", "Chờ xác nhận"],
    ["CONFIRMED", "Đã xác nhận"],
    ["PREPARING", "Đang chuẩn bị"],
    ["READY_FOR_PICKUP", "Sẵn sàng nhận"],
    ["COMPLETED", "Đã nhận hàng"]
  ];
  const statusLabels = Object.fromEntries(orderSteps);
  statusLabels.CANCELLED = "Đã hủy";

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
      item.id = `customer-order-${order.id}`;
      item.dataset.orderId = String(order.id);
      if (requestedOrderId && String(order.id) === requestedOrderId) {
        item.classList.add("customer-order-selected");
      }
      const title = document.createElement("strong");
      const orderLink = document.createElement("a");
      orderLink.href = AppRoutes.getRoute("customer.orders", { orderId: order.id });
      orderLink.textContent = `${order.orderCode} · ${order.storeName}`;
      title.append(orderLink);
      const state = document.createElement("span");
      state.className = "dash-badge";
      if (order.status === "CANCELLED") state.classList.add("disabled");
      else if (order.status === "PENDING" || order.status === "READY_FOR_PICKUP") state.classList.add("pending");
      state.textContent = statusLabels[order.status] || order.status;
      const date = document.createElement("p");
      date.className = "dash-muted";
      date.textContent = new Date(order.createdAt).toLocaleString("vi-VN");
      const rows = document.createElement("p");
      rows.textContent = order.items.map(line => `${line.productName} × ${line.quantity}`).join(" · ");
      const total = document.createElement("p");
      total.textContent = `Tổng: ${Number(order.totalAmount).toLocaleString("vi-VN")} ${order.currency}`;
      item.append(title, state, date, rows, total);
      const activeStep = orderSteps.findIndex(([key]) => key === order.status);
      if (activeStep >= 0) {
        const progress = document.createElement("ol");
        progress.className = "order-progress";
        progress.setAttribute("aria-label", "Tiến độ đơn hàng");
        orderSteps.forEach(([key, label], index) => {
          const step = document.createElement("li");
          step.className = index < activeStep ? "complete" : index === activeStep ? "current" : "";
          const marker = document.createElement("span");
          marker.className = "order-progress-marker";
          marker.setAttribute("aria-hidden", "true");
          const text = document.createElement("span");
          text.className = "order-progress-label";
          text.textContent = label;
          step.append(marker, text);
          progress.append(step);
        });
        item.append(progress);
      } else if (order.status === "CANCELLED") {
        const cancelled = document.createElement("p");
        cancelled.className = "order-cancelled-note";
        cancelled.textContent = "Đơn hàng này đã kết thúc và không còn trong tiến trình nhận hàng.";
        item.append(cancelled);
      }
      if (requestedOrderId && String(order.id) === requestedOrderId && order.history.length) {
        const history = document.createElement("ol");
        history.className = "order-history";
        history.setAttribute("aria-label", "Lịch sử cập nhật đơn hàng");
        order.history.forEach(entry => {
          const event = document.createElement("li");
          const eventTitle = document.createElement("strong");
          eventTitle.textContent = statusLabels[entry.status] || entry.status;
          const eventDate = document.createElement("time");
          eventDate.dateTime = entry.createdAt;
          eventDate.textContent = new Date(entry.createdAt).toLocaleString("vi-VN");
          event.append(eventTitle, eventDate);
          if (entry.note) {
            const note = document.createElement("span");
            note.textContent = entry.note;
            event.append(note);
          }
          history.append(event);
        });
        item.append(history);
      }
      if (requestedOrderId && String(order.id) === requestedOrderId) {
        requestAnimationFrame(() => item.scrollIntoView({ block: "center" }));
      }
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

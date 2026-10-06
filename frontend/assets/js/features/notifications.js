(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const list = document.querySelector("#notifications");
  const status = document.querySelector("#notification-status");
  const readAllButton = document.querySelector("#notifications-read-all");
  const notificationTypes = {
    ORDER_CREATED: "Đơn hàng",
    ORDER_STATUS: "Cập nhật đơn hàng",
    ORDER_READY: "Sẵn sàng nhận hàng",
    ORDER_COMPLETED: "Hoàn tất đơn hàng",
    ORDER_CANCELLED: "Hủy đơn hàng"
  };

  async function load() {
    if (!await AppRoutes.requireAuth("CUSTOMER")) return;
    status.textContent = "Đang tải thông báo…";
    status.className = "dash-status";
    const response = await fetch(`${API}/api/notifications`, { credentials: "include" });
    const result = await response.json().catch(() => null);
    if (response.status === 401) {
      AppRoutes.handleSessionExpired("CUSTOMER");
      return;
    }
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể tải thông báo.");
    }
    list.replaceChildren();
    const unreadCount = result.data.filter(notification => !notification.read).length;
    status.textContent = unreadCount ? `${unreadCount} thông báo chưa đọc.` : `${result.data.length} thông báo.`;
    readAllButton.hidden = unreadCount === 0;
    if (!result.data.length) {
      const empty = document.createElement("li");
      empty.className = "dash-empty";
      empty.textContent = "Chưa có thông báo.";
      list.append(empty);
    }
    result.data.forEach(notification => {
      const item = document.createElement("li");
      item.className = notification.read ? "notification-item" : "notification-item unread";
      const heading = document.createElement("div");
      heading.className = "notification-heading";
      const title = document.createElement("strong");
      title.textContent = notification.title;
      heading.append(title);
      if (!notification.read) {
        const badge = document.createElement("span");
        badge.className = "notification-new-badge";
        badge.textContent = "MỚI";
        heading.append(badge);
      }
      const message = document.createElement("p");
      message.textContent = notification.message;
      const date = document.createElement("span");
      date.className = "dash-muted";
      date.textContent = new Date(notification.createdAt).toLocaleString("vi-VN");
      if (notification.createdAt) item.dataset.createdAt = notification.createdAt;
      const type = document.createElement("span");
      type.className = "notification-type";
      type.textContent = notificationTypes[notification.type] || notification.type;
      item.append(heading, type, message, date);
      if (!notification.read) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "dash-button secondary";
        button.textContent = "Đánh dấu đã đọc";
        button.addEventListener("click", async () => {
          button.disabled = true;
          try {
            const r = await fetch(`${API}/api/notifications/${notification.id}/read`, {
              method: "PATCH", credentials: "include", headers: { "X-Requested-With": "fetch" }
            });
            const d = await r.json().catch(() => null);
            if (!r.ok || !d || !d.success) throw new Error(d && d.message || "Không thể cập nhật thông báo.");
            await load();
          } catch (error) {
            status.textContent = error.message;
            status.className = "dash-status error";
            button.disabled = false;
          }
        });
        item.append(button);
      }
      list.append(item);
    });
  }

  readAllButton.addEventListener("click", async () => {
    readAllButton.disabled = true;
    try {
      const response = await fetch(`${API}/api/notifications/read-all`, {
        method: "PATCH", credentials: "include", headers: { "X-Requested-With": "fetch" }
      });
      const result = await response.json().catch(() => null);
      if (!response.ok || !result || !result.success) {
        throw new Error(result && result.message || "Không thể cập nhật thông báo.");
      }
      await load();
      status.textContent = "Đã đánh dấu tất cả thông báo là đã đọc.";
      status.className = "dash-status success";
    } catch (error) {
      status.textContent = error.message;
      status.className = "dash-status error";
    } finally {
      readAllButton.disabled = false;
    }
  });

  load().catch(error => {
    status.textContent = error.message;
    status.className = "dash-status error";
  });
})();

(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const list = document.querySelector("#notifications");
  const status = document.querySelector("#notification-status");

  async function load() {
    if (!await AppRoutes.requireAuth()) return;
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
    if (!result.data.length) {
      const empty = document.createElement("li");
      empty.className = "dash-empty";
      empty.textContent = "Chưa có thông báo.";
      list.append(empty);
    }
    result.data.forEach(notification => {
      const item = document.createElement("li");
      const title = document.createElement("strong");
      title.textContent = notification.title;
      const message = document.createElement("p");
      message.textContent = notification.message;
      const date = document.createElement("span");
      date.className = "dash-muted";
      date.textContent = new Date(notification.createdAt).toLocaleString("vi-VN");
      item.append(title, message, date);
      if (!notification.read) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "dash-button secondary";
        button.textContent = "Đánh dấu đã đọc";
        button.addEventListener("click", async () => {
          const r = await fetch(`${API}/api/notifications/${notification.id}/read`, {
            method: "PATCH", credentials: "include", headers: { "X-Requested-With": "fetch" }
          });
          const d = await r.json().catch(() => null);
          if (!r.ok || !d || !d.success) throw new Error(d && d.message || "Không thể cập nhật thông báo.");
          await load();
        });
        item.append(button);
      }
      list.append(item);
    });
  }

  document.querySelector("#notifications-read-all").addEventListener("click", async () => {
    try {
      const response = await fetch(`${API}/api/notifications/read-all`, {
        method: "PATCH", credentials: "include", headers: { "X-Requested-With": "fetch" }
      });
      const result = await response.json().catch(() => null);
      if (!response.ok || !result || !result.success) {
        throw new Error(result && result.message || "Không thể cập nhật thông báo.");
      }
      await load();
    } catch (error) {
      status.textContent = error.message;
      status.className = "dash-status error";
    }
  });

  load().catch(error => {
    status.textContent = error.message;
    status.classList.add("error");
  });
})();

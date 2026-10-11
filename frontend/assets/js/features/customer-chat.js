(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  let conversation = null;
  let currentUserId = null;
  let selectedStoreId = "";
  let selectedOrderId = null;
  let pollTimer = null;

  const launcher = document.createElement("button");
  launcher.type = "button";
  launcher.className = "chat-launcher";
  launcher.textContent = "Chat với cửa hàng";
  launcher.setAttribute("aria-expanded", "false");
  launcher.setAttribute("aria-controls", "customer-chat-panel");

  const panel = document.createElement("section");
  panel.id = "customer-chat-panel";
  panel.className = "chat-panel";
  panel.hidden = true;
  panel.setAttribute("aria-label", "Chat với cửa hàng");
  const heading = document.createElement("header");
  heading.className = "chat-panel-header";
  const title = document.createElement("strong");
  title.textContent = "Chat với cửa hàng";
  const close = document.createElement("button");
  close.type = "button";
  close.className = "chat-close";
  close.textContent = "Đóng";
  close.setAttribute("aria-label", "Đóng khung chat");
  heading.append(title, close);
  const notice = document.createElement("p");
  notice.className = "chat-status";
  notice.setAttribute("role", "status");
  const messages = document.createElement("ol");
  messages.className = "chat-messages";
  messages.setAttribute("aria-live", "polite");
  const form = document.createElement("form");
  form.className = "chat-compose";
  const input = document.createElement("textarea");
  input.maxLength = 4000;
  input.rows = 2;
  input.placeholder = "Nhập tin nhắn…";
  input.setAttribute("aria-label", "Nội dung tin nhắn");
  input.required = true;
  const send = document.createElement("button");
  send.type = "submit";
  send.className = "dash-button";
  send.textContent = "Gửi";
  form.append(input, send);
  panel.append(heading, notice, messages, form);
  document.body.append(launcher, panel);

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
      const returnUrl = `${location.pathname}${location.search}`;
      location.assign(AppRoutes.goToLogin("CUSTOMER", returnUrl));
      throw new Error("Vui lòng đăng nhập để chat với cửa hàng.");
    }
    if (!response.ok || !result || !result.success) {
      throw new Error(result && result.message || "Không thể kết nối cuộc trò chuyện.");
    }
    return result.data;
  }

  function showError(error) {
    notice.textContent = error instanceof TypeError
      ? "Không thể kết nối máy chủ. Vui lòng thử lại."
      : error.message;
    notice.classList.add("error");
  }

  function renderMessages(items) {
    messages.replaceChildren();
    items.forEach(message => {
      const item = document.createElement("li");
      item.className = Number(message.senderUserId) === Number(currentUserId)
        ? "chat-message mine" : "chat-message";
      const text = document.createElement("p");
      text.textContent = message.messageText;
      const time = document.createElement("time");
      time.textContent = new Date(message.createdAt).toLocaleString("vi-VN");
      item.append(text, time);
      messages.append(item);
    });
    messages.scrollTop = messages.scrollHeight;
  }

  async function refresh() {
    if (!conversation || panel.hidden) return;
    try {
      const items = await request(`/api/chat/conversations/${conversation.id}/messages`);
      renderMessages(items);
      await request(`/api/chat/conversations/${conversation.id}/read`, { method: "PATCH" });
      notice.textContent = conversation.storeName || "Đang trò chuyện với cửa hàng";
      notice.classList.remove("error");
    } catch (error) {
      showError(error);
    } finally {
      if (!panel.hidden) pollTimer = setTimeout(refresh, 3500);
    }
  }

  async function open(storeId, orderId) {
    panel.hidden = false;
    launcher.setAttribute("aria-expanded", "true");
    selectedStoreId = String(storeId || localStorage.getItem("selectedStoreId") || "");
    selectedOrderId = orderId ? Number(orderId) : null;
    notice.textContent = "Đang mở cuộc trò chuyện…";
    notice.classList.remove("error");
    try {
      if (!selectedStoreId || !/^\d+$/.test(selectedStoreId)) {
        const response = await fetch(`${API}/api/stores`, { credentials: "include" });
        const result = await response.json().catch(() => null);
        if (!response.ok || !result || !result.success || !result.data.length) {
          throw new Error("Hiện chưa có cửa hàng để bắt đầu trò chuyện.");
        }
        selectedStoreId = String(result.data[0].id);
      }
      const user = await request("/api/auth/me");
      currentUserId = user.id;
      conversation = await request("/api/customer/chat/conversations", {
        method: "POST",
        body: JSON.stringify({
          storeId: Number(selectedStoreId),
          ...(selectedOrderId ? { orderId: selectedOrderId } : {})
        })
      });
      title.textContent = `Chat với ${conversation.storeName || "cửa hàng"}`;
      clearTimeout(pollTimer);
      await refresh();
      input.focus();
    } catch (error) {
      showError(error);
    }
  }

  window.StoreChat = { open };
  launcher.addEventListener("click", () => open());
  close.addEventListener("click", () => {
    panel.hidden = true;
    launcher.setAttribute("aria-expanded", "false");
    clearTimeout(pollTimer);
  });
  document.addEventListener("click", event => {
    const trigger = event.target.closest("[data-chat-store]");
    if (!trigger) return;
    const storeId = trigger.dataset.chatStore === "selected"
      ? document.querySelector("#store-select")?.value || localStorage.getItem("selectedStoreId")
      : trigger.dataset.chatStore;
    open(storeId, trigger.dataset.chatOrder);
  });
  form.addEventListener("submit", async event => {
    event.preventDefault();
    if (!conversation || !input.value.trim()) return;
    send.disabled = true;
    try {
      await request(`/api/chat/conversations/${conversation.id}/messages`, {
        method: "POST",
        body: JSON.stringify({ messageText: input.value })
      });
      input.value = "";
      clearTimeout(pollTimer);
      await refresh();
    } catch (error) {
      showError(error);
    } finally {
      send.disabled = false;
    }
  });
})();

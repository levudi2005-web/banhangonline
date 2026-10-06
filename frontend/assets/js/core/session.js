(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const requestedArea = new URLSearchParams(location.search).get("area");
  const pathArea = location.pathname.startsWith("/quan-ly/")
    || location.pathname.startsWith("/pages/staff/") ? "staff" : "customer";
  const area = ["customer", "staff", "owner"].includes(requestedArea)
    ? requestedArea
    : document.body.dataset.authArea || pathArea;
  if (!["customer", "staff", "owner"].includes(area)) {
    location.replace(AppRoutes.ROUTES.public.notFound);
    return;
  }
  const role = AppRoutes.roleForArea(area);
  const state = document.querySelector("#session-state");
  const accountInfo = document.querySelector("#account-info");
  const error = document.querySelector("#session-error");
  const retry = document.querySelector("#session-retry");

  const showError = message => {
    if (state) state.hidden = true;
    if (error) {
      error.textContent = message;
      error.hidden = false;
      error.classList.add("show");
    }
    if (retry) retry.hidden = false;
  };

  async function loadSession() {
    if (state) {
      state.hidden = false;
      state.textContent = "Đang tải thông tin tài khoản…";
      state.classList.add("show");
    }
    if (error) {
      error.hidden = true;
      error.classList.remove("show");
    }
    if (retry) retry.hidden = true;
    try {
      const user = await AppRoutes.requireAuth(role);
      if (!user) return;
      const roles = Array.isArray(user.roles) ? user.roles : [];
      const customerLinks = document.querySelector("#customer-links");
      if (customerLinks) customerLinks.hidden = !roles.includes("CUSTOMER");

      const accountName = document.querySelector("#account-name");
      const accountRoles = document.querySelector("#account-roles");
      const accountPhone = document.querySelector("#account-phone");
      if (accountName) accountName.textContent = user.fullName || user.username || "";
      if (accountRoles) accountRoles.textContent = roles.join(", ");
      if (accountPhone) accountPhone.textContent = user.phone || "Chưa cung cấp";

      const form = document.querySelector("#profile-form");
      if (form) {
        form.elements.fullName.value = user.fullName || "";
        form.elements.username.value = user.username || "";
        form.elements.email.value = user.email || "";
      }
      const logoutLink = document.querySelector("#logout-link");
      if (logoutLink) {
        logoutLink.href = `${AppRoutes.ROUTES[area].logout}?area=${encodeURIComponent(area)}`;
      }
      document.body.classList.toggle("staff", area !== "customer");
      if (state) state.hidden = true;
      if (accountInfo) accountInfo.hidden = false;
    } catch (loadError) {
      showError(loadError instanceof TypeError
        ? "Không thể kết nối máy chủ. Kiểm tra kết nối rồi thử lại."
        : loadError.message);
    }
  }

  async function logout() {
    const submit = document.querySelector("#logout-submit");
    if (!submit) return;
    submit.disabled = true;
    if (state) {
      state.textContent = "Đang thu hồi phiên đăng nhập…";
      state.hidden = false;
      state.classList.add("show");
    }
    if (error) error.hidden = true;
    try {
      const response = await fetch(`${API}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
        headers: { "X-Requested-With": "fetch" }
      });
      const result = await response.json().catch(() => null);
      if (!response.ok || !result || !result.success) {
        throw new Error(result && result.message || "Không thể thu hồi phiên đăng nhập.");
      }
      location.replace(AppRoutes.goToLogin(role));
    } catch (logoutError) {
      submit.disabled = false;
      showError(logoutError instanceof TypeError
        ? "Không thể kết nối máy chủ để đăng xuất. Vui lòng thử lại."
        : logoutError.message);
    }
  }

  if (document.body.dataset.authMode === "logout") {
    document.querySelector("#logout-submit")?.addEventListener("click", logout);
  } else {
    retry?.addEventListener("click", loadSession);
    loadSession();
  }
})();

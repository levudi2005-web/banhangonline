(() => {
  "use strict";
  const API = (window.API_BASE || "").replace(/\/$/, "");
  const requestedArea = new URLSearchParams(location.search).get("area");
  const area = document.body.dataset.authArea === "staff" || requestedArea === "staff" ? "staff" : "customer";
  const state = document.querySelector("#session-state");
  const accountInfo = document.querySelector("#account-info");
  const error = document.querySelector("#session-error");

  const showError = message => {
    state.hidden = true;
    error.textContent = message;
    error.hidden = false;
    error.classList.add("show");
  };

  async function loadSession() {
    try {
      const response = await fetch(`${API}/api/auth/me`, { credentials: "include" });
      if (response.status === 401) {
        location.replace(`/pages/${area}/auth/login.html`);
        return;
      }
      if (!response.ok) throw new Error("session");

      const result = await response.json();
      if (!result.data) throw new Error("session");
      const roles = result.data.roles || [];
      const staff = roles.includes("STAFF") || roles.includes("OWNER");
      if (area === "staff" && !staff) {
        location.replace("/pages/customer/auth/login.html");
        return;
      }
      if (area === "customer" && !roles.includes("CUSTOMER") && staff) {
        location.replace("/pages/auth/session.html?area=staff");
        return;
      }
      document.querySelector("#account-name").textContent =
        result.data.fullName || result.data.username;
      document.querySelector("#account-roles").textContent =
        roles.join(", ");
      document.querySelector("#account-phone").textContent = result.data.phone || "Chưa cung cấp";
      document.querySelector("#profileFullName").value = result.data.fullName || "";
      document.querySelector("#profileUsername").value = result.data.username || "";
      document.querySelector("#profileEmail").value = result.data.email || "";
      document.querySelector("#logout-link").href = `/pages/${area}/auth/logout.html`;
      document.body.classList.toggle("staff", area === "staff");
      state.hidden = true;
      accountInfo.hidden = false;
    } catch {
      showError("Không thể tải thông tin phiên. Vui lòng thử lại.");
    }
  }

  async function logout() {
    const submit = document.querySelector("#logout-submit");
    const state = document.querySelector("#session-state");
    submit.disabled = true;
    state.textContent = "Đang thu hồi phiên đăng nhập…";
    state.hidden = false;
    state.classList.add("show");
    document.querySelector("#session-error").hidden = true;
    try {
      const response = await fetch(`${API}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
        headers: { "X-Requested-With": "fetch" }
      });
      if (!response.ok) throw new Error("logout");
      location.replace(`/pages/${area}/auth/login.html`);
    } catch {
      submit.disabled = false;
      showError("Không thể đăng xuất. Vui lòng thử lại.");
    }
  }

  if (document.body.dataset.authMode === "logout") {
    document.querySelector("#logout-submit").addEventListener("click", logout);
  } else {
    loadSession();
  }
})();

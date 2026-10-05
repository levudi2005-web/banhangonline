(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  const ROUTES = Object.freeze({
    public: {
      gateway: "/pages/auth/index.html",
      forbidden: "/pages/auth/403.html",
      notFound: "/error/404.html"
    },
    customer: {
      login: "/pages/customer/auth/login.html",
      register: "/pages/customer/auth/register.html",
      forgot: "/pages/customer/auth/forgot-password.html",
      reset: "/pages/customer/auth/reset-password.html",
      logout: "/pages/customer/auth/logout.html",
      home: "/pages/auth/session.html?area=customer"
    },
    staff: {
      login: "/pages/staff/auth/login.html?area=staff",
      forgot: "/pages/staff/auth/forgot-password.html",
      reset: "/pages/staff/auth/reset-password.html",
      logout: "/pages/staff/auth/logout.html",
      home: "/pages/auth/session.html?area=staff"
    },
    owner: {
      login: "/pages/staff/auth/login.html?area=owner",
      logout: "/pages/staff/auth/logout.html",
      home: "/pages/auth/session.html?area=owner"
    }
  });

  const routeKeys = {
    gateway: ROUTES.public.gateway,
    forbidden: ROUTES.public.forbidden,
    notFound: ROUTES.public.notFound,
    "customer.login": ROUTES.customer.login,
    "customer.register": ROUTES.customer.register,
    "customer.forgot": ROUTES.customer.forgot,
    "customer.reset": ROUTES.customer.reset,
    "customer.logout": ROUTES.customer.logout,
    "staff.login": ROUTES.staff.login,
    "staff.forgot": ROUTES.staff.forgot,
    "staff.reset": ROUTES.staff.reset,
    "staff.logout": ROUTES.staff.logout,
    "owner.login": ROUTES.owner.login,
    "owner.logout": ROUTES.owner.logout
  };

  function getRoute(routeKey, query = {}) {
    const route = routeKeys[routeKey];
    if (!route) return null;
    const url = new URL(route, location.origin);
    Object.entries(query).forEach(([key, value]) => url.searchParams.set(key, value));
    return `${url.pathname}${url.search}${url.hash}`;
  }

  function validateReturnUrl(value) {
    if (!value) return null;
    try {
      const url = new URL(value, location.origin);
      if (url.origin !== location.origin || !url.pathname.startsWith("/pages/customer/")) return null;
      return `${url.pathname}${url.search}${url.hash}`;
    } catch {
      return null;
    }
  }

  function getHomeRoute(role) {
    if (role === "CUSTOMER") return ROUTES.customer.home;
    if (role === "OWNER") return ROUTES.owner.home;
    if (role === "STAFF") return ROUTES.staff.home;
    return ROUTES.public.gateway;
  }

  function redirectByRole(role, returnUrl) {
    return role === "CUSTOMER"
      ? validateReturnUrl(returnUrl) || getHomeRoute(role)
      : getHomeRoute(role);
  }

  function goToLogin(role, returnUrl) {
    const loginRoute = role === "OWNER" ? ROUTES.owner.login
      : role === "STAFF" ? ROUTES.staff.login
        : ROUTES.customer.login;
    const safeReturnUrl = role === "CUSTOMER" ? validateReturnUrl(returnUrl) : null;
    if (!safeReturnUrl) return loginRoute;
    const url = new URL(loginRoute, location.origin);
    url.searchParams.set("returnUrl", safeReturnUrl);
    return `${url.pathname}${url.search}`;
  }

  function handleSessionExpired(role) {
    const loginUrl = new URL(goToLogin(role), location.origin);
    loginUrl.searchParams.set("expired", "1");
    location.replace(`${loginUrl.pathname}${loginUrl.search}`);
  }

  function requireRole(user, role) {
    return Boolean(user && Array.isArray(user.roles) && user.roles.includes(role));
  }

  async function requireAuth(role) {
    const response = await fetch(`${API}/api/auth/me`, { credentials: "include" });
    if (response.status === 401) {
      handleSessionExpired(role);
      return null;
    }
    const result = await response.json().catch(() => null);
    if (!response.ok || !result || !result.success || !result.data) {
      throw new Error(result && result.message || "Không thể xác minh phiên đăng nhập.");
    }
    if (role && !requireRole(result.data, role)) {
      location.replace(ROUTES.public.forbidden);
      return null;
    }
    return result.data;
  }

  function roleForArea(area) {
    if (area === "owner") return "OWNER";
    if (area === "staff") return "STAFF";
    return "CUSTOMER";
  }

  function areaForRole(role) {
    if (role === "OWNER") return "owner";
    if (role === "STAFF") return "staff";
    return "customer";
  }

  async function setupForbiddenPage() {
    const homeLink = document.querySelector("#return-home");
    const logoutLink = document.querySelector("#logout-link");
    const status = document.querySelector("#forbidden-status");
    if (!homeLink || !status) return;
    try {
      const response = await fetch(`${API}/api/auth/me`, { credentials: "include" });
      if (response.status === 401) {
        status.textContent = "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại để tiếp tục.";
        homeLink.href = ROUTES.public.gateway;
        homeLink.textContent = "Về trang đăng nhập";
        if (logoutLink) logoutLink.hidden = true;
        return;
      }
      const result = await response.json().catch(() => null);
      if (!response.ok || !result || !result.success || !result.data) {
        throw new Error(result && result.message || "Không thể xác minh phiên đăng nhập.");
      }
      const roles = Array.isArray(result.data.roles) ? result.data.roles : [];
      const role = ["OWNER", "STAFF", "CUSTOMER"].find(candidate => roles.includes(candidate));
      homeLink.href = getHomeRoute(role);
      if (logoutLink) {
        logoutLink.href = `${ROUTES[areaForRole(role)].logout}?area=${areaForRole(role)}`;
      }
      status.textContent = "Tài khoản hiện tại không được phép truy cập trang này.";
    } catch (error) {
      status.textContent = error instanceof TypeError
        ? "Không thể kết nối máy chủ để xác minh quyền truy cập."
        : error.message;
    }
  }

  document.querySelectorAll("[data-route]").forEach(link => {
    const route = getRoute(link.dataset.route);
    if (route) link.href = route;
  });

  const loginArea = new URLSearchParams(location.search).get("area");
  if (location.pathname === "/pages/staff/auth/login.html") {
    const title = document.querySelector("#management-login-title");
    const description = document.querySelector("#management-login-description");
    if (loginArea === "owner" && title && description) {
      title.textContent = "Đăng nhập chủ cửa hàng";
      description.textContent = "Dành cho tài khoản chủ cửa hàng đã được quản trị viên duyệt.";
    } else if (loginArea === "staff" && title && description) {
      title.textContent = "Đăng nhập nhân viên";
      description.textContent = "Dành cho nhân viên đã được cấp tài khoản.";
    }
  }

  window.AppRoutes = Object.freeze({
    ROUTES,
    getRoute,
    getHomeRoute,
    goToLogin,
    handleSessionExpired,
    redirectByRole,
    requireAuth,
    requireRole,
    areaForRole,
    roleForArea,
    validateReturnUrl
  });

  setupForbiddenPage();
})();

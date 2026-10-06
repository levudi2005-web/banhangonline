(() => {
  "use strict";

  const API = (window.API_BASE || "").replace(/\/$/, "");
  const ROUTES = Object.freeze({
    public: {
      gateway: "/",
      forbidden: "/403",
      notFound: "/404"
    },
    customer: {
      login: "/dang-nhap",
      register: "/dang-ky",
      forgot: "/quen-mat-khau",
      reset: "/dat-lai-mat-khau",
      logout: "/dang-xuat?area=customer",
      home: "/cua-hang",
      product: "/san-pham",
      category: "/danh-muc",
      search: "/tim-kiem",
      cart: "/gio-hang",
      checkout: "/thanh-toan",
      orders: "/don-hang",
      notifications: "/thong-bao",
      addresses: "/tai-khoan/dia-chi",
      account: "/tai-khoan"
    },
    staff: {
      login: "/quan-ly/dang-nhap?area=staff",
      forgot: "/quan-ly/quen-mat-khau?area=staff",
      reset: "/quan-ly/dat-lai-mat-khau?area=staff",
      logout: "/quan-ly/dang-xuat?area=staff",
      home: "/quan-ly/staff",
      products: "/quan-ly/staff/san-pham",
      inventory: "/quan-ly/staff/ton-kho",
      orders: "/quan-ly/staff/don-hang",
      notifications: "/quan-ly/staff/thong-bao"
    },
    owner: {
      login: "/quan-ly/dang-nhap?area=owner",
      forgot: "/quan-ly/quen-mat-khau?area=owner",
      reset: "/quan-ly/dat-lai-mat-khau?area=owner",
      logout: "/quan-ly/dang-xuat?area=owner",
      home: "/quan-ly/owner",
      store: "/quan-ly/owner/cua-hang",
      staff: "/quan-ly/owner/nhan-vien",
      permissions: "/quan-ly/owner/phan-quyen",
      categories: "/quan-ly/owner/danh-muc",
      products: "/quan-ly/owner/san-pham",
      inventory: "/quan-ly/owner/ton-kho",
      orders: "/quan-ly/owner/don-hang",
      notifications: "/quan-ly/owner/thong-bao"
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
    "customer.home": ROUTES.customer.home,
    "customer.product": ROUTES.customer.product,
    "customer.category": ROUTES.customer.category,
    "customer.search": ROUTES.customer.search,
    "customer.cart": ROUTES.customer.cart,
    "customer.checkout": ROUTES.customer.checkout,
    "customer.orders": ROUTES.customer.orders,
    "customer.account": ROUTES.customer.account,
    "customer.notifications": ROUTES.customer.notifications,
    "customer.addresses": ROUTES.customer.addresses,
    "staff.login": ROUTES.staff.login,
    "staff.forgot": ROUTES.staff.forgot,
    "staff.reset": ROUTES.staff.reset,
    "staff.logout": ROUTES.staff.logout,
    "staff.home": ROUTES.staff.home,
    "staff.products": ROUTES.staff.products,
    "staff.inventory": ROUTES.staff.inventory,
    "staff.orders": ROUTES.staff.orders,
    "staff.notifications": ROUTES.staff.notifications,
    "owner.login": ROUTES.owner.login,
    "owner.forgot": ROUTES.owner.forgot,
    "owner.reset": ROUTES.owner.reset,
    "owner.logout": ROUTES.owner.logout,
    "owner.home": ROUTES.owner.home,
    "owner.store": ROUTES.owner.store,
    "owner.staff": ROUTES.owner.staff,
    "owner.permissions": ROUTES.owner.permissions,
    "owner.categories": ROUTES.owner.categories,
    "owner.products": ROUTES.owner.products,
    "owner.inventory": ROUTES.owner.inventory,
    "owner.orders": ROUTES.owner.orders,
    "owner.notifications": ROUTES.owner.notifications
  };

  function getRoute(routeKey, query = {}) {
    const loginArea = new URLSearchParams(location.search).get("area");
    const route = loginArea === "owner" && routeKey === "staff.forgot"
      ? ROUTES.owner.forgot
      : loginArea === "owner" && routeKey === "staff.reset"
        ? ROUTES.owner.reset
        : routeKeys[routeKey];
    if (!route) return null;
    const url = new URL(route, location.origin);
    const values = { ...query };
    if (routeKey === "customer.product" && values.slug) {
      url.pathname = `${ROUTES.customer.product}/${encodeURIComponent(values.slug)}`;
      delete values.slug;
      delete values.product;
    } else if (routeKey === "customer.category" && values.slug) {
      url.pathname = `${ROUTES.customer.category}/${encodeURIComponent(values.slug)}`;
      delete values.slug;
    } else if (routeKey === "customer.orders" && values.orderId) {
      url.pathname = `${ROUTES.customer.orders}/${encodeURIComponent(values.orderId)}`;
      delete values.orderId;
    }
    Object.entries(values).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    });
    return `${url.pathname}${url.search}${url.hash}`;
  }

  function validateReturnUrl(value) {
    if (!value) return null;
    try {
      const url = new URL(value, location.origin);
      const isCustomerRoute = url.pathname === ROUTES.customer.home
        || url.pathname === ROUTES.customer.product
        || url.pathname.startsWith(`${ROUTES.customer.product}/`)
        || url.pathname === ROUTES.customer.category
        || url.pathname.startsWith(`${ROUTES.customer.category}/`)
        || /^\/don-hang\/\d+$/.test(url.pathname)
        || [ROUTES.customer.cart, ROUTES.customer.checkout, ROUTES.customer.orders,
          ROUTES.customer.notifications, ROUTES.customer.addresses, ROUTES.customer.account]
          .includes(url.pathname);
      if (url.origin !== location.origin || !isCustomerRoute) return null;
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

  function canonicalLegacyRoute(url) {
    const legacy = {
      "/pages/auth/index.html": ROUTES.public.gateway,
      "/pages/auth/403.html": ROUTES.public.forbidden,
      "/error/404.html": ROUTES.public.notFound,
      "/pages/customer/auth/login.html": ROUTES.customer.login,
      "/pages/customer/auth/register.html": ROUTES.customer.register,
      "/pages/customer/auth/forgot-password.html": ROUTES.customer.forgot,
      "/pages/customer/auth/reset-password.html": ROUTES.customer.reset,
      "/pages/customer/auth/logout.html": ROUTES.customer.logout,
      "/pages/customer/index.html": ROUTES.customer.home,
      "/pages/customer/product.html": ROUTES.customer.product,
      "/pages/customer/cart.html": ROUTES.customer.cart,
      "/pages/customer/orders.html": ROUTES.customer.orders,
      "/pages/customer/notifications.html": ROUTES.customer.notifications,
      "/pages/customer/addresses.html": ROUTES.customer.addresses,
      "/pages/auth/session.html": ROUTES.customer.account,
      "/pages/staff/auth/login.html": "/quan-ly/dang-nhap",
      "/pages/staff/auth/forgot-password.html": "/quan-ly/quen-mat-khau",
      "/pages/staff/auth/reset-password.html": "/quan-ly/dat-lai-mat-khau",
      "/pages/staff/auth/logout.html": "/quan-ly/dang-xuat",
      "/pages/owner/dashboard.html": ROUTES.owner.home,
      "/pages/owner/store.html": ROUTES.owner.store,
      "/pages/owner/staff.html": ROUTES.owner.staff,
      "/pages/owner/permissions.html": ROUTES.owner.permissions,
      "/pages/owner/categories.html": ROUTES.owner.categories,
      "/pages/owner/products.html": ROUTES.owner.products,
      "/pages/owner/inventory.html": ROUTES.owner.inventory,
      "/pages/owner/orders.html": ROUTES.owner.orders,
      "/pages/owner/notifications.html": ROUTES.owner.notifications,
      "/pages/staff/dashboard.html": ROUTES.staff.home,
      "/pages/staff/products.html": ROUTES.staff.products,
      "/pages/staff/inventory.html": ROUTES.staff.inventory,
      "/pages/staff/orders.html": ROUTES.staff.orders,
      "/pages/staff/notifications.html": ROUTES.staff.notifications
    };
    const route = legacy[url.pathname];
    if (!route) return null;
    const target = new URL(route, location.origin);
    const requestedArea = url.searchParams.get("area");
    if (url.pathname === "/pages/staff/auth/login.html") {
      target.searchParams.set("area", requestedArea === "owner" ? "owner" : "staff");
    } else if (url.pathname === "/pages/staff/auth/logout.html"
        || url.pathname === "/pages/staff/auth/forgot-password.html"
        || url.pathname === "/pages/staff/auth/reset-password.html") {
      target.searchParams.set("area", ["owner", "staff"].includes(requestedArea) ? requestedArea : "staff");
    }
    url.searchParams.forEach((value, key) => {
      if (!target.searchParams.has(key)) target.searchParams.set(key, value);
    });
    return `${target.pathname}${target.search}${url.hash}`;
  }

  function canonicalizeLegacyLinks() {
    document.querySelectorAll("a[href]").forEach(link => {
      if (link.hasAttribute("data-route")) return;
      const url = new URL(link.href, location.href);
      if (url.origin !== location.origin) return;
      const route = canonicalLegacyRoute(url);
      if (route) link.href = route;
    });
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
        logoutLink.href = getRoute(`${areaForRole(role)}.logout`);
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
  canonicalizeLegacyLinks();

  const loginArea = new URLSearchParams(location.search).get("area");
  if (location.pathname === "/quan-ly/dang-nhap" || location.pathname === "/pages/staff/auth/login.html") {
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
    canonicalLegacyRoute,
    validateReturnUrl
  });

  setupForbiddenPage();
})();

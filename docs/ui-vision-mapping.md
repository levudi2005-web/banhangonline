# UI Vision Book — route and component mapping

This mapping ties the 36 PDF labels to the existing frontend pages and shared
feature code. The current requested retail design direction uses primary red
(`#CB1C22`), orange (`#F36F21`), a light-gray canvas, white rounded cards, fine
borders, restrained shadows, and visible focus states. Existing APIs and
feature modules continue to own behavior. Shared styling is centralized in
`frontend/assets/css/design-system.css`, with auth screens using `style.css`
and `auth.css`, customer and management screens using `dashboard.css`, and
owner/staff workflows rendered by `management-app.js`. The page paths in the
table identify internal static targets; public navigation uses the clean URLs
listed below.

**Status**

- **Refactored** — route and behavior retained; visual conformance is assessed
  separately in the runtime audit below.
- **Implemented** — required screen/state or interaction is present and mapped
  to its active frontend surface.
- **Missing** — no implementation found.
- **Blocked** — implementation exists, but an external dependency prevents
  end-to-end verification.

| UI | Screen / state | Route | Page / component | Status |
| --- | --- | --- | --- | --- |
| UI-001 | Public role gateway | `/pages/auth/index.html` | `pages/auth/index.html`; `style.css` | Refactored |
| UI-002 | Access denied | `/pages/auth/403.html` | `pages/auth/403.html`; `core/routes.js` | Refactored |
| UI-003 | Not found | `/error/404.html` | `error/404.html`; `core/routes.js` | Refactored |
| UI-004 | Customer session / profile | `/pages/auth/session.html` | `pages/auth/session.html`; `core/session.js`; `style.css`, `dashboard.css` | Refactored |
| UI-005 | Customer login | `/pages/customer/auth/login.html` | `pages/customer/auth/login.html`; `core/app.js`; `style.css`, `auth.css` | Refactored |
| UI-006 | Customer registration | `/pages/customer/auth/register.html` | `pages/customer/auth/register.html`; `core/app.js`; `style.css`, `auth.css` | Refactored |
| UI-007 | Customer password recovery | `/pages/customer/auth/forgot-password.html` | `pages/customer/auth/forgot-password.html`; `core/app.js`; `auth.css` | Refactored |
| UI-008 | Customer password reset | `/pages/customer/auth/reset-password.html` | `pages/customer/auth/reset-password.html`; `core/app.js`; `auth.css` | Refactored |
| UI-009 | Customer logout | `/pages/customer/auth/logout.html` | `pages/customer/auth/logout.html`; `core/session.js`; `style.css` | Refactored |
| UI-010 | Storefront / product catalog | `/pages/customer/index.html` | `pages/customer/index.html`; `features/customer-home.js`; `dashboard.css` | Refactored |
| UI-011 | Product details | `/pages/customer/product.html` | `pages/customer/product.html`; `features/customer-product.js`; `dashboard.css` | Refactored |
| UI-012 | Cart and pickup checkout | `/pages/customer/cart.html` | `pages/customer/cart.html`; `features/cart.js`; `dashboard.css` | Refactored |
| UI-013 | Customer orders | `/pages/customer/orders.html` | `pages/customer/orders.html`; `features/customer-orders.js`; `dashboard.css` | Refactored |
| UI-014 | Customer notifications | `/pages/customer/notifications.html` | `pages/customer/notifications.html`; `features/notifications.js`; `dashboard.css` | Refactored |
| UI-015 | Customer addresses | `/pages/customer/addresses.html` | `pages/customer/addresses.html`; `features/addresses.js`; `dashboard.css` | Refactored |
| UI-016 | Owner dashboard | `/pages/owner/dashboard.html` | `pages/owner/dashboard.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-017 | Store management | `/pages/owner/store.html` | `pages/owner/store.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-018 | Staff management | `/pages/owner/staff.html` | `pages/owner/staff.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-019 | Permission management | `/pages/owner/permissions.html` | `pages/owner/permissions.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-020 | Product categories | `/pages/owner/categories.html` | `pages/owner/categories.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-021 | Owner product management | `/pages/owner/products.html` | `pages/owner/products.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-022 | Owner inventory | `/pages/owner/inventory.html` | `pages/owner/inventory.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-023 | Owner orders | `/pages/owner/orders.html` | `pages/owner/orders.html`; `features/management-app.js`, `features/store-orders.js`; `dashboard.css` | Refactored |
| UI-024 | Owner notifications | `/pages/owner/notifications.html` | `pages/owner/notifications.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-025 | Staff / owner login | `/pages/staff/auth/login.html?area=staff` (staff) / `?area=owner` (owner) | `pages/staff/auth/login.html`; `core/routes.js`, `core/app.js`; `style.css`, `auth.css` | Refactored |
| UI-026 | Staff / owner password recovery | `/pages/staff/auth/forgot-password.html` | `pages/staff/auth/forgot-password.html`; `core/app.js`; `auth.css` | Refactored |
| UI-027 | Staff / owner password reset | `/pages/staff/auth/reset-password.html` | `pages/staff/auth/reset-password.html`; `core/app.js`; `auth.css` | Refactored |
| UI-028 | Staff / owner logout | `/pages/staff/auth/logout.html` | `pages/staff/auth/logout.html`; `core/session.js`; `style.css` | Refactored |
| UI-029 | Staff dashboard | `/pages/staff/dashboard.html` | `pages/staff/dashboard.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-030 | Staff products | `/pages/staff/products.html` | `pages/staff/products.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-031 | Staff inventory | `/pages/staff/inventory.html` | `pages/staff/inventory.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-032 | Staff orders | `/pages/staff/orders.html` | `pages/staff/orders.html`; `features/management-app.js`, `features/store-orders.js`; `dashboard.css` | Refactored |
| UI-033 | Staff notifications | `/pages/staff/notifications.html` | `pages/staff/notifications.html`; `features/management-app.js`; `dashboard.css` | Refactored |
| UI-034 | Product create / edit dialog | `/pages/owner/products.html` and `/pages/staff/products.html` | `features/management-app.js`; shared product dialog in `dashboard.css` | Implemented / Refactored |
| UI-035 | Product image empty, preview, upload, replace and error states | Same product management routes | `features/management-app.js`; S3-compatible image API; `dashboard.css` | Implemented / Refactored; multi-image gallery is incomplete |
| UI-036 | Inventory adjustment dialog | `/pages/owner/inventory.html` and `/pages/staff/inventory.html` | `features/management-app.js`; shared inventory dialog in `dashboard.css` | Implemented / Refactored |

## Canonical public URLs

The Spring MVC clean-URL controller forwards GET page routes to the existing
static pages. It does not match or rewrite `/api/**`. Product and category links
use their slugs; order-detail URLs select and highlight the requested order in
the existing order list.

| Public URL | Screen |
| --- | --- |
| `/dang-nhap`, `/dang-ky`, `/quen-mat-khau`, `/dat-lai-mat-khau`, `/dang-xuat` | Customer authentication |
| `/cua-hang`, `/san-pham`, `/san-pham/{slug}` | Store catalog and product detail |
| `/danh-muc/{slug}`, `/tim-kiem` | Catalog filtering and search |
| `/gio-hang`, `/thanh-toan` | Cart and pickup checkout |
| `/don-hang`, `/don-hang/{id}` | Customer orders |
| `/thong-bao`, `/tai-khoan`, `/tai-khoan/dia-chi` | Customer notifications and account |
| `/quan-ly/dang-nhap`, `/quan-ly/quen-mat-khau`, `/quan-ly/dat-lai-mat-khau`, `/quan-ly/dang-xuat` | Owner and staff authentication |
| `/quan-ly/owner/*`, `/quan-ly/staff/*` | Owner and staff management pages |

## Audit summary

- **Implemented:** all 36 labels have an active page, clean public URL, shared
  component, or dialog/state mapping. Product-image validation, preview,
  replacement, removal, busy/error/empty states, and inventory adjustment are
  represented.
- **Refactored to match the PDF:** tall centered 403/404 cards, compact
  authentication forms, the logout confirmation card and neutral cancel action,
  dashboard recent-order activity, blue brand accents, product/inventory
  layouts, and responsive dialog behavior.
- **Blocked:** persistent image upload requires production S3-compatible
  storage configuration. With storage disabled, the API correctly returns
  `IMAGE_STORAGE_UNAVAILABLE` (HTTP 503); the UI reports that the image was not
  uploaded and does not write files to local or Render ephemeral storage.
- Dynamic fixture values remain different from the illustrative PDF data.
- `database/sql/12-chat/` was not modified.

## Runtime visual audit

- Rendered UI-001–UI-036 at 1280 × 900 in Chromium against the HTTPS preview,
  then compared the four rendered contact sheets and individual screens with
  PDF pages 4–39. UI-025 was rendered for staff and owner login variants.
  Rendered screenshots and PDF references are retained in the session audit
  artifacts, not in the repository.
- The complete audit used an isolated E2E MariaDB on loopback port 3307,
  `E2eFixtureSeederTest`, the Spring E2E profile on loopback port 8081, and a
  local frontend gateway on port 4173. Only the gateway was exposed through the
  HTTPS tunnel; neither database nor backend was tunneled. The fixture seeder
  passed, and production database settings were not used.
- Browser storage states for CUSTOMER, OWNER, and STAFF were created from the
  E2E fixtures. The customer cart and saved addresses were populated with
  synthetic fixture data; a pickup order was placed in `PENDING`, producing a
  customer notification. Screens were then loaded from the HTTPS preview with
  their role-specific sessions. Cloudflare blocked browser POST requests, so
  fixture writes were made against the local gateway; HTTPS page rendering and
  authenticated GET data were verified.
- **UI-001–UI-003:** the welcome, forbidden, and not-found pages use the PDF's
  blue/pale blue-gray palette; the 403/404 cards are centered, sized to retain
  the reference's generous vertical whitespace, and keep their action buttons
  centered.
- **UI-004–UI-009:** the customer profile and auth screens render. Login,
  registration, recovery/reset forms use compact PDF-aligned sizing; logout is
  a centered confirmation card with clear cancel/submit actions.
- **UI-010–UI-015:** the store catalog, product detail, cart, orders,
  notifications, and address-management screens render with authenticated
  fixture data. Product/store content and counts differ from the reference data.
  The audit order is `PENDING`; the PDF order screen depicts more lifecycle
  examples. The customer has one generated notification and two synthetic
  addresses.
- **UI-016–UI-024:** owner dashboard, store, staff, permissions, category,
  product, inventory, order, and notification screens render with an OWNER
  session and selected store. Dashboard metrics, SVG shortcut icons, and a
  recent-order activity panel use data from the existing order API. Owner
  notifications and product/inventory/order counts vary with the fixture.
- **UI-025–UI-028:** staff/owner login, password recovery/reset, and logout
  states render. Existing routes and controls are preserved. The logout notice
  is informational rather than error-colored, and the cancel action is styled
  as the PDF's neutral secondary button.
- **UI-029–UI-033:** staff dashboard, products, inventory, orders, and
  notifications render with a STAFF session. The activity panel uses only
  orders the current staff permissions allow them to view; it displays a
  permission notice otherwise. Fixture data differs from the PDF examples.
- **UI-034–UI-036:** the authenticated product editor, selected-image preview,
  and inventory adjustment dialog render. UI-035 supports local preview,
  replace/remove, validation and visible loading/error states. Persistent
  upload remains blocked until storage is configured; HTTP 503
  `IMAGE_STORAGE_UNAVAILABLE` is shown explicitly rather than treated as
  success.
- Final-pass rerenders captured the 403, 404, customer login, and customer/staff
  logout screens at 1280 × 900 and 390 × 844 using a loopback-only static
  renderer. Each page had no horizontal overflow at either viewport, and the
  rendered accent token was `#426fbb`. These static checks do not exercise
  authenticated API-backed management screens.
- The authenticated E2E services and session files used for the earlier
  36-screen audit are no longer running. The recent-activity code, product
  editor, inventory dialog, and image-upload error handling therefore were not
  rerendered after the final CSS/JS edits; their earlier authenticated audit
  captures remain the available evidence. Persistent upload remains blocked
  without configured S3-compatible storage.
- A later design pass replaced the shared blue accent with the requested red
  and orange tokens and removed the layered page-background gradients. The
  storefront was checked in the integrated browser at 360, 375, 390, 768,
  1024, and 1440 CSS pixels; authenticated management pages were not rerun in
  that pass.
- UI-035 currently manages one product image through `products.image_url`.
  An eight-image gallery with persistent metadata, ordering, and main-image
  selection is not implemented. The current database schema has no
  `product_images` table, and schema deployment remains a reviewed manual
  operation.
- Visual styling and UI rendering changes are in frontend CSS/JS/markup. Clean
  page URLs are forwarded by a GET-only Spring MVC controller and covered by
  `CleanUrlControllerTest`; `/api/**` mappings are not changed. Authentication,
  authorization, roles, permissions, API contracts, order lifecycle, and
  `database/sql/12-chat/` were not changed. No commit, push, or deployment was
  performed.

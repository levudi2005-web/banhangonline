(() => {
  "use strict";

  const NOMINATIM = "https://nominatim.openstreetmap.org";
  const DEFAULT_CENTER = [16, 107.5];
  let requestStartedAt = 0;
  let requestQueue = Promise.resolve();
  let pickerSequence = 0;

  function makeElement(tag, text, className) {
    const node = document.createElement(tag);
    if (text) node.textContent = text;
    if (className) node.className = className;
    return node;
  }

  function geocode(url) {
    const request = requestQueue.then(async () => {
      const delay = Math.max(0, 1000 - (Date.now() - requestStartedAt));
      if (delay) await new Promise(resolve => setTimeout(resolve, delay));
      requestStartedAt = Date.now();
      const response = await fetch(url, {
        headers: { "Accept-Language": "vi,en" },
        referrerPolicy: "strict-origin-when-cross-origin"
      });
      const result = await response.json().catch(() => null);
      if (!response.ok || !result) {
        throw new Error(response.status === 429
          ? "Dịch vụ tìm địa chỉ đang giới hạn yêu cầu. Vui lòng đợi một chút rồi thử lại."
          : "Không thể tìm vị trí trên bản đồ. Vui lòng thử lại.");
      }
      return result;
    });
    requestQueue = request.then(() => undefined, () => undefined);
    return request;
  }

  function fillAddress(fields, address) {
    if (!fields || !address) return;
    const first = (...values) => values.find(value => typeof value === "string" && value.trim()) || "";
    if (fields.province) fields.province.value = first(address.state, address.region);
    if (fields.district) {
      fields.district.value = first(address.city_district, address.district, address.county, address.city);
    }
    if (fields.ward) {
      fields.ward.value = first(address.suburb, address.quarter, address.neighbourhood,
        address.ward, address.village, address.town);
    }
    if (fields.addressLine) {
      const street = [address.house_number, address.road].filter(Boolean).join(" ");
      if (street) fields.addressLine.value = street;
    }
  }

  function mount(container, options) {
    if (!container || !options || !window.L) {
      if (container) {
        container.textContent = "Bản đồ không tải được. Vui lòng làm mới trang để thử lại.";
        container.classList.add("location-picker-error");
      }
      return { setPosition() {} };
    }

    const id = `location-picker-map-${++pickerSequence}`;
    const latitude = options.latitudeInput;
    const longitude = options.longitudeInput;
    const search = makeElement("input");
    search.type = "search";
    search.className = "location-picker-search";
    search.placeholder = "Nhập địa chỉ cần tìm";
    search.setAttribute("aria-label", "Tìm địa chỉ trên bản đồ");
    const searchButton = makeElement("button", "Tìm địa chỉ", "dash-button secondary");
    searchButton.type = "button";
    const currentButton = makeElement("button", "Dùng vị trí hiện tại", "dash-button secondary");
    currentButton.type = "button";
    const actions = makeElement("div", undefined, "location-picker-actions");
    actions.append(search, searchButton, currentButton);
    const mapElement = makeElement("div", undefined, "location-picker-map");
    mapElement.id = id;
    mapElement.setAttribute("role", "application");
    mapElement.setAttribute("aria-label", "Bản đồ OpenStreetMap, bấm để chọn vị trí");
    const status = makeElement("p", "Bấm vào bản đồ để chọn vị trí.", "location-picker-status");
    status.setAttribute("role", "status");
    status.setAttribute("aria-live", "polite");
    const attribution = makeElement("small", "Bản đồ © OpenStreetMap contributors. Tìm kiếm địa chỉ do OpenStreetMap xử lý khi bạn yêu cầu.");
    attribution.className = "location-picker-attribution";
    container.replaceChildren(actions, mapElement, status, attribution);

    const initialLat = latitude && latitude.value.trim() !== "" ? Number(latitude.value) : NaN;
    const initialLon = longitude && longitude.value.trim() !== "" ? Number(longitude.value) : NaN;
    const hasInitialPosition = Number.isFinite(initialLat) && Number.isFinite(initialLon);
    const map = window.L.map(mapElement, { scrollWheelZoom: false })
      .setView(hasInitialPosition ? [initialLat, initialLon] : DEFAULT_CENTER, hasInitialPosition ? 16 : 5);
    window.L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; <a href=\"https://www.openstreetmap.org/copyright\" target=\"_blank\" rel=\"noopener noreferrer\">OpenStreetMap contributors</a>"
    }).addTo(map);
    let marker = null;

    function setBusy(value) {
      searchButton.disabled = value;
      currentButton.disabled = value;
    }

    function clearCoordinates(message) {
      latitude.value = "";
      longitude.value = "";
      if (marker) {
        marker.remove();
        marker = null;
      }
      if (message) status.textContent = message;
    }

    function drawPosition(lat, lon) {
      const point = [lat, lon];
      map.setView(point, Math.max(map.getZoom(), 15));
      if (!marker) {
        marker = window.L.marker(point, { draggable: true }).addTo(map);
        marker.on("dragend", () => {
          const position = marker.getLatLng();
          selectPosition(position.lat, position.lng, true);
        });
      } else {
        marker.setLatLng(point);
      }
    }

    async function selectPosition(lat, lon, reverseLookup) {
      if (!Number.isFinite(lat) || !Number.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
        throw new Error("Tọa độ vị trí không hợp lệ.");
      }
      latitude.value = lat.toFixed(7);
      longitude.value = lon.toFixed(7);
      drawPosition(lat, lon);
      status.textContent = `Đã chọn vị trí ${lat.toFixed(5)}, ${lon.toFixed(5)}.`;
      if (!reverseLookup) return;
      setBusy(true);
      try {
        const url = new URL(`${NOMINATIM}/reverse`);
        url.search = new URLSearchParams({ lat: String(lat), lon: String(lon), format: "jsonv2" });
        const result = await geocode(url);
        fillAddress(options.addressFields, result.address);
        if (result.display_name) status.textContent = `Đã chọn: ${result.display_name}`;
      } catch (error) {
        status.textContent = `${error.message} Tọa độ đã được lưu; bạn có thể nhập địa chỉ thủ công.`;
      } finally {
        setBusy(false);
      }
    }

    function queryFromAddress() {
      const fields = options.addressFields || {};
      return [fields.addressLine, fields.ward, fields.district, fields.province]
        .map(field => field && field.value.trim()).filter(Boolean).join(", ");
    }

    async function searchAddress() {
      const query = search.value.trim() || queryFromAddress();
      if (!query) {
        status.textContent = "Nhập địa chỉ hoặc điền các trường địa chỉ trước khi tìm.";
        search.focus();
        return;
      }
      setBusy(true);
      status.textContent = "Đang tìm vị trí…";
      try {
        const url = new URL(`${NOMINATIM}/search`);
        url.search = new URLSearchParams({ q: query, format: "jsonv2", limit: "1", countrycodes: "vn" });
        const result = await geocode(url);
        if (!Array.isArray(result) || !result.length) {
          status.textContent = "Không tìm thấy địa chỉ. Thử thêm phường, quận hoặc thành phố.";
          return;
        }
        await selectPosition(Number(result[0].lat), Number(result[0].lon), true);
      } catch (error) {
        status.textContent = error.message;
      } finally {
        setBusy(false);
      }
    }

    searchButton.addEventListener("click", searchAddress);
    search.addEventListener("keydown", event => {
      if (event.key === "Enter") {
        event.preventDefault();
        searchAddress();
      }
    });
    currentButton.addEventListener("click", () => {
      if (!navigator.geolocation) {
        status.textContent = "Trình duyệt này không hỗ trợ xác định vị trí.";
        return;
      }
      currentButton.disabled = true;
      status.textContent = "Đang xác định vị trí hiện tại…";
      navigator.geolocation.getCurrentPosition(
        position => {
          selectPosition(position.coords.latitude, position.coords.longitude, true)
            .catch(error => { status.textContent = error.message; })
            .finally(() => { currentButton.disabled = false; });
        },
        error => {
          status.textContent = error.code === error.PERMISSION_DENIED
            ? "Bạn chưa cho phép truy cập vị trí. Bạn vẫn có thể tìm địa chỉ hoặc chọn trực tiếp trên bản đồ."
            : "Không xác định được vị trí hiện tại. Vui lòng thử lại hoặc chọn trên bản đồ.";
          currentButton.disabled = false;
        },
        { enableHighAccuracy: false, maximumAge: 300000, timeout: 10000 }
      );
    });
    map.on("click", event => {
      selectPosition(event.latlng.lat, event.latlng.lng, true)
        .catch(error => { status.textContent = error.message; });
    });

    Object.values(options.addressFields || {}).forEach(field => {
      field.addEventListener("input", () => {
        if (latitude.value || longitude.value) {
          clearCoordinates("Địa chỉ đã thay đổi. Hãy chọn lại vị trí để tránh sai lệch.");
        }
      });
    });

    const picker = {
      setPosition(lat, lon) {
        const parsedLat = Number(lat);
        const parsedLon = Number(lon);
        if (lat === null || lat === undefined || lat === "" || lon === null || lon === undefined || lon === ""
            || !Number.isFinite(parsedLat) || !Number.isFinite(parsedLon)) {
          clearCoordinates("Bấm vào bản đồ để chọn vị trí.");
          return;
        }
        latitude.value = parsedLat.toFixed(7);
        longitude.value = parsedLon.toFixed(7);
        drawPosition(parsedLat, parsedLon);
        status.textContent = `Vị trí đã lưu: ${parsedLat.toFixed(5)}, ${parsedLon.toFixed(5)}.`;
      }
    };
    if (hasInitialPosition) picker.setPosition(initialLat, initialLon);
    window.setTimeout(() => map.invalidateSize(), 0);
    return picker;
  }

  window.LocationPicker = Object.freeze({ mount });
})();

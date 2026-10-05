-- Idempotent starter catalog for BanHangOnline.
-- Inserts only missing categories/products and inventory rows for ACTIVE stores.
-- Safe to rerun after a store is approved; existing records are never updated/deleted.
-- Product images are NULL intentionally; the storefront renders a category fallback.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

INSERT INTO categories (name, slug, description, status)
SELECT seed.name, seed.slug, seed.description, 'ACTIVE'
FROM (
    SELECT 'Điện thoại' AS name, 'dien-thoai' AS slug,
           'Điện thoại thông minh chính hãng, phục vụ nhu cầu liên lạc, làm việc và giải trí.' AS description
    UNION ALL SELECT 'Laptop', 'laptop',
           'Máy tính xách tay cho học tập, văn phòng, sáng tạo nội dung và công việc chuyên nghiệp.'
    UNION ALL SELECT 'Máy tính bảng', 'may-tinh-bang',
           'Máy tính bảng đa năng cho học tập, giải trí, ghi chú và làm việc di động.'
    UNION ALL SELECT 'Tai nghe & âm thanh', 'tai-nghe-am-thanh',
           'Tai nghe, loa di động và thiết bị âm thanh cho nhu cầu nghe nhạc, họp và giải trí.'
    UNION ALL SELECT 'Phụ kiện điện thoại', 'phu-kien-dien-thoai',
           'Ốp lưng, kính bảo vệ và phụ kiện thiết yếu dành cho điện thoại thông minh.'
    UNION ALL SELECT 'Phụ kiện máy tính', 'phu-kien-may-tinh',
           'Webcam, hub USB và phụ kiện hữu ích cho góc làm việc máy tính.'
    UNION ALL SELECT 'Màn hình', 'man-hinh',
           'Màn hình máy tính phục vụ văn phòng, thiết kế và giải trí tại nhà.'
    UNION ALL SELECT 'Bàn phím', 'ban-phim',
           'Bàn phím máy tính có dây và không dây cho làm việc và sử dụng hàng ngày.'
    UNION ALL SELECT 'Chuột máy tính', 'chuot-may-tinh',
           'Chuột máy tính có dây và không dây với nhiều lựa chọn cho văn phòng.'
    UNION ALL SELECT 'Thiết bị mạng', 'thiet-bi-mang',
           'Thiết bị mạng gia đình giúp kết nối ổn định cho học tập và làm việc.'
    UNION ALL SELECT 'Sạc & cáp', 'sac-cap',
           'Bộ sạc, cáp sạc và phụ kiện cấp nguồn tương thích với thiết bị phổ biến.'
    UNION ALL SELECT 'Thiết bị lưu trữ', 'thiet-bi-luu-tru',
           'Ổ cứng thể rắn và thiết bị lưu trữ di động cho máy tính cá nhân.'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM categories existing WHERE existing.slug = seed.slug
);

INSERT INTO products
    (category_id, sku, name, slug, description, price, currency, status, image_url)
SELECT c.id, seed.sku, seed.name, seed.slug, seed.description,
       seed.price, 'VND', 'ACTIVE', NULL
FROM (
    SELECT 'dien-thoai' AS category_slug, 'BHO-STARTER-PHONE-SAMSUNG-A56-256' AS sku,
           'Samsung Galaxy A56 5G 12GB/256GB' AS name, 'samsung-galaxy-a56-5g-256gb' AS slug,
           'Điện thoại 5G màn hình Super AMOLED 6,7 inch, camera đa dụng và bộ nhớ 256GB.' AS description,
           11990000 AS price, 12 AS stock_quantity, 3 AS reorder_level
    UNION ALL SELECT 'dien-thoai', 'BHO-STARTER-PHONE-SAMSUNG-S25FE-256',
           'Samsung Galaxy S25 FE 8GB/256GB', 'samsung-galaxy-s25-fe-256gb',
           'Điện thoại Galaxy FE màn hình AMOLED, camera chống rung và hiệu năng cao cấp.', 16990000, 8, 2
    UNION ALL SELECT 'dien-thoai', 'BHO-STARTER-PHONE-APPLE-IP16-128',
           'Apple iPhone 16 128GB', 'apple-iphone-16-128gb',
           'iPhone màn hình Super Retina XDR 6,1 inch, camera 48MP và cổng USB-C.', 19990000, 10, 2
    UNION ALL SELECT 'dien-thoai', 'BHO-STARTER-PHONE-APPLE-IP16PRO-256',
           'Apple iPhone 16 Pro 256GB', 'apple-iphone-16-pro-256gb',
           'iPhone Pro khung titanium, màn hình ProMotion và hệ thống camera chuyên nghiệp.', 28990000, 6, 2
    UNION ALL SELECT 'dien-thoai', 'BHO-STARTER-PHONE-XIAOMI-14T-512',
           'Xiaomi 14T 12GB/512GB', 'xiaomi-14t-512gb',
           'Điện thoại 5G màn hình AMOLED 144Hz, camera Leica và bộ nhớ 512GB.', 13490000, 9, 2

    UNION ALL SELECT 'laptop', 'BHO-STARTER-LAPTOP-HP-PAVILION15-I5',
           'HP Pavilion 15 eg3093TU Core i5/16GB/512GB', 'hp-pavilion-15-eg3093tu',
           'Laptop 15,6 inch Full HD, Core i5, RAM 16GB và SSD 512GB cho công việc văn phòng.', 16990000, 6, 2
    UNION ALL SELECT 'laptop', 'BHO-STARTER-LAPTOP-DELL-INSPIRON14-I5',
           'Dell Inspiron 14 5440 Core 5/16GB/512GB', 'dell-inspiron-14-5440',
           'Laptop 14 inch gọn nhẹ, màn hình FHD+, RAM 16GB và SSD PCIe 512GB.', 18490000, 5, 2
    UNION ALL SELECT 'laptop', 'BHO-STARTER-LAPTOP-LENOVO-THINKPAD-E14',
           'Lenovo ThinkPad E14 Gen 6 Ryzen 5/16GB/512GB', 'lenovo-thinkpad-e14-gen-6',
           'Laptop doanh nghiệp 14 inch, bàn phím ThinkPad, RAM 16GB và SSD 512GB.', 19990000, 5, 2
    UNION ALL SELECT 'laptop', 'BHO-STARTER-LAPTOP-ASUS-VIVOBOOK-S14',
           'ASUS Vivobook S 14 OLED Core Ultra 5/16GB/512GB', 'asus-vivobook-s14-oled-ultra-5',
           'Laptop OLED 14 inch sắc nét, Core Ultra 5, RAM 16GB và SSD 512GB.', 22990000, 4, 1
    UNION ALL SELECT 'laptop', 'BHO-STARTER-LAPTOP-ACER-ASPIRE5-R5',
           'Acer Aspire 5 A515 Ryzen 5/16GB/512GB', 'acer-aspire-5-a515-ryzen-5',
           'Laptop 15,6 inch đa dụng với Ryzen 5, RAM 16GB và SSD 512GB.', 14490000, 7, 2

    UNION ALL SELECT 'may-tinh-bang', 'BHO-STARTER-TABLET-SAMSUNG-TABS9FE',
           'Samsung Galaxy Tab S9 FE Wi-Fi 128GB', 'samsung-galaxy-tab-s9-fe-128gb',
           'Máy tính bảng 10,9 inch, hỗ trợ S Pen và chống nước bụi IP68.', 9990000, 7, 2
    UNION ALL SELECT 'may-tinh-bang', 'BHO-STARTER-TABLET-APPLE-IPAD11-128',
           'Apple iPad 11 inch Wi-Fi 128GB', 'apple-ipad-11-wifi-128gb',
           'iPad 11 inch Liquid Retina, phù hợp học tập, ghi chú và giải trí.', 9990000, 8, 2
    UNION ALL SELECT 'may-tinh-bang', 'BHO-STARTER-TABLET-XIAOMI-PAD7-256',
           'Xiaomi Pad 7 8GB/256GB', 'xiaomi-pad-7-256gb',
           'Máy tính bảng màn hình 3.2K 144Hz, loa Dolby Atmos và bộ nhớ 256GB.', 9990000, 6, 2
    UNION ALL SELECT 'may-tinh-bang', 'BHO-STARTER-TABLET-LENOVO-TABPLUS',
           'Lenovo Tab Plus 8GB/256GB', 'lenovo-tab-plus-256gb',
           'Máy tính bảng giải trí màn hình 11,5 inch và hệ thống loa JBL.', 7490000, 8, 2
    UNION ALL SELECT 'may-tinh-bang', 'BHO-STARTER-TABLET-SAMSUNG-TABA9PLUS',
           'Samsung Galaxy Tab A9+ Wi-Fi 128GB', 'samsung-galaxy-tab-a9-plus-128gb',
           'Máy tính bảng 11 inch, hỗ trợ chia màn hình và giải trí gia đình.', 6490000, 10, 3

    UNION ALL SELECT 'tai-nghe-am-thanh', 'BHO-STARTER-AUDIO-SONY-WHCH720N',
           'Sony WH-CH720N chống ồn không dây', 'sony-wh-ch720n',
           'Tai nghe chụp tai Bluetooth chống ồn chủ động, thời lượng nghe dài.', 2490000, 18, 5
    UNION ALL SELECT 'tai-nghe-am-thanh', 'BHO-STARTER-AUDIO-JBL-TUNE770NC',
           'JBL Tune 770NC Bluetooth chống ồn', 'jbl-tune-770nc',
           'Tai nghe không dây chống ồn với âm thanh JBL Pure Bass và thiết kế gập.', 2690000, 15, 4
    UNION ALL SELECT 'tai-nghe-am-thanh', 'BHO-STARTER-AUDIO-SAMSUNG-BUDS3FE',
           'Samsung Galaxy Buds3 FE', 'samsung-galaxy-buds3-fe',
           'Tai nghe true wireless chống ồn chủ động, kết nối thuận tiện với Galaxy.', 2990000, 20, 5
    UNION ALL SELECT 'tai-nghe-am-thanh', 'BHO-STARTER-AUDIO-ANKER-SOUNCOREQ20I',
           'Anker Soundcore Q20i chống ồn', 'anker-soundcore-q20i',
           'Tai nghe chụp tai Bluetooth chống ồn hybrid, hỗ trợ nghe qua cáp AUX.', 1190000, 22, 6
    UNION ALL SELECT 'tai-nghe-am-thanh', 'BHO-STARTER-AUDIO-XIAOMI-REDMIBUDS6',
           'Xiaomi Redmi Buds 6', 'xiaomi-redmi-buds-6',
           'Tai nghe true wireless gọn nhẹ, chống ồn và hộp sạc di động.', 1090000, 25, 6

    UNION ALL SELECT 'phu-kien-dien-thoai', 'BHO-STARTER-PHONEACC-SPIGEN-IP16',
           'Ốp lưng Spigen Rugged Armor iPhone 16', 'spigen-rugged-armor-iphone-16',
           'Ốp lưng chống sốc Air Cushion, bề mặt nhám hạn chế bám vân tay.', 490000, 35, 10
    UNION ALL SELECT 'phu-kien-dien-thoai', 'BHO-STARTER-PHONEACC-ESR-IP16PRO',
           'Ốp lưng ESR Classic Hybrid iPhone 16 Pro', 'esr-classic-hybrid-iphone-16-pro',
           'Ốp lưng trong suốt chống ố vàng, tương thích sạc không dây MagSafe.', 390000, 40, 10
    UNION ALL SELECT 'phu-kien-dien-thoai', 'BHO-STARTER-PHONEACC-ANKER-POWERBANK10K',
           'Pin sạc dự phòng Anker 10.000mAh 30W', 'anker-powerbank-10000mah-30w',
           'Pin dự phòng dung lượng 10.000mAh, sạc nhanh USB-C công suất 30W.', 1190000, 28, 8
    UNION ALL SELECT 'phu-kien-dien-thoai', 'BHO-STARTER-PHONEACC-BASEUS-USBC100W',
           'Cáp Baseus USB-C to USB-C 100W 1m', 'baseus-usbc-usbc-100w-1m',
           'Cáp USB-C bọc dù hỗ trợ sạc công suất đến 100W và truyền dữ liệu.', 190000, 60, 15
    UNION ALL SELECT 'phu-kien-dien-thoai', 'BHO-STARTER-PHONEACC-UNIQ-GLASS-IP16',
           'Kính cường lực UNIQ Optix iPhone 16', 'uniq-optix-glass-iphone-16',
           'Kính bảo vệ màn hình trong suốt, phủ hạn chế bám dấu vân tay.', 290000, 50, 12

    UNION ALL SELECT 'phu-kien-may-tinh', 'BHO-STARTER-COMPUTERACC-LOGITECH-C920',
           'Webcam Logitech C920 HD Pro', 'logitech-c920-hd-pro',
           'Webcam Full HD 1080p với micro kép, phù hợp họp trực tuyến và học tập.', 1690000, 12, 4
    UNION ALL SELECT 'phu-kien-may-tinh', 'BHO-STARTER-COMPUTERACC-ANKER-555HUB',
           'Hub Anker 555 USB-C 8-in-1', 'anker-555-usbc-hub-8in1',
           'Hub USB-C mở rộng HDMI 4K, Ethernet, USB-A, USB-C và đầu đọc thẻ.', 1990000, 10, 3
    UNION ALL SELECT 'phu-kien-may-tinh', 'BHO-STARTER-COMPUTERACC-UGREEN-REVODOK6',
           'Hub UGREEN Revodok 6-in-1 USB-C', 'ugreen-revodok-6in1',
           'Hub USB-C nhỏ gọn với HDMI 4K, USB-A, USB-C PD và đầu đọc thẻ.', 890000, 14, 4
    UNION ALL SELECT 'phu-kien-may-tinh', 'BHO-STARTER-COMPUTERACC-LOGITECH-H390',
           'Tai nghe máy tính Logitech H390 USB', 'logitech-h390-usb',
           'Tai nghe USB có micro chống ồn và nút điều khiển âm lượng trên dây.', 790000, 16, 5
    UNION ALL SELECT 'phu-kien-may-tinh', 'BHO-STARTER-COMPUTERACC-ORICO-M2PV',
           'Hộp ổ cứng ORICO M.2 NVMe USB-C', 'orico-m2-nvme-usbc-enclosure',
           'Vỏ chuyển ổ M.2 NVMe sang USB-C, hỗ trợ tản nhiệt cho ổ cứng di động.', 590000, 14, 4

    UNION ALL SELECT 'man-hinh', 'BHO-STARTER-MONITOR-DELL-P2425H',
           'Màn hình Dell Pro 24 P2425H IPS', 'dell-pro-24-p2425h',
           'Màn hình IPS 23,8 inch Full HD 100Hz, chân đế công thái học.', 4690000, 7, 2
    UNION ALL SELECT 'man-hinh', 'BHO-STARTER-MONITOR-LG-27MR400',
           'Màn hình LG 27MR400-B IPS 27 inch', 'lg-27mr400-b-27-inch',
           'Màn hình IPS 27 inch Full HD 100Hz, hỗ trợ AMD FreeSync.', 3290000, 8, 2
    UNION ALL SELECT 'man-hinh', 'BHO-STARTER-MONITOR-SAMSUNG-LS27C310',
           'Màn hình Samsung Essential S3 27 inch', 'samsung-essential-s3-27-inch',
           'Màn hình IPS 27 inch Full HD, viền mỏng và chế độ bảo vệ mắt.', 2890000, 9, 2
    UNION ALL SELECT 'man-hinh', 'BHO-STARTER-MONITOR-ASUS-VA24EHF',
           'Màn hình ASUS VA24EHF IPS 24 inch', 'asus-va24ehf-24-inch',
           'Màn hình IPS 23,8 inch Full HD 100Hz, hỗ trợ Adaptive-Sync.', 2690000, 10, 3
    UNION ALL SELECT 'man-hinh', 'BHO-STARTER-MONITOR-LG-32UN650',
           'Màn hình LG 32UN650-W 4K UHD', 'lg-32un650-w-4k',
           'Màn hình IPS 31,5 inch độ phân giải 4K UHD, hỗ trợ HDR10.', 8990000, 4, 1

    UNION ALL SELECT 'ban-phim', 'BHO-STARTER-KEYBOARD-LOGITECH-K380S',
           'Bàn phím Logitech Pebble Keys 2 K380s', 'logitech-pebble-keys-2-k380s',
           'Bàn phím Bluetooth gọn nhẹ, kết nối và chuyển đổi giữa ba thiết bị.', 790000, 18, 5
    UNION ALL SELECT 'ban-phim', 'BHO-STARTER-KEYBOARD-LOGITECH-K120',
           'Bàn phím Logitech K120 USB', 'logitech-k120-usb',
           'Bàn phím có dây bố cục đầy đủ, phím êm và thiết kế chống tràn.', 220000, 30, 8
    UNION ALL SELECT 'ban-phim', 'BHO-STARTER-KEYBOARD-KEYCHRON-K2V2',
           'Bàn phím cơ Keychron K2 Version 2', 'keychron-k2-version-2',
           'Bàn phím cơ không dây layout 75%, kết nối Bluetooth và USB-C.', 1890000, 10, 3
    UNION ALL SELECT 'ban-phim', 'BHO-STARTER-KEYBOARD-DAREU-EK87',
           'Bàn phím cơ DareU EK87 USB', 'dareu-ek87-usb',
           'Bàn phím cơ TKL 87 phím kết nối USB, phù hợp bàn làm việc gọn gàng.', 690000, 14, 4
    UNION ALL SELECT 'ban-phim', 'BHO-STARTER-KEYBOARD-AKKO-3087',
           'Bàn phím cơ AKKO 3087 Silent', 'akko-3087-silent',
           'Bàn phím cơ TKL 87 phím với switch yên tĩnh cho môi trường văn phòng.', 1390000, 10, 3

    UNION ALL SELECT 'chuot-may-tinh', 'BHO-STARTER-MOUSE-LOGITECH-M650',
           'Chuột không dây Logitech Signature M650', 'logitech-signature-m650',
           'Chuột không dây công thái học, cuộn SmartWheel và kết nối Bluetooth/USB.', 790000, 20, 6
    UNION ALL SELECT 'chuot-may-tinh', 'BHO-STARTER-MOUSE-LOGITECH-MXANYWHERE3S',
           'Chuột Logitech MX Anywhere 3S', 'logitech-mx-anywhere-3s',
           'Chuột không dây nhỏ gọn, cảm biến 8K DPI và cuộn MagSpeed.', 1990000, 10, 3
    UNION ALL SELECT 'chuot-may-tinh', 'BHO-STARTER-MOUSE-LOGITECH-B100',
           'Chuột Logitech B100 USB', 'logitech-b100-usb',
           'Chuột quang có dây ba nút, thiết kế đối xứng dùng hàng ngày.', 95000, 40, 10
    UNION ALL SELECT 'chuot-may-tinh', 'BHO-STARTER-MOUSE-MICROSOFT-BLUETOOTH',
           'Chuột Microsoft Bluetooth Mouse', 'microsoft-bluetooth-mouse',
           'Chuột Bluetooth gọn nhẹ, tương thích Windows và các thiết bị hỗ trợ Bluetooth.', 390000, 18, 5
    UNION ALL SELECT 'chuot-may-tinh', 'BHO-STARTER-MOUSE-RAZER-DEATHADDERV2X',
           'Chuột không dây Razer DeathAdder V2 X HyperSpeed', 'razer-deathadder-v2-x-hyperspeed',
           'Chuột không dây công thái học, cảm biến quang học và kết nối kép.', 1190000, 10, 3

    UNION ALL SELECT 'thiet-bi-mang', 'BHO-STARTER-NETWORK-TP-LINK-ARCHERC64',
           'Router Wi-Fi TP-Link Archer C64 AC1200', 'tp-link-archer-c64-ac1200',
           'Router Wi-Fi băng tần kép AC1200 với bốn ăng-ten và cổng Gigabit.', 690000, 12, 4
    UNION ALL SELECT 'thiet-bi-mang', 'BHO-STARTER-NETWORK-ASUS-RTAX57',
           'Router ASUS RT-AX57 Wi-Fi 6 AX3000', 'asus-rt-ax57-ax3000',
           'Router Wi-Fi 6 băng tần kép AX3000 với bảo mật AiProtection.', 1690000, 7, 2
    UNION ALL SELECT 'thiet-bi-mang', 'BHO-STARTER-NETWORK-TP-LINK-RE315',
           'Bộ kích sóng Wi-Fi TP-Link RE315 AC1200', 'tp-link-re315-ac1200',
           'Bộ mở rộng vùng phủ sóng Wi-Fi băng tần kép, cài đặt qua ứng dụng.', 490000, 14, 4
    UNION ALL SELECT 'thiet-bi-mang', 'BHO-STARTER-NETWORK-TP-LINK-SG105',
           'Switch TP-Link TL-SG105 5 cổng Gigabit', 'tp-link-tl-sg105-5-port',
           'Switch để bàn năm cổng Gigabit, vỏ kim loại và hoạt động không quạt.', 390000, 12, 4
    UNION ALL SELECT 'thiet-bi-mang', 'BHO-STARTER-NETWORK-TENDA-U12',
           'USB Wi-Fi adapter Tenda U12 AC1300', 'tenda-u12-ac1300-usb',
           'Adapter USB Wi-Fi băng tần kép AC1300 với ăng-ten rời.', 420000, 10, 3

    UNION ALL SELECT 'sac-cap', 'BHO-STARTER-CHARGER-ANKER-NANO45W',
           'Củ sạc Anker Nano 45W USB-C', 'anker-nano-45w-usbc',
           'Củ sạc GaN một cổng USB-C 45W, hỗ trợ sạc nhanh thiết bị di động.', 690000, 25, 7
    UNION ALL SELECT 'sac-cap', 'BHO-STARTER-CHARGER-UGREEN-NEXODE65W',
           'Củ sạc UGREEN Nexode 65W GaN', 'ugreen-nexode-65w-gan',
           'Bộ sạc GaN 65W nhiều cổng USB-C/USB-A cho điện thoại và laptop.', 990000, 20, 6
    UNION ALL SELECT 'sac-cap', 'BHO-STARTER-CHARGER-BASEUS-CCGAN30W',
           'Củ sạc Baseus GaN 30W USB-C', 'baseus-gan-30w-usbc',
           'Củ sạc nhỏ gọn 30W hỗ trợ USB Power Delivery cho điện thoại và máy tính bảng.', 390000, 28, 8
    UNION ALL SELECT 'sac-cap', 'BHO-STARTER-CABLE-APPLE-USBC-1M',
           'Cáp Apple USB-C Charge Cable 1m', 'apple-usbc-charge-cable-1m',
           'Cáp USB-C dài 1m dùng sạc và đồng bộ thiết bị có cổng USB-C.', 490000, 30, 8
    UNION ALL SELECT 'sac-cap', 'BHO-STARTER-CABLE-ANKER-POWERLINE3',
           'Cáp Anker PowerLine III USB-C to Lightning 0.9m', 'anker-powerline-iii-usbc-lightning',
           'Cáp USB-C to Lightning hỗ trợ sạc nhanh cho iPhone tương thích.', 390000, 24, 7

    UNION ALL SELECT 'thiet-bi-luu-tru', 'BHO-STARTER-STORAGE-SAMSUNG-T7-1TB',
           'SSD di động Samsung T7 1TB USB-C', 'samsung-t7-portable-ssd-1tb',
           'Ổ SSD di động 1TB tốc độ cao, vỏ nhôm nhỏ gọn và kết nối USB-C.', 2890000, 10, 3
    UNION ALL SELECT 'thiet-bi-luu-tru', 'BHO-STARTER-STORAGE-SANDISK-EXTREME1TB',
           'SSD di động SanDisk Extreme Portable 1TB', 'sandisk-extreme-portable-ssd-1tb',
           'SSD di động 1TB chống bụi nước IP65, phù hợp sao lưu khi di chuyển.', 3190000, 8, 2
    UNION ALL SELECT 'thiet-bi-luu-tru', 'BHO-STARTER-STORAGE-KINGSTON-NV2-1TB',
           'SSD Kingston NV2 M.2 NVMe 1TB', 'kingston-nv2-m2-nvme-1tb',
           'Ổ SSD NVMe M.2 2280 dung lượng 1TB cho nâng cấp laptop và máy tính bàn.', 1590000, 12, 3
    UNION ALL SELECT 'thiet-bi-luu-tru', 'BHO-STARTER-STORAGE-WD-SN770-1TB',
           'SSD WD_BLACK SN770 NVMe 1TB', 'wd-black-sn770-nvme-1tb',
           'SSD NVMe PCIe Gen4 1TB, phù hợp máy tính cá nhân và máy chơi game.', 1990000, 10, 3
    UNION ALL SELECT 'thiet-bi-luu-tru', 'BHO-STARTER-STORAGE-KINGSTON-DTX128',
           'USB Kingston DataTraveler Exodia 128GB', 'kingston-datatraveler-exodia-128gb',
           'USB flash 128GB với nắp bảo vệ và vòng móc khóa tiện mang theo.', 190000, 25, 7
) AS seed
JOIN categories c ON c.slug = seed.category_slug AND c.status = 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM products existing
    WHERE existing.sku = seed.sku OR existing.slug = seed.slug
);

INSERT INTO inventory (store_id, product_id, quantity, reserved_quantity, reorder_level, status)
SELECT s.id, p.id, seed.stock_quantity, 0, seed.reorder_level, 'ACTIVE'
FROM (
    SELECT 'BHO-STARTER-PHONE-SAMSUNG-A56-256' AS sku, 12 AS stock_quantity, 3 AS reorder_level
    UNION ALL SELECT 'BHO-STARTER-PHONE-SAMSUNG-S25FE-256', 8, 2
    UNION ALL SELECT 'BHO-STARTER-PHONE-APPLE-IP16-128', 10, 2
    UNION ALL SELECT 'BHO-STARTER-PHONE-APPLE-IP16PRO-256', 6, 2
    UNION ALL SELECT 'BHO-STARTER-PHONE-XIAOMI-14T-512', 9, 2
    UNION ALL SELECT 'BHO-STARTER-LAPTOP-HP-PAVILION15-I5', 6, 2
    UNION ALL SELECT 'BHO-STARTER-LAPTOP-DELL-INSPIRON14-I5', 5, 2
    UNION ALL SELECT 'BHO-STARTER-LAPTOP-LENOVO-THINKPAD-E14', 5, 2
    UNION ALL SELECT 'BHO-STARTER-LAPTOP-ASUS-VIVOBOOK-S14', 4, 1
    UNION ALL SELECT 'BHO-STARTER-LAPTOP-ACER-ASPIRE5-R5', 7, 2
    UNION ALL SELECT 'BHO-STARTER-TABLET-SAMSUNG-TABS9FE', 7, 2
    UNION ALL SELECT 'BHO-STARTER-TABLET-APPLE-IPAD11-128', 8, 2
    UNION ALL SELECT 'BHO-STARTER-TABLET-XIAOMI-PAD7-256', 6, 2
    UNION ALL SELECT 'BHO-STARTER-TABLET-LENOVO-TABPLUS', 8, 2
    UNION ALL SELECT 'BHO-STARTER-TABLET-SAMSUNG-TABA9PLUS', 10, 3
    UNION ALL SELECT 'BHO-STARTER-AUDIO-SONY-WHCH720N', 18, 5
    UNION ALL SELECT 'BHO-STARTER-AUDIO-JBL-TUNE770NC', 15, 4
    UNION ALL SELECT 'BHO-STARTER-AUDIO-SAMSUNG-BUDS3FE', 20, 5
    UNION ALL SELECT 'BHO-STARTER-AUDIO-ANKER-SOUNCOREQ20I', 22, 6
    UNION ALL SELECT 'BHO-STARTER-AUDIO-XIAOMI-REDMIBUDS6', 25, 6
    UNION ALL SELECT 'BHO-STARTER-PHONEACC-SPIGEN-IP16', 35, 10
    UNION ALL SELECT 'BHO-STARTER-PHONEACC-ESR-IP16PRO', 40, 10
    UNION ALL SELECT 'BHO-STARTER-PHONEACC-ANKER-POWERBANK10K', 28, 8
    UNION ALL SELECT 'BHO-STARTER-PHONEACC-BASEUS-USBC100W', 60, 15
    UNION ALL SELECT 'BHO-STARTER-PHONEACC-UNIQ-GLASS-IP16', 50, 12
    UNION ALL SELECT 'BHO-STARTER-COMPUTERACC-LOGITECH-C920', 12, 4
    UNION ALL SELECT 'BHO-STARTER-COMPUTERACC-ANKER-555HUB', 10, 3
    UNION ALL SELECT 'BHO-STARTER-COMPUTERACC-UGREEN-REVODOK6', 14, 4
    UNION ALL SELECT 'BHO-STARTER-COMPUTERACC-LOGITECH-H390', 16, 5
    UNION ALL SELECT 'BHO-STARTER-COMPUTERACC-ORICO-M2PV', 14, 4
    UNION ALL SELECT 'BHO-STARTER-MONITOR-DELL-P2425H', 7, 2
    UNION ALL SELECT 'BHO-STARTER-MONITOR-LG-27MR400', 8, 2
    UNION ALL SELECT 'BHO-STARTER-MONITOR-SAMSUNG-LS27C310', 9, 2
    UNION ALL SELECT 'BHO-STARTER-MONITOR-ASUS-VA24EHF', 10, 3
    UNION ALL SELECT 'BHO-STARTER-MONITOR-LG-32UN650', 4, 1
    UNION ALL SELECT 'BHO-STARTER-KEYBOARD-LOGITECH-K380S', 18, 5
    UNION ALL SELECT 'BHO-STARTER-KEYBOARD-LOGITECH-K120', 30, 8
    UNION ALL SELECT 'BHO-STARTER-KEYBOARD-KEYCHRON-K2V2', 10, 3
    UNION ALL SELECT 'BHO-STARTER-KEYBOARD-DAREU-EK87', 14, 4
    UNION ALL SELECT 'BHO-STARTER-KEYBOARD-AKKO-3087', 10, 3
    UNION ALL SELECT 'BHO-STARTER-MOUSE-LOGITECH-M650', 20, 6
    UNION ALL SELECT 'BHO-STARTER-MOUSE-LOGITECH-MXANYWHERE3S', 10, 3
    UNION ALL SELECT 'BHO-STARTER-MOUSE-LOGITECH-B100', 40, 10
    UNION ALL SELECT 'BHO-STARTER-MOUSE-MICROSOFT-BLUETOOTH', 18, 5
    UNION ALL SELECT 'BHO-STARTER-MOUSE-RAZER-DEATHADDERV2X', 10, 3
    UNION ALL SELECT 'BHO-STARTER-NETWORK-TP-LINK-ARCHERC64', 12, 4
    UNION ALL SELECT 'BHO-STARTER-NETWORK-ASUS-RTAX57', 7, 2
    UNION ALL SELECT 'BHO-STARTER-NETWORK-TP-LINK-RE315', 14, 4
    UNION ALL SELECT 'BHO-STARTER-NETWORK-TP-LINK-SG105', 12, 4
    UNION ALL SELECT 'BHO-STARTER-NETWORK-TENDA-U12', 10, 3
    UNION ALL SELECT 'BHO-STARTER-CHARGER-ANKER-NANO45W', 25, 7
    UNION ALL SELECT 'BHO-STARTER-CHARGER-UGREEN-NEXODE65W', 20, 6
    UNION ALL SELECT 'BHO-STARTER-CHARGER-BASEUS-CCGAN30W', 28, 8
    UNION ALL SELECT 'BHO-STARTER-CABLE-APPLE-USBC-1M', 30, 8
    UNION ALL SELECT 'BHO-STARTER-CABLE-ANKER-POWERLINE3', 24, 7
    UNION ALL SELECT 'BHO-STARTER-STORAGE-SAMSUNG-T7-1TB', 10, 3
    UNION ALL SELECT 'BHO-STARTER-STORAGE-SANDISK-EXTREME1TB', 8, 2
    UNION ALL SELECT 'BHO-STARTER-STORAGE-KINGSTON-NV2-1TB', 12, 3
    UNION ALL SELECT 'BHO-STARTER-STORAGE-WD-SN770-1TB', 10, 3
    UNION ALL SELECT 'BHO-STARTER-STORAGE-KINGSTON-DTX128', 25, 7
) AS seed
JOIN products p ON p.sku = seed.sku AND p.status = 'ACTIVE'
JOIN stores s ON s.status = 'ACTIVE'
LEFT JOIN inventory existing ON existing.store_id = s.id AND existing.product_id = p.id
WHERE existing.id IS NULL;

COMMIT;

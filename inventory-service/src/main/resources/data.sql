-- Clear existing products so the catalog is always in sync with this file.
-- Safe: Reservation.productId is a plain Long column (no FK constraint).
DELETE FROM products;

INSERT INTO products (name, description, price, stock_quantity, image_url) VALUES

-- ── Audio (3 products) ────────────────────────────────────────────────
('Sony WH-1000XM5',
 'Industry-leading ANC, 30-hour battery, multipoint Bluetooth 5.2, ultra-soft ear pads',
 349.99, 18,
 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600&q=80'),

('Bose QuietComfort Ultra Earbuds',
 'Immersive Audio spatial sound, CustomTune ANC, 6hr + 18hr case, IPX4, Qi wireless charging',
 279.99, 22,
 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600&q=80'),

('Marshall Emberton II Speaker',
 '20W signature Marshall sound, IP67 waterproof, 30hr battery, 360° sound, USB-C charge',
 149.99, 30,
 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600&q=80'),

-- ── Computing (5 products) ────────────────────────────────────────────
('Keychron Q3 Pro Keyboard',
 'Wireless TKL, QMK/VIA, gasket-mount, aluminium frame, hot-swap Gateron G Pro switches',
 199.99, 12,
 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&q=80'),

('Logitech MX Keys S Keyboard',
 'Smart Actions keys, backlit keys with proximity sensor, USB-C charge, Easy-Switch 3 devices',
 109.99, 25,
 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600&q=80'),

('Samsung T7 Shield SSD 2TB',
 '1050 MB/s read, IP65 shock/water/dust resistant, AES 256-bit, includes USB-C & USB-A cables',
 169.99, 20,
 'https://images.unsplash.com/photo-1531492746076-161ca9bcad58?w=600&q=80'),

('Twelve South HiRise Pro Stand',
 'Adjustable height & angle aluminium stand for MacBook/laptops up to 17", ventilated design',
 79.99, 35,
 'https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=600&q=80'),

('CalDigit TS4 Thunderbolt 4 Dock',
 '18 ports: 3× TB4, 5× USB-A, 2× USB-C, 2.5GbE, SD 4.0, 98W host charging, single cable',
 389.99, 8,
 'https://images.unsplash.com/photo-1625948515291-69613efd103f?w=600&q=80'),

-- ── Wearables (3 products) ────────────────────────────────────────────
('Apple Watch Ultra 2',
 '49mm titanium, dual-frequency GPS, 60hr battery (low power), Action button, 100m waterproof',
 799.99, 6,
 'https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=600&q=80'),

('Garmin Forerunner 965',
 'AMOLED touchscreen, full-color maps, HRV stress, training readiness, 31hr GPS battery',
 599.99, 9,
 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&q=80'),

('Oura Ring Gen 3',
 'Sleep score, readiness score, heart rate, SpO2, skin temp — titanium, 7-day battery',
 299.99, 15,
 'https://images.unsplash.com/photo-1617043786394-f977fa12eddf?w=600&q=80'),

-- ── Cameras (3 products) ──────────────────────────────────────────────
('Sony ZV-E10 II Mirrorless Camera',
 '26MP APS-C, 4K60p, real-time eye AF, vari-angle LCD, directional mic, E-mount lenses',
 748.99, 5,
 'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=600&q=80'),

('DJI Osmo Pocket 3',
 '1" CMOS, 4K120fps, 3-axis stabilisation, 2" rotatable touchscreen, 166min battery',
 519.99, 11,
 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600&q=80'),

('Elgato Facecam Pro Webcam',
 '4K60fps STARVIS 2 sensor, Sony lens, no compression passthrough, Edge app AI framing',
 299.99, 18,
 'https://images.unsplash.com/photo-1587826080692-f439cd0b70da?w=600&q=80'),

-- ── Power (3 products) ────────────────────────────────────────────────
('Ugreen Nexode Pro 160W Charger',
 '4-port GaN (2× USB-C 140W + 2× USB-A), charges MacBook + iPad + iPhone simultaneously',
 89.99, 50,
 'https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=600&q=80'),

('Anker SOLIX C200 Power Station',
 '2048Wh LiFePO4, 2400W AC, solar input 600W, app control, 10-year lifespan, UPS function',
 1299.99, 3,
 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=600&q=80'),

('Belkin BoostCharge Pro Pad 3-in-1',
 'MagSafe 15W + Watch fast charge + AirPods, foldable travel design, USB-C cable included',
 149.99, 40,
 'https://images.unsplash.com/photo-1586495777744-4e6232bf2176?w=600&q=80'),

-- ── Displays (3 products) ─────────────────────────────────────────────
('LG 32GQ850-B 32" QHD Monitor',
 '2560×1440 Nano IPS, 240Hz, 1ms GtG, HDMI 2.1, G-Sync Compatible, 98% DCI-P3',
 549.99, 0,
 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&q=80'),

('BenQ ScreenBar Halo',
 'Auto-dimming + back glow ambient light, wireless dial controller, asymmetric optical beam',
 219.99, 22,
 'https://images.unsplash.com/photo-1614680376573-df3480f0c6ff?w=600&q=80'),

('Elgato Stream Deck +',
 '8 LCD keys + 4 touch-strip encoders, unlimited custom pages, Works with 300+ apps & tools',
 199.99, 4,
 'https://images.unsplash.com/photo-1547394765-185e1e68f34e?w=600&q=80');

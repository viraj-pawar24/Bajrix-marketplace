-- Sample data: enough overlap between sellers/products to exercise price
-- comparison, seller status filtering, out-of-stock/MOQ edge cases and a
-- stopped listing.

INSERT INTO sellers (name, contact_email, status) VALUES
 ('Shree Traders', 'contact@shreetraders.example', 'APPROVED'),
 ('Om Building Supplies', 'sales@ombuildingsupplies.example', 'APPROVED'),
 ('Konkan Hardware Mart', 'info@konkanhardware.example', 'APPROVED'),
 ('Patil Cement Depot', 'orders@patilcement.example', 'APPROVED'),
 ('Deccan Steel Co.', 'hello@deccansteel.example', 'APPROVED'),
 ('NewLine Constructions', 'newline@example.com', 'PENDING'),
 ('Fresh Start Suppliers', 'freshstart@example.com', 'PENDING'),
 ('Bygone Materials Ltd', 'bygone@example.com', 'REJECTED');

INSERT INTO products (name, brand, category, unit, description) VALUES
 ('PPC Cement 50kg', 'UltraTech', 'Cement', '50 kg bag', 'Portland Pozzolana Cement, general construction use.'),
 ('OPC 53 Grade Cement 50kg', 'ACC', 'Cement', '50 kg bag', 'Ordinary Portland Cement, 53 grade, for structural work.'),
 ('TMT Bar 12mm', 'Tata Tiscon', 'Steel', '12mm x 12m rod', 'Fe550D grade TMT reinforcement bar.'),
 ('TMT Bar 8mm', 'Tata Tiscon', 'Steel', '8mm x 12m rod', 'Fe550D grade TMT reinforcement bar, lighter gauge.'),
 ('Red Clay Brick', 'Local', 'Bricks & Blocks', 'per 1000 bricks', 'Standard size red clay bricks for masonry.'),
 ('AAC Block 600x200x100mm', 'Siporex', 'Bricks & Blocks', 'per block', 'Autoclaved aerated concrete block, lightweight.'),
 ('River Sand', 'Local', 'Aggregates', 'per brass', 'Washed river sand for plastering and concrete.'),
 ('20mm Aggregate', 'Local', 'Aggregates', 'per brass', 'Crushed stone aggregate for concrete work.'),
 ('Exterior Emulsion Paint 20L', 'Asian Paints', 'Paint', '20 litre pail', 'Weatherproof exterior emulsion, white base.'),
 ('Interior Emulsion Paint 10L', 'Berger', 'Paint', '10 litre pail', 'Smooth interior wall emulsion.'),
 ('Vitrified Floor Tile 2x2', 'Kajaria', 'Tiles', 'per box (4 tiles)', 'Glossy finish vitrified tile, 600x600mm.'),
 ('CPVC Pipe 1 inch', 'Astral', 'Plumbing', '3 metre length', 'CPVC pipe for hot & cold water lines.'),
 ('PVC Pipe 4 inch', 'Supreme', 'Plumbing', '3 metre length', 'PVC pipe for drainage.'),
 ('Wash Basin - Ceramic', 'Hindware', 'Sanitaryware', 'per piece', 'Wall-mounted ceramic wash basin.'),
 ('Electrical Wire 1.5sqmm', 'Polycab', '100 metre coil', 'Electrical', 'FRLS insulated copper wire, single core.');

-- Cement: classic multi-seller price comparison example from the brief.
INSERT INTO seller_listings (product_id, seller_id, price, stock, min_order_qty, active) VALUES
 ((SELECT id FROM products WHERE name = 'PPC Cement 50kg'), (SELECT id FROM sellers WHERE name = 'Shree Traders'), 390.00, 500, 10, TRUE),
 ((SELECT id FROM products WHERE name = 'PPC Cement 50kg'), (SELECT id FROM sellers WHERE name = 'Om Building Supplies'), 385.00, 120, 20, TRUE),
 ((SELECT id FROM products WHERE name = 'PPC Cement 50kg'), (SELECT id FROM sellers WHERE name = 'Patil Cement Depot'), 405.00, 800, 5, TRUE),
 ((SELECT id FROM products WHERE name = 'PPC Cement 50kg'), (SELECT id FROM sellers WHERE name = 'Konkan Hardware Mart'), 398.00, 0, 10, TRUE), -- in stock=0 -> shown but not orderable

 ((SELECT id FROM products WHERE name = 'OPC 53 Grade Cement 50kg'), (SELECT id FROM sellers WHERE name = 'Patil Cement Depot'), 412.00, 300, 10, TRUE),
 ((SELECT id FROM products WHERE name = 'OPC 53 Grade Cement 50kg'), (SELECT id FROM sellers WHERE name = 'Shree Traders'), 420.00, 150, 10, TRUE),

 ((SELECT id FROM products WHERE name = 'TMT Bar 12mm'), (SELECT id FROM sellers WHERE name = 'Deccan Steel Co.'), 68000.00, 40, 1, TRUE),
 ((SELECT id FROM products WHERE name = 'TMT Bar 12mm'), (SELECT id FROM sellers WHERE name = 'Konkan Hardware Mart'), 69500.00, 15, 2, TRUE),
 ((SELECT id FROM products WHERE name = 'TMT Bar 12mm'), (SELECT id FROM sellers WHERE name = 'Om Building Supplies'), 67800.00, 5, 1, FALSE), -- stopped selling: excluded from buyer view

 ((SELECT id FROM products WHERE name = 'TMT Bar 8mm'), (SELECT id FROM sellers WHERE name = 'Deccan Steel Co.'), 64500.00, 60, 1, TRUE),

 ((SELECT id FROM products WHERE name = 'Red Clay Brick'), (SELECT id FROM sellers WHERE name = 'Shree Traders'), 6800.00, 25, 1, TRUE),
 ((SELECT id FROM products WHERE name = 'Red Clay Brick'), (SELECT id FROM sellers WHERE name = 'Konkan Hardware Mart'), 6500.00, 40, 2, TRUE),

 ((SELECT id FROM products WHERE name = 'AAC Block 600x200x100mm'), (SELECT id FROM sellers WHERE name = 'Om Building Supplies'), 62.00, 3000, 100, TRUE),

 ((SELECT id FROM products WHERE name = 'River Sand'), (SELECT id FROM sellers WHERE name = 'Shree Traders'), 5200.00, 18, 1, TRUE),
 ((SELECT id FROM products WHERE name = 'River Sand'), (SELECT id FROM sellers WHERE name = 'Patil Cement Depot'), 4950.00, 2, 3, TRUE), -- stock < MOQ -> not orderable

 ((SELECT id FROM products WHERE name = '20mm Aggregate'), (SELECT id FROM sellers WHERE name = 'Shree Traders'), 3800.00, 30, 1, TRUE),

 ((SELECT id FROM products WHERE name = 'Exterior Emulsion Paint 20L'), (SELECT id FROM sellers WHERE name = 'Konkan Hardware Mart'), 5400.00, 22, 1, TRUE),
 ((SELECT id FROM products WHERE name = 'Exterior Emulsion Paint 20L'), (SELECT id FROM sellers WHERE name = 'Om Building Supplies'), 5250.00, 8, 1, TRUE),

 ((SELECT id FROM products WHERE name = 'Interior Emulsion Paint 10L'), (SELECT id FROM sellers WHERE name = 'Konkan Hardware Mart'), 2450.00, 45, 1, TRUE),

 ((SELECT id FROM products WHERE name = 'Vitrified Floor Tile 2x2'), (SELECT id FROM sellers WHERE name = 'Om Building Supplies'), 780.00, 200, 4, TRUE),
 ((SELECT id FROM products WHERE name = 'Vitrified Floor Tile 2x2'), (SELECT id FROM sellers WHERE name = 'Shree Traders'), 810.00, 90, 4, TRUE),

 ((SELECT id FROM products WHERE name = 'CPVC Pipe 1 inch'), (SELECT id FROM sellers WHERE name = 'Konkan Hardware Mart'), 340.00, 150, 5, TRUE),

 ((SELECT id FROM products WHERE name = 'PVC Pipe 4 inch'), (SELECT id FROM sellers WHERE name = 'Konkan Hardware Mart'), 610.00, 70, 5, TRUE),

 ((SELECT id FROM products WHERE name = 'Wash Basin - Ceramic'), (SELECT id FROM sellers WHERE name = 'Om Building Supplies'), 1450.00, 35, 1, TRUE),

 ((SELECT id FROM products WHERE name = 'Electrical Wire 1.5sqmm'), (SELECT id FROM sellers WHERE name = 'Deccan Steel Co.'), 1250.00, 60, 2, TRUE);

-- A PENDING seller has listings too, but they must never surface to buyers
-- (findBuyerVisibleListingsForProduct filters on seller status = APPROVED).
INSERT INTO seller_listings (product_id, seller_id, price, stock, min_order_qty, active) VALUES
 ((SELECT id FROM products WHERE name = 'PPC Cement 50kg'), (SELECT id FROM sellers WHERE name = 'NewLine Constructions'), 375.00, 200, 10, TRUE),
 ((SELECT id FROM products WHERE name = 'Red Clay Brick'), (SELECT id FROM sellers WHERE name = 'Fresh Start Suppliers'), 6100.00, 50, 1, TRUE);

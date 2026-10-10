ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT 'General';

UPDATE products SET category = 'Tech' WHERE name IN ('Wireless Keyboard', 'Phone Case');
UPDATE products SET category = 'Home office' WHERE name = 'Desk Mat';
UPDATE products SET category = 'Outdoors' WHERE name = 'Water Bottle';

INSERT INTO products (name, description, price, stock, category) VALUES
('Noise-Cancelling Headphones', 'Over-ear Bluetooth headphones with 30 hour battery', 89.00, 12, 'Tech'),
('USB-C Charging Cable', 'Braided 2 metre cable with fast charging', 9.50, 60, 'Tech'),
('Wireless Mouse', 'Silent click mouse with a quiet scroll wheel', 19.99, 25, 'Tech'),
('Portable Power Bank', '10000 mAh battery with two USB ports', 24.90, 18, 'Tech'),
('Webcam 1080p', 'Full HD webcam with a built-in microphone', 34.00, 8, 'Tech'),
('Laptop Stand', 'Aluminium adjustable stand for 13 to 17 inch laptops', 29.50, 15, 'Tech'),
('Desk Lamp', 'LED lamp with three brightness levels and a USB port', 22.00, 14, 'Home office'),
('Seat Cushion', 'Memory foam cushion that supports your lower back', 27.50, 10, 'Home office'),
('Monitor Riser', 'Wooden shelf that lifts your screen and hides cables', 31.00, 9, 'Home office'),
('Cable Organizer Box', 'Hides power strips and tidies loose cables', 14.00, 22, 'Home office'),
('Ceramic Mug', 'Blue ceramic mug, 350ml, dishwasher safe', 9.99, 40, 'Kitchen'),
('French Press', 'Glass coffee press for four cups', 21.00, 11, 'Kitchen'),
('Cast Iron Pan', '26cm pre-seasoned pan for the stove or oven', 38.00, 6, 'Kitchen'),
('Bamboo Chopping Board', 'Large board with a juice groove', 17.50, 20, 'Kitchen'),
('Lunch Box', 'Leakproof lunch box with two compartments', 12.90, 30, 'Kitchen'),
('Camping Headlamp', 'Rechargeable LED headlamp, 200 lumens', 16.00, 16, 'Outdoors'),
('Foldable Daypack', 'Light backpack that folds into its own pocket', 24.00, 13, 'Outdoors'),
('Picnic Blanket', 'Water-resistant blanket with a carry strap', 19.00, 17, 'Outdoors'),
('Packing Cubes', 'Set of four cubes that keep a suitcase tidy', 18.00, 24, 'Travel'),
('Travel Adapter', 'Works in over 150 countries, with three USB ports', 15.50, 19, 'Travel'),
('Neck Pillow', 'Memory foam pillow for long journeys', 13.50, 21, 'Travel'),
('Hardcover Notebook', 'A5 dotted notebook with 192 pages', 11.00, 45, 'Stationery'),
('Gel Pen Set', 'Twelve smooth pens in assorted colours', 8.00, 50, 'Stationery'),
('Desk Planner', 'Weekly planner with undated pages', 10.50, 4, 'Stationery'),
('Sticky Notes Pack', 'Six pads in pastel colours', 5.50, 0, 'Stationery');
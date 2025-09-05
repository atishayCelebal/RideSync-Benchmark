-- Flyway migration V2: Insert sample data for testing
-- This script inserts sample data for development and testing purposes

-- Insert sample users (UUIDs will be auto-generated)
INSERT INTO users (username, email, password, first_name, last_name, role) VALUES
('admin', 'admin@ridesync.com', 'admin123', 'Admin', 'User', 'ADMIN'),
('john_doe', 'john@example.com', 'password123', 'John', 'Doe', 'USER'),
('jane_smith', 'jane@example.com', 'password123', 'Jane', 'Smith', 'USER'),
('bob_wilson', 'bob@example.com', 'password123', 'Bob', 'Wilson', 'USER'),
('alice_brown', 'alice@example.com', 'password123', 'Alice', 'Brown', 'USER');

-- Insert sample groups using subqueries to get user UUIDs
INSERT INTO groups (name, description, admin_id) VALUES
('Weekend Riders', 'Group for weekend cycling trips', (SELECT id FROM users WHERE username = 'admin')),
('Commute Squad', 'Daily commute group', (SELECT id FROM users WHERE username = 'john_doe')),
('Mountain Bikers', 'Mountain biking enthusiasts', (SELECT id FROM users WHERE username = 'jane_smith'));

-- Insert sample group members using subqueries
INSERT INTO group_members (group_id, user_id, role) VALUES
((SELECT id FROM groups WHERE name = 'Weekend Riders'), (SELECT id FROM users WHERE username = 'admin'), 'ADMIN'),
((SELECT id FROM groups WHERE name = 'Weekend Riders'), (SELECT id FROM users WHERE username = 'john_doe'), 'MEMBER'),
((SELECT id FROM groups WHERE name = 'Weekend Riders'), (SELECT id FROM users WHERE username = 'jane_smith'), 'MEMBER'),
((SELECT id FROM groups WHERE name = 'Commute Squad'), (SELECT id FROM users WHERE username = 'john_doe'), 'ADMIN'),
((SELECT id FROM groups WHERE name = 'Commute Squad'), (SELECT id FROM users WHERE username = 'bob_wilson'), 'MEMBER'),
((SELECT id FROM groups WHERE name = 'Commute Squad'), (SELECT id FROM users WHERE username = 'alice_brown'), 'MEMBER'),
((SELECT id FROM groups WHERE name = 'Mountain Bikers'), (SELECT id FROM users WHERE username = 'jane_smith'), 'ADMIN'),
((SELECT id FROM groups WHERE name = 'Mountain Bikers'), (SELECT id FROM users WHERE username = 'bob_wilson'), 'MEMBER'),
((SELECT id FROM groups WHERE name = 'Mountain Bikers'), (SELECT id FROM users WHERE username = 'alice_brown'), 'MEMBER');

-- Insert sample rides using subqueries
INSERT INTO rides (name, description, group_id, user_id, status, start_time) VALUES
('Morning Commute', 'Daily morning commute to office', (SELECT id FROM groups WHERE name = 'Commute Squad'), (SELECT id FROM users WHERE username = 'john_doe'), 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
('Weekend Adventure', 'Scenic weekend ride', (SELECT id FROM groups WHERE name = 'Weekend Riders'), (SELECT id FROM users WHERE username = 'admin'), 'PLANNED', CURRENT_TIMESTAMP + INTERVAL '2 days'),
('Mountain Trail', 'Challenging mountain trail ride', (SELECT id FROM groups WHERE name = 'Mountain Bikers'), (SELECT id FROM users WHERE username = 'jane_smith'), 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '30 minutes');

-- Insert sample location updates using subqueries
INSERT INTO location_updates (ride_id, user_id, latitude, longitude, speed, heading, device_id) VALUES
((SELECT id FROM rides WHERE name = 'Morning Commute'), (SELECT id FROM users WHERE username = 'john_doe'), 40.7128, -74.0060, 15.5, 45.0, (SELECT id FROM devices WHERE device_id = 'galaxy_s24_001')),
((SELECT id FROM rides WHERE name = 'Morning Commute'), (SELECT id FROM users WHERE username = 'john_doe'), 40.7130, -74.0058, 16.2, 47.0, (SELECT id FROM devices WHERE device_id = 'galaxy_s24_001')),
((SELECT id FROM rides WHERE name = 'Morning Commute'), (SELECT id FROM users WHERE username = 'john_doe'), 40.7132, -74.0056, 14.8, 49.0, (SELECT id FROM devices WHERE device_id = 'galaxy_s24_001')),
((SELECT id FROM rides WHERE name = 'Mountain Trail'), (SELECT id FROM users WHERE username = 'jane_smith'), 40.7589, -73.9851, 8.5, 120.0, (SELECT id FROM devices WHERE device_id = 'iphone_14_001')),
((SELECT id FROM rides WHERE name = 'Mountain Trail'), (SELECT id FROM users WHERE username = 'jane_smith'), 40.7591, -73.9849, 9.1, 122.0, (SELECT id FROM devices WHERE device_id = 'iphone_14_001'));

-- Insert sample devices using subqueries
INSERT INTO devices (device_name, device_id, device_type, os_version, app_version, gps_accuracy, user_id) VALUES
('iPhone 15 Pro', 'iphone_15_pro_001', 'MOBILE', 'iOS 17.2', '1.0.0', 3.5, (SELECT id FROM users WHERE username = 'admin')),
('Samsung Galaxy S24', 'galaxy_s24_001', 'MOBILE', 'Android 14', '1.0.0', 4.2, (SELECT id FROM users WHERE username = 'john_doe')),
('iPhone 14', 'iphone_14_001', 'MOBILE', 'iOS 16.7', '1.0.0', 5.1, (SELECT id FROM users WHERE username = 'jane_smith')),
('Garmin Edge 1040', 'garmin_edge_001', 'BIKE_COMPUTER', 'Firmware 15.2', '1.0.0', 2.8, (SELECT id FROM users WHERE username = 'bob_wilson')),
('Apple Watch Series 9', 'apple_watch_001', 'SMARTWATCH', 'watchOS 10.2', '1.0.0', 8.5, (SELECT id FROM users WHERE username = 'alice_brown'));

-- Insert sample alerts using subqueries with device context
INSERT INTO alerts (ride_id, user_id, device_id, type, message, severity, latitude, longitude) VALUES
((SELECT id FROM rides WHERE name = 'Morning Commute'), (SELECT id FROM users WHERE username = 'john_doe'), (SELECT id FROM devices WHERE device_id = 'galaxy_s24_001'), 'STATIONARY', 'User john_doe (Samsung Galaxy S24, Android 14) has been stationary for 180 seconds', 'WARNING', 40.7128, -74.0060),
((SELECT id FROM rides WHERE name = 'Mountain Trail'), (SELECT id FROM users WHERE username = 'jane_smith'), (SELECT id FROM devices WHERE device_id = 'iphone_14_001'), 'DIRECTION_DRIFT', 'User jane_smith (iPhone 14, iOS 16.7) is drifting from group direction by 45.5 degrees', 'WARNING', 40.7589, -73.9851);

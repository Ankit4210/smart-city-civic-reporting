-- =============================================================================
-- SAFE & SMART CITY - CIVIC ISSUE REPORTING SYSTEM
-- Database: smart_city_db
-- Target RDBMS: MySQL 8.0+ / MariaDB / SQLite Compatible Schema
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `smart_city_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `smart_city_db`;

-- Drop existing tables in reverse dependency order
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `activity_logs`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `upvotes`;
DROP TABLE IF EXISTS `resolutions`;
DROP TABLE IF EXISTS `reports`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- Table 1: users (Citizen & Municipal Admin Profiles)
-- -----------------------------------------------------------------------------
CREATE TABLE `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `role` ENUM('citizen', 'admin', 'officer') DEFAULT 'citizen',
  `civic_points` INT DEFAULT 50,
  `ward` VARCHAR(100) DEFAULT 'Ward 12 - Central',
  `avatar` VARCHAR(255) DEFAULT 'default-avatar.png',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_users_email` (`email`),
  INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Table 2: categories (Civic Issue Classification & SLA)
-- -----------------------------------------------------------------------------
CREATE TABLE `categories` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `icon` VARCHAR(50) NOT NULL,
  `department` VARCHAR(100) NOT NULL,
  `sla_hours` INT DEFAULT 48,
  `description` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Table 3: reports (Citizen Civic Complaints & Geolocation)
-- -----------------------------------------------------------------------------
CREATE TABLE `reports` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `tracking_id` VARCHAR(50) NOT NULL UNIQUE,
  `user_id` INT NOT NULL,
  `category_id` INT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT NOT NULL,
  `landmark` VARCHAR(200) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `ward` VARCHAR(100) DEFAULT 'Ward 12 - Central',
  `latitude` DECIMAL(10, 7) NOT NULL,
  `longitude` DECIMAL(10, 7) NOT NULL,
  `before_photo` VARCHAR(255) DEFAULT NULL,
  `priority` ENUM('Low', 'Medium', 'High', 'Critical') DEFAULT 'Medium',
  `status` ENUM('Pending', 'In Progress', 'Resolved', 'Rejected') DEFAULT 'Pending',
  `upvotes` INT DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE RESTRICT,
  INDEX `idx_reports_tracking` (`tracking_id`),
  INDEX `idx_reports_status` (`status`),
  INDEX `idx_reports_ward` (`ward`),
  INDEX `idx_reports_coords` (`latitude`, `longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Table 4: resolutions (Verified After-Photo Proof & Resolution Remarks)
-- -----------------------------------------------------------------------------
CREATE TABLE `resolutions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `report_id` INT NOT NULL UNIQUE,
  `admin_id` INT NOT NULL,
  `after_photo` VARCHAR(255) NOT NULL,
  `remarks` TEXT NOT NULL,
  `action_taken` VARCHAR(200) DEFAULT 'Site rectified by municipal crew',
  `resolved_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Table 5: upvotes (Community Validation & Duplicate Prevention)
-- -----------------------------------------------------------------------------
CREATE TABLE `upvotes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `report_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_report_user_upvote` (`report_id`, `user_id`),
  FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Table 6: feedback (Post-Resolution Citizen Satisfaction Rating)
-- -----------------------------------------------------------------------------
CREATE TABLE `feedback` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `report_id` INT NOT NULL UNIQUE,
  `user_id` INT NOT NULL,
  `rating` INT NOT NULL CHECK (`rating` BETWEEN 1 AND 5),
  `is_satisfied` TINYINT(1) NOT NULL DEFAULT 1,
  `comments` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Table 7: notifications (Citizen Status & Gamification Alerts)
-- -----------------------------------------------------------------------------
CREATE TABLE `notifications` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `report_id` INT DEFAULT NULL,
  `title` VARCHAR(150) NOT NULL,
  `message` TEXT NOT NULL,
  `type` VARCHAR(50) DEFAULT 'status_update',
  `is_read` TINYINT(1) DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Table 8: activity_logs (Audit Trail & Timeline)
-- -----------------------------------------------------------------------------
CREATE TABLE `activity_logs` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `report_id` INT NOT NULL,
  `actor_name` VARCHAR(100) NOT NULL,
  `action` VARCHAR(100) NOT NULL,
  `details` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- SAMPLE SEED DATA INSERTION (Academic Demonstration)
-- =============================================================================

-- Seed Categories
INSERT INTO `categories` (`id`, `name`, `code`, `icon`, `department`, `sla_hours`, `description`) VALUES
(1, 'Garbage & Waste Accumulation', 'GARBAGE', 'fa-trash-can', 'Solid Waste Management', 24, 'Overflowing bins, illegal garbage dumps, bio-waste'),
(2, 'Potholes & Damaged Roads', 'ROADS', 'fa-road-circle-exclamation', 'Roads & Infrastructure', 72, 'Road craters, broken tarmac, missing manhole covers'),
(3, 'Streetlight Failure & Dark Spots', 'LIGHTING', 'fa-lightbulb', 'Electrical Engineering', 24, 'Non-functional lamps, hanging wires, low lighting'),
(4, 'Water Leakage & Pipeline Burst', 'WATER', 'fa-faucet-drip', 'Water Supply & Sewerage', 12, 'Drinking water pipeline bursts, contaminated supply'),
(5, 'Drainage & Sewage Overflow', 'DRAINAGE', 'fa-water', 'Public Health Engineering', 24, 'Blocked storm drains, open sewage overflow'),
(6, 'Public Littering & Urination', 'HYGIENE', 'fa-hand-sparkles', 'Sanitation & Health', 48, 'Public hygiene violations, unsanitary public spaces'),
(7, 'Broken Footpath & Encroachment', 'FOOTPATH', 'fa-person-walking-dashed-line-arrow-right', 'Town Planning & Enforcement', 96, 'Destroyed pedestrian walkways, unauthorized stalls');

-- Seed Users (Bcrypt hash for 'admin123' and 'citizen123')
INSERT INTO `users` (`id`, `name`, `email`, `password`, `phone`, `role`, `civic_points`, `ward`) VALUES
(1, 'Demo Municipal Admin', 'admin@example.test', '$2a$10$QL5ac0JWVNzIrmHXTIM02eK.1gutuUxMmTrTHUR.rQENy6P2t/vuC', '0000000001', 'admin', 500, 'Headquarters'),
(2, 'Demo Ward Officer', 'officer@example.test', '$2a$10$QL5ac0JWVNzIrmHXTIM02eK.1gutuUxMmTrTHUR.rQENy6P2t/vuC', '0000000002', 'admin', 350, 'Ward 12 - Central'),
(3, 'Demo Citizen One', 'citizen.one@example.test', '$2a$10$faWLc5Yw5LtTE5gdIqgH0evWroGywVppKZZ6EmQsDDSx.fCGEilVi', '0000000003', 'citizen', 220, 'Ward 12 - Central'),
(4, 'Demo Citizen Two', 'citizen.two@example.test', '$2a$10$faWLc5Yw5LtTE5gdIqgH0evWroGywVppKZZ6EmQsDDSx.fCGEilVi', '0000000004', 'citizen', 180, 'Ward 7 - North'),
(5, 'Demo Citizen Three', 'citizen.three@example.test', '$2a$10$faWLc5Yw5LtTE5gdIqgH0evWroGywVppKZZ6EmQsDDSx.fCGEilVi', '0000000005', 'citizen', 110, 'Ward 4 - East');

-- Seed Reports
INSERT INTO `reports` (`id`, `tracking_id`, `user_id`, `category_id`, `title`, `description`, `landmark`, `address`, `ward`, `latitude`, `longitude`, `before_photo`, `priority`, `status`, `upvotes`) VALUES
(1, 'SSC-2026-8812', 3, 1, 'Overflowing garbage bin near Central Market Gate 2', 'Garbage has not been collected for the past 4 days. Foul smell and stray animals causing severe public hazard.', 'Opposite State Bank ATM, Central Market', 'Market Road, Ward 12 - Central', 'Ward 12 - Central', 28.6139000, 77.2090000, 'sample_garbage_before.jpg', 'High', 'In Progress', 14),
(2, 'SSC-2026-9041', 4, 2, 'Deep hazardous pothole on Main Ring Road curve', 'Large crater on the road causing traffic slowdowns and two-wheeler skids especially during nighttime.', 'Near Metro Pillar 142', 'North Ring Road, Ward 7 - North', 'Ward 7 - North', 28.6328000, 77.2197000, 'sample_pothole_before.jpg', 'Critical', 'Resolved', 28),
(3, 'SSC-2026-6734', 3, 3, 'Entire row of 4 streetlights dark near Girls Hostel lane', 'Streetlights out for over a week creating unsafe dark spot for pedestrians at night.', 'Lane 4 behind Community Center', 'Shanti Path, Ward 12 - Central', 'Ward 12 - Central', 28.6180000, 77.2150000, 'sample_streetlight_before.jpg', 'High', 'Pending', 9),
(4, 'SSC-2026-4190', 5, 4, 'Major main supply water leakage flooding street', 'Fresh clean drinking water bursting from underground joint and wasting thousands of liters per hour.', 'Corner of Sector 4 Park', 'Park Avenue, Ward 4 - East', 'Ward 4 - East', 28.6050000, 77.2280000, 'sample_water_before.jpg', 'Critical', 'In Progress', 19);

-- Seed Resolution Record
INSERT INTO `resolutions` (`id`, `report_id`, `admin_id`, `after_photo`, `remarks`, `action_taken`, `resolved_at`) VALUES
(1, 2, 1, 'sample_pothole_after.jpg', 'Patching work completed with cold-mix asphalt. Traffic flow restored.', 'Asphalt Filling & Roller Compaction', CURRENT_TIMESTAMP);

-- Seed Feedback Record
INSERT INTO `feedback` (`id`, `report_id`, `user_id`, `rating`, `is_satisfied`, `comments`) VALUES
(1, 2, 4, 5, 1, 'Very quick resolution within 24 hours! Thank you municipal team.');

-- Seed Upvotes
INSERT INTO `upvotes` (`report_id`, `user_id`) VALUES
(1, 3), (1, 4), (1, 5),
(2, 4), (2, 3), (2, 5),
(3, 3), (3, 4),
(4, 5), (4, 3);

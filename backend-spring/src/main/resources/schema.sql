-- FairAir Database Schema
-- Compatible with H2, PostgreSQL, and SQLite
-- To migrate: just change the R2DBC connection URL in application.yml

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for email lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
    pnr VARCHAR(6) PRIMARY KEY,
    booking_reference VARCHAR(36) NOT NULL UNIQUE,
    user_id VARCHAR(36),
    flight_number VARCHAR(10) NOT NULL,
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    fare_family VARCHAR(20) NOT NULL,
    passengers_json TEXT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for booking lookups
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(created_at);

-- ============================================================================
-- ADMIN & CONTENT MANAGEMENT
-- ============================================================================

-- Admin users (separate from customer users)
CREATE TABLE IF NOT EXISTS admin_users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL, -- SUPER_ADMIN, CONTENT_MANAGER, MARKETING, B2B_SALES
    department VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_users_email ON admin_users(email);
CREATE INDEX IF NOT EXISTS idx_admin_users_role ON admin_users(role);

-- Static pages (About, Aircraft, Media Centre, etc.)
CREATE TABLE IF NOT EXISTS static_pages (
    id VARCHAR(36) PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE, -- about-us, aircraft, media-centre
    title VARCHAR(255) NOT NULL,
    title_ar VARCHAR(255),
    content TEXT NOT NULL,
    content_ar TEXT,
    meta_description VARCHAR(500),
    meta_description_ar VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    created_by VARCHAR(36) NOT NULL,
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES admin_users(id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_static_pages_slug ON static_pages(slug);
CREATE INDEX IF NOT EXISTS idx_static_pages_published ON static_pages(is_published);

-- Legal documents (Privacy Policy, Terms, Cookie Policy, etc.)
CREATE TABLE IF NOT EXISTS legal_documents (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(50) NOT NULL, -- PRIVACY_POLICY, TERMS_OF_USE, COOKIE_POLICY, CARRIER_REGULATIONS, CONDITIONS_OF_CARRIAGE, CUSTOMER_RIGHTS, PRINCIPLE_OF_USE
    version VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    title_ar VARCHAR(255),
    content TEXT NOT NULL,
    content_ar TEXT,
    effective_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES admin_users(id),
    UNIQUE(type, version)
);

CREATE INDEX IF NOT EXISTS idx_legal_documents_type ON legal_documents(type);
CREATE INDEX IF NOT EXISTS idx_legal_documents_current ON legal_documents(is_current);

-- Promotions
CREATE TABLE IF NOT EXISTS promotions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) UNIQUE, -- Optional promo code
    title VARCHAR(255) NOT NULL,
    title_ar VARCHAR(255),
    description TEXT,
    description_ar TEXT,
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'SAR',
    min_purchase_amount DECIMAL(10, 2),
    max_discount_amount DECIMAL(10, 2),
    origin_code VARCHAR(3), -- NULL means all origins
    destination_code VARCHAR(3), -- NULL means all destinations
    fare_family VARCHAR(20), -- NULL means all fare families
    image_url VARCHAR(500),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    max_uses INT,
    current_uses INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(36) NOT NULL,
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES admin_users(id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_promotions_active ON promotions(is_active);
CREATE INDEX IF NOT EXISTS idx_promotions_dates ON promotions(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(code);

-- Destination content (for destination pages)
CREATE TABLE IF NOT EXISTS destination_content (
    id VARCHAR(36) PRIMARY KEY,
    airport_code VARCHAR(3) NOT NULL UNIQUE,
    city_name VARCHAR(100) NOT NULL,
    city_name_ar VARCHAR(100),
    country VARCHAR(100) NOT NULL,
    country_ar VARCHAR(100),
    description TEXT,
    description_ar TEXT,
    highlights TEXT, -- JSON array of highlights
    highlights_ar TEXT,
    image_url VARCHAR(500),
    gallery_urls TEXT, -- JSON array of image URLs
    lowest_fare DECIMAL(10, 2),
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(36) NOT NULL,
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES admin_users(id),
    FOREIGN KEY (updated_by) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_destination_content_code ON destination_content(airport_code);
CREATE INDEX IF NOT EXISTS idx_destination_content_featured ON destination_content(is_featured);

-- ============================================================================
-- B2B / AGENCY PORTAL
-- ============================================================================

-- Travel agencies
CREATE TABLE IF NOT EXISTS agencies (
    id VARCHAR(36) PRIMARY KEY,
    agency_code VARCHAR(20) NOT NULL UNIQUE, -- e.g., AGN001
    name VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    type VARCHAR(50) NOT NULL, -- TRAVEL_AGENT, CORPORATE, TOUR_OPERATOR
    contact_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    tax_id VARCHAR(100),
    license_number VARCHAR(100),
    commission_rate DECIMAL(5, 2) DEFAULT 0.00, -- Percentage
    credit_limit DECIMAL(12, 2) DEFAULT 0.00,
    current_balance DECIMAL(12, 2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, SUSPENDED, REJECTED
    approved_by VARCHAR(36),
    approved_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approved_by) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_agencies_code ON agencies(agency_code);
CREATE INDEX IF NOT EXISTS idx_agencies_status ON agencies(status);
CREATE INDEX IF NOT EXISTS idx_agencies_type ON agencies(type);

-- Agency users (staff of travel agencies)
CREATE TABLE IF NOT EXISTS agency_users (
    id VARCHAR(36) PRIMARY KEY,
    agency_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL, -- AGENCY_ADMIN, AGENT
    phone VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agency_id) REFERENCES agencies(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_agency_users_email ON agency_users(email);
CREATE INDEX IF NOT EXISTS idx_agency_users_agency ON agency_users(agency_id);

-- Group booking requests
CREATE TABLE IF NOT EXISTS group_booking_requests (
    id VARCHAR(36) PRIMARY KEY,
    request_number VARCHAR(20) NOT NULL UNIQUE, -- GRP-YYYYMMDD-XXXX
    agency_id VARCHAR(36), -- NULL for direct requests
    contact_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    company_name VARCHAR(255),
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    departure_date DATE NOT NULL,
    return_date DATE,
    passenger_count INT NOT NULL,
    trip_type VARCHAR(20) NOT NULL, -- ONE_WAY, ROUND_TRIP
    fare_class_preference VARCHAR(20), -- ECONOMY, BUSINESS
    special_requirements TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, QUOTED, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    quoted_amount DECIMAL(12, 2),
    quoted_currency VARCHAR(3),
    quoted_at TIMESTAMP,
    quoted_by VARCHAR(36),
    quote_valid_until TIMESTAMP,
    booking_pnr VARCHAR(6),
    assigned_to VARCHAR(36),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agency_id) REFERENCES agencies(id),
    FOREIGN KEY (quoted_by) REFERENCES admin_users(id),
    FOREIGN KEY (assigned_to) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_group_requests_number ON group_booking_requests(request_number);
CREATE INDEX IF NOT EXISTS idx_group_requests_status ON group_booking_requests(status);
CREATE INDEX IF NOT EXISTS idx_group_requests_agency ON group_booking_requests(agency_id);

-- Charter requests
CREATE TABLE IF NOT EXISTS charter_requests (
    id VARCHAR(36) PRIMARY KEY,
    request_number VARCHAR(20) NOT NULL UNIQUE, -- CHR-YYYYMMDD-XXXX
    agency_id VARCHAR(36), -- NULL for direct requests
    contact_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50) NOT NULL,
    company_name VARCHAR(255),
    charter_type VARCHAR(50) NOT NULL, -- SPORTS_TEAM, CORPORATE, HAJJ_UMRAH, SPECIAL_EVENT, OTHER
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    departure_date DATE NOT NULL,
    return_date DATE,
    passenger_count INT NOT NULL,
    aircraft_preference VARCHAR(50), -- A320, A321, ANY
    catering_requirements TEXT,
    special_requirements TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, QUOTED, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    quoted_amount DECIMAL(12, 2),
    quoted_currency VARCHAR(3),
    quoted_at TIMESTAMP,
    quoted_by VARCHAR(36),
    quote_valid_until TIMESTAMP,
    assigned_to VARCHAR(36),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agency_id) REFERENCES agencies(id),
    FOREIGN KEY (quoted_by) REFERENCES admin_users(id),
    FOREIGN KEY (assigned_to) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_charter_requests_number ON charter_requests(request_number);
CREATE INDEX IF NOT EXISTS idx_charter_requests_status ON charter_requests(status);
CREATE INDEX IF NOT EXISTS idx_charter_requests_type ON charter_requests(charter_type);

-- Agency bookings (bookings made through B2B portal)
CREATE TABLE IF NOT EXISTS agency_bookings (
    id VARCHAR(36) PRIMARY KEY,
    agency_id VARCHAR(36) NOT NULL,
    agent_user_id VARCHAR(36) NOT NULL,
    booking_pnr VARCHAR(6) NOT NULL,
    commission_amount DECIMAL(10, 2) NOT NULL,
    commission_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED
    payment_type VARCHAR(20) NOT NULL, -- CREDIT, INVOICE
    invoice_number VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agency_id) REFERENCES agencies(id),
    FOREIGN KEY (agent_user_id) REFERENCES agency_users(id),
    FOREIGN KEY (booking_pnr) REFERENCES bookings(pnr)
);

CREATE INDEX IF NOT EXISTS idx_agency_bookings_agency ON agency_bookings(agency_id);
CREATE INDEX IF NOT EXISTS idx_agency_bookings_pnr ON agency_bookings(booking_pnr);

-- ============================================================================
-- USER PROFILE & SAVED TRAVELERS
-- ============================================================================

-- Saved travelers (family, friends, frequent co-travelers)
CREATE TABLE IF NOT EXISTS saved_travelers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL, -- MALE, FEMALE
    nationality VARCHAR(2) NOT NULL, -- ISO 3166-1 alpha-2
    email VARCHAR(255),
    phone VARCHAR(20),
    relationship VARCHAR(50), -- SELF, SPOUSE, CHILD, PARENT, SIBLING, FRIEND, OTHER
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_saved_travelers_user_id ON saved_travelers(user_id);

-- Travel documents (passport, national ID, etc.)
CREATE TABLE IF NOT EXISTS travel_documents (
    id VARCHAR(36) PRIMARY KEY,
    traveler_id VARCHAR(36) NOT NULL,
    document_type VARCHAR(20) NOT NULL, -- PASSPORT, NATIONAL_ID, VISA
    document_number VARCHAR(50) NOT NULL,
    issuing_country VARCHAR(2) NOT NULL, -- ISO 3166-1 alpha-2
    expiry_date DATE NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (traveler_id) REFERENCES saved_travelers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_travel_documents_traveler_id ON travel_documents(traveler_id);

-- Saved payment methods
CREATE TABLE IF NOT EXISTS saved_payment_methods (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL, -- CARD, APPLE_PAY
    card_last_four VARCHAR(4),
    card_brand VARCHAR(20), -- VISA, MASTERCARD, AMEX
    card_holder_name VARCHAR(100),
    expiry_month INT,
    expiry_year INT,
    nickname VARCHAR(50),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    payment_token VARCHAR(255), -- encrypted token for recurring payments
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_saved_payment_methods_user_id ON saved_payment_methods(user_id);

-- ============================================================================
-- AI REFERENCE DATA (KOOG)
-- ============================================================================

CREATE TABLE IF NOT EXISTS airports (
    code VARCHAR(3) PRIMARY KEY,
    name_en VARCHAR(100),
    name_ar VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS airport_aliases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    airport_code VARCHAR(3) NOT NULL,
    alias VARCHAR(100) NOT NULL,
    UNIQUE(airport_code, alias),
    FOREIGN KEY (airport_code) REFERENCES airports(code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    UNIQUE(origin, destination),
    FOREIGN KEY (origin) REFERENCES airports(code),
    FOREIGN KEY (destination) REFERENCES airports(code)
);

CREATE INDEX IF NOT EXISTS idx_airport_aliases_alias ON airport_aliases(alias);

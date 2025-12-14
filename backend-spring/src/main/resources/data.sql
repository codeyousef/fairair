-- Demo users for development and testing
-- Password for all users: "password" (BCrypt hashed)
-- Hash generated with BCryptPasswordEncoder.encode("password")

MERGE INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
KEY(id)
VALUES ('user-001', 'employee@fairair.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'John', 'Smith', 'EMPLOYEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
KEY(id)
VALUES ('user-002', 'jane@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Jane', 'Doe', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
KEY(id)
VALUES ('user-003', 'admin@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin', 'User', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- SAVED TRAVELERS & PROFILE DATA FOR JANE DOE (user-002)
-- ============================================================================

-- Jane herself (primary traveler)
MERGE INTO saved_travelers (id, user_id, first_name, last_name, date_of_birth, gender, nationality, email, phone, relationship, is_primary, created_at, updated_at)
KEY(id)
VALUES ('traveler-001', 'user-002', 'Jane', 'Doe', '1985-03-15', 'FEMALE', 'SA', 'jane@test.com', '+966501234567', 'SELF', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Jane's husband
MERGE INTO saved_travelers (id, user_id, first_name, last_name, date_of_birth, gender, nationality, email, phone, relationship, is_primary, created_at, updated_at)
KEY(id)
VALUES ('traveler-002', 'user-002', 'Ahmed', 'Doe', '1983-07-22', 'MALE', 'SA', 'ahmed.doe@email.com', '+966502345678', 'SPOUSE', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Jane's daughter
MERGE INTO saved_travelers (id, user_id, first_name, last_name, date_of_birth, gender, nationality, email, phone, relationship, is_primary, created_at, updated_at)
KEY(id)
VALUES ('traveler-003', 'user-002', 'Layla', 'Doe', '2012-11-08', 'FEMALE', 'SA', NULL, NULL, 'CHILD', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Jane's son
MERGE INTO saved_travelers (id, user_id, first_name, last_name, date_of_birth, gender, nationality, email, phone, relationship, is_primary, created_at, updated_at)
KEY(id)
VALUES ('traveler-004', 'user-002', 'Omar', 'Doe', '2015-05-20', 'MALE', 'SA', NULL, NULL, 'CHILD', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- TRAVEL DOCUMENTS
-- ============================================================================

-- Jane's passport
MERGE INTO travel_documents (id, traveler_id, document_type, document_number, issuing_country, expiry_date, is_primary, created_at)
KEY(id)
VALUES ('doc-001', 'traveler-001', 'PASSPORT', 'A12345678', 'SA', '2029-03-15', TRUE, CURRENT_TIMESTAMP);

-- Jane's national ID
MERGE INTO travel_documents (id, traveler_id, document_type, document_number, issuing_country, expiry_date, is_primary, created_at)
KEY(id)
VALUES ('doc-002', 'traveler-001', 'NATIONAL_ID', '1087654321', 'SA', '2030-06-01', FALSE, CURRENT_TIMESTAMP);

-- Ahmed's passport
MERGE INTO travel_documents (id, traveler_id, document_type, document_number, issuing_country, expiry_date, is_primary, created_at)
KEY(id)
VALUES ('doc-003', 'traveler-002', 'PASSPORT', 'B98765432', 'SA', '2028-07-22', TRUE, CURRENT_TIMESTAMP);

-- Ahmed's national ID
MERGE INTO travel_documents (id, traveler_id, document_type, document_number, issuing_country, expiry_date, is_primary, created_at)
KEY(id)
VALUES ('doc-004', 'traveler-002', 'NATIONAL_ID', '1076543210', 'SA', '2029-12-15', FALSE, CURRENT_TIMESTAMP);

-- Layla's passport
MERGE INTO travel_documents (id, traveler_id, document_type, document_number, issuing_country, expiry_date, is_primary, created_at)
KEY(id)
VALUES ('doc-005', 'traveler-003', 'PASSPORT', 'C11223344', 'SA', '2027-11-08', TRUE, CURRENT_TIMESTAMP);

-- Omar's passport
MERGE INTO travel_documents (id, traveler_id, document_type, document_number, issuing_country, expiry_date, is_primary, created_at)
KEY(id)
VALUES ('doc-006', 'traveler-004', 'PASSPORT', 'D55667788', 'SA', '2027-05-20', TRUE, CURRENT_TIMESTAMP);

-- ============================================================================
-- SAVED PAYMENT METHODS
-- ============================================================================

-- Jane's primary credit card
MERGE INTO saved_payment_methods (id, user_id, type, card_last_four, card_brand, card_holder_name, expiry_month, expiry_year, nickname, is_default, payment_token, created_at)
KEY(id)
VALUES ('payment-001', 'user-002', 'CARD', '4242', 'VISA', 'Jane Doe', 12, 2027, 'Personal Visa', TRUE, 'tok_visa_personal_001', CURRENT_TIMESTAMP);

-- Jane's secondary credit card
MERGE INTO saved_payment_methods (id, user_id, type, card_last_four, card_brand, card_holder_name, expiry_month, expiry_year, nickname, is_default, payment_token, created_at)
KEY(id)
VALUES ('payment-002', 'user-002', 'CARD', '5555', 'MASTERCARD', 'Jane Doe', 6, 2026, 'Family Mastercard', FALSE, 'tok_mc_family_001', CURRENT_TIMESTAMP);

-- ============================================================================
-- AI REFERENCE DATA (KOOG)
-- ============================================================================

MERGE INTO airports (code, name_en, name_ar) KEY(code) VALUES ('RUH', 'Riyadh', 'الرياض');
MERGE INTO airports (code, name_en, name_ar) KEY(code) VALUES ('JED', 'Jeddah', 'جدة');
MERGE INTO airports (code, name_en, name_ar) KEY(code) VALUES ('DMM', 'Dammam', 'الدمام');
MERGE INTO airports (code, name_en, name_ar) KEY(code) VALUES ('DXB', 'Dubai', 'دبي');
MERGE INTO airports (code, name_en, name_ar) KEY(code) VALUES ('CAI', 'Cairo', 'القاهرة');

MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('RUH', 'riyadh');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('RUH', 'ruh');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('RUH', 'الرياض');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('RUH', 'raydh');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('RUH', 'reyaadh');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('JED', 'jed');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('JED', 'jeddah');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('JED', 'جدة');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('JED', 'jiddah');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('DMM', 'dammam');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('DMM', 'dhahran');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('DMM', 'الدمام');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('DMM', 'dmm');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('DXB', 'dubai');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('DXB', 'dxb');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('DXB', 'دبي');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('CAI', 'cairo');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('CAI', 'cai');
MERGE INTO airport_aliases (airport_code, alias) KEY(airport_code, alias) VALUES ('CAI', 'القاهرة');

MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('RUH', 'JED');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('JED', 'RUH');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('RUH', 'DMM');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('DMM', 'RUH');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('RUH', 'DXB');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('DXB', 'RUH');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('RUH', 'CAI');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('CAI', 'RUH');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('JED', 'DMM');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('DMM', 'JED');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('JED', 'CAI');
MERGE INTO routes (origin, destination) KEY(origin, destination) VALUES ('CAI', 'JED');

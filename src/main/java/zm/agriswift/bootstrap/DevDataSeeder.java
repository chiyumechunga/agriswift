package zm.agriswift.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import zm.agriswift.common.security.PiiCipher;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agriswift.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final String FARMER_CODE = "FRA-0001";

    private final JdbcTemplate jdbc;
    private final PiiCipher pii;
    private final PasswordEncoder passwordEncoder;

    @Value("${agriswift.seed.admin-password:Password123!}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("DevDataSeeder: Starting prerequisite data seeding...");

        try {
            seedStaff();
            seedFarmer();
            seedReferenceData();
            seedChartOfAccounts();
            seedPayoutInfrastructure();

            log.info("DevDataSeeder: All prerequisite data seeded successfully.");
        } catch (Exception e) {
            log.error("DevDataSeeder: Failed to seed data", e);
            throw e;
        }
    }

    /**
     * Helper to fetch exactly one row. Throws if the query returns 0 or >1 rows,
     * which helps catch schema drift or unexpected duplicate data early.
     */
    private <T> T getOne(String sql, Class<T> type, Object... args) {
        List<T> rows = jdbc.queryForList(sql, type, args);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Expected exactly one row for query: " + sql);
        }
        return rows.get(0);
    }

    private void seedStaff() {
        // 1. Ensure STAFF role exists (roles.role_name is UNIQUE)
        jdbc.update("""
            INSERT INTO roles (role_name, description)
            SELECT 'STAFF', 'Staff role'
            WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'STAFF')
            """);

        Short staffRoleId = getOne(
                "SELECT role_id FROM roles WHERE role_name = 'STAFF'", Short.class);

        // 2. Ensure admin user exists
        // Check against external_subject because it has a UNIQUE constraint in the schema.
        jdbc.update("""
            INSERT INTO users (external_subject, username, email, password_hash, is_active, created_at, updated_at)
            SELECT 'local:admin', 'admin', 'admin@agriswift.zm', ?, true, NOW(), NOW()
            WHERE NOT EXISTS (SELECT 1 FROM users WHERE external_subject = 'local:admin')
            """,
                passwordEncoder.encode(adminPassword));

        UUID userId = getOne(
                "SELECT user_id FROM users WHERE external_subject = 'local:admin'", UUID.class);

        // 3. Ensure user-role mapping exists (Composite Primary Key: user_id, role_id)
        jdbc.update("""
            INSERT INTO user_roles (user_id, role_id, granted_at)
            SELECT ?, ?, NOW()
            WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = ? AND role_id = ?)
            """,
                userId, staffRoleId, userId, staffRoleId);

        log.info("Admin user ensured: admin / {} (user_id: {})", adminPassword, userId);
    }

    private void seedFarmer() throws GeneralSecurityException {
        String nationalId = "999999/61/1";
        String mobile     = "0978602368";

        // 1. Ensure farmer exists (farmers.farmer_code is UNIQUE)
        jdbc.update("""
            INSERT INTO farmers (
                farmer_code, first_name, last_name,
                national_id_ciphertext, national_id_hash,
                mobile_number_ciphertext, mobile_number_hash,
                preferred_language, kyc_status, onboarding_channel,
                is_active, created_at, updated_at
            )
            SELECT ?, ?, ?, ?, ?, ?, ?, 'ENGLISH', 'VERIFIED', 'MANUAL', true, NOW(), NOW()
            WHERE NOT EXISTS (SELECT 1 FROM farmers WHERE farmer_code = ?)
            """,
                FARMER_CODE, "Test", "Farmer",
                pii.encrypt(nationalId), pii.generateBlindIndex(nationalId),
                pii.encrypt(mobile),    pii.generateBlindIndex(mobile),
                FARMER_CODE);

        UUID farmerId = getOne(
                "SELECT farmer_id FROM farmers WHERE farmer_code = ?", UUID.class, FARMER_CODE);

        // 2. Ensure credentials exist (Unique index on farmer_id)
        jdbc.update("""
            INSERT INTO farmer_credentials (farmer_id, pin_hash, failed_attempts, locked_until, created_at)
            SELECT ?, ?, 0, NULL, NOW()
            WHERE NOT EXISTS (SELECT 1 FROM farmer_credentials WHERE farmer_id = ?)
            """,
                farmerId, passwordEncoder.encode("1234"), farmerId);

        log.info("Test farmer ensured: {} / mobile {} / PIN 1234 (farmer_id: {})",
                FARMER_CODE, mobile, farmerId);
    }

    private void seedReferenceData() {
        // Depots (depot_code is UNIQUE)
        jdbc.update("""
            INSERT INTO depots (depot_code, name, district, province, is_active, created_at)
            SELECT 'DPT-005', 'Lusaka Central Depot', 'Lusaka', 'Lusaka', true, NOW()
            WHERE NOT EXISTS (SELECT 1 FROM depots WHERE depot_code = 'DPT-005')
            """);
        jdbc.update("""
            INSERT INTO depots (depot_code, name, district, province, is_active, created_at)
            SELECT 'DPT-003', 'Bwana Mkubwa Depot', 'Ndola', 'Copperbelt', true, NOW()
            WHERE NOT EXISTS (SELECT 1 FROM depots WHERE depot_code = 'DPT-003')
            """);

        // Crop Types (name is UNIQUE)
        jdbc.update("""
            INSERT INTO crop_types (name)
            SELECT 'White Maize' WHERE NOT EXISTS (SELECT 1 FROM crop_types WHERE name = 'White Maize')
            """);
        jdbc.update("""
            INSERT INTO crop_types (name)
            SELECT 'Paddy Rice' WHERE NOT EXISTS (SELECT 1 FROM crop_types WHERE name = 'Paddy Rice')
            """);

        // Crop Prices (Time-series logic remains unchanged)
        jdbc.update("""
            UPDATE crop_prices SET effective_to = '2026-07-28 23:59:59.999999+02'
            WHERE effective_to IS NULL AND effective_from < '2026-07-29 00:00:00+02'
            """);

        jdbc.update("""
            UPDATE crop_prices cp SET
                season = '2026', price_per_kg = v.price, max_moisture_pct = v.moisture,
                board_resolution_ref = 'FRA-BOARD-RES-2026-07',
                effective_from = '2026-07-29 00:00:00+02', effective_to = NULL
            FROM (VALUES
                ('White Maize', 6.9400::numeric, 13.50::numeric),
                ('Paddy Rice',  7.5000::numeric, 14.00::numeric))
            AS v(crop, price, moisture)
            JOIN crop_types ct ON ct.name = v.crop
            WHERE cp.crop_type_id = ct.crop_type_id AND cp.effective_to IS NULL
              AND (cp.board_resolution_ref IS NULL OR cp.board_resolution_ref <> 'FRA-BOARD-RES-2026-07')
            """);

        jdbc.update("""
            INSERT INTO crop_prices (crop_type_id, season, price_per_kg, max_moisture_pct,
                                     board_resolution_ref, effective_from)
            SELECT ct.crop_type_id, '2026', v.price, v.moisture,
                   'FRA-BOARD-RES-2026-07', '2026-07-29 00:00:00+02'
            FROM (VALUES
                ('White Maize', 6.9400::numeric, 13.50::numeric),
                ('Paddy Rice',  7.5000::numeric, 14.00::numeric))
            AS v(crop, price, moisture)
            JOIN crop_types ct ON ct.name = v.crop
            WHERE NOT EXISTS (
                SELECT 1 FROM crop_prices cp
                WHERE cp.crop_type_id = ct.crop_type_id
                  AND cp.board_resolution_ref = 'FRA-BOARD-RES-2026-07')
            """);

        log.info("Reference data ensured.");
    }

    private void seedChartOfAccounts() {
        // account_code is PRIMARY KEY
        jdbc.update("""
            INSERT INTO chart_of_accounts (account_code, account_name, account_category)
            SELECT 'FARMER_PAYABLE', 'Farmer Payable', 'LIABILITY'
            WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_code = 'FARMER_PAYABLE')
            """);
        jdbc.update("""
            INSERT INTO chart_of_accounts (account_code, account_name, account_category)
            SELECT 'BANK_SETTLEMENT', 'Bank Settlement Clearing', 'ASSET'
            WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_code = 'BANK_SETTLEMENT')
            """);
        jdbc.update("""
            INSERT INTO chart_of_accounts (account_code, account_name, account_category)
            SELECT 'TREASURY_CASH', 'Treasury Cash', 'ASSET'
            WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_code = 'TREASURY_CASH')
            """);
    }

    private void seedPayoutInfrastructure() throws GeneralSecurityException {
        // 1. Ensure Payment Provider exists (provider_code is UNIQUE)
        jdbc.update("""
            INSERT INTO payment_providers (provider_type, provider_name, provider_code, is_active, health_status)
            SELECT 'MOBILE_MONEY', 'Airtel Money', 'AIRTEL', true, 'HEALTHY'
            WHERE NOT EXISTS (SELECT 1 FROM payment_providers WHERE provider_code = 'AIRTEL')
            """);

        Short providerId = getOne(
                "SELECT provider_id FROM payment_providers WHERE provider_code = 'AIRTEL'", Short.class);

        UUID farmerId = getOne(
                "SELECT farmer_id FROM farmers WHERE farmer_code = ?", UUID.class, FARMER_CODE);

        String msisdn = "0978602368";

        // 2. Ensure Preferred Payment Account exists
        // Note: The schema has a partial unique index: idx_one_preferred_account_per_farmer (farmer_id) WHERE is_preferred
        jdbc.update("""
            INSERT INTO farmer_payment_accounts (
                farmer_id, provider_id, account_type, account_owner_type,
                account_number_ciphertext, account_number_hash,
                is_preferred, is_verified, is_verified_by_zechl, is_active, created_at
            )
            SELECT ?, ?, 'MOBILE_MONEY', 'FARMER', ?, ?, true, true, true, true, NOW()
            WHERE NOT EXISTS (
                SELECT 1 FROM farmer_payment_accounts 
                WHERE farmer_id = ? AND is_preferred = true
            )
            """,
                farmerId, providerId,
                pii.encrypt(msisdn), pii.generateBlindIndex(msisdn),
                farmerId);

        log.info("Preferred MOBILE_MONEY account ensured for farmer {} ({}).", FARMER_CODE, farmerId);
    }
}
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
import java.util.UUID;

/**
 * Seeds prerequisite data for development/testing.
 * Gated by agriswift.seed.enabled=true property.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agriswift.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final UUID FARMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
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

    private void seedStaff() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class, "admin");

        if (count != null && count > 0) {
            log.debug("Admin user already exists, skipping.");
            return;
        }

        Integer roleId = jdbc.queryForObject(
                "SELECT role_id FROM roles WHERE role_name = 'ROLE_STAFF'",
                Integer.class);

        if (roleId == null) {
            jdbc.update("INSERT INTO roles (role_name, description) VALUES ('ROLE_STAFF', 'Staff role')");
            roleId = jdbc.queryForObject(
                    "SELECT role_id FROM roles WHERE role_name = 'ROLE_STAFF'",
                    Integer.class);
        }

        UUID userId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO users (user_id, external_subject, username, password_hash, is_active, created_at)
            VALUES (?, ?, ?, ?, true, NOW())
            """,
                userId, "local-admin", "admin", passwordEncoder.encode(adminPassword));

        jdbc.update("""
            INSERT INTO user_roles (user_id, role_id, granted_at)
            VALUES (?, ?, NOW())
            """,
                userId, roleId);

        log.info("Created admin user: admin / {}", adminPassword);
    }

    private void seedFarmer() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM farmers WHERE farmer_id = ?",
                Integer.class, FARMER_ID);

        if (count != null && count > 0) {
            log.debug("Test farmer already exists, skipping.");
            return;
        }

        String nationalId = "999999/61/1";
        String mobile = "0978602368";

        try {
            jdbc.update("""
                INSERT INTO farmers (
                    farmer_id, farmer_code, first_name, last_name, 
                    national_id_ciphertext, national_id_hash,
                    mobile_number_ciphertext, mobile_number_hash,
                    preferred_language, kyc_status, onboarding_channel,
                    is_active, created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, 
                    ?, ?,
                    ?, ?,
                    'ENGLISH', 'VERIFIED', 'MANUAL',
                    true, NOW(), NOW()
                )
                """,
                    FARMER_ID, FARMER_CODE, "Test", "Farmer",
                    pii.encrypt(nationalId), pii.generateBlindIndex(nationalId),
                    pii.encrypt(mobile), pii.generateBlindIndex(mobile));

            log.info("Created test farmer: {} ({})", FARMER_CODE, FARMER_ID);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to encrypt farmer PII", e);
        }
    }

    private void seedReferenceData() {
        log.info("Seeding reference data...");

        // Depots (Lusaka is DPT-005)
        jdbc.update("""
            INSERT INTO depots (depot_code, name, district, province, is_active, created_at)
            VALUES
              ('DPT-005', 'Lusaka Central Depot', 'Lusaka', 'Lusaka', true, NOW()),
              ('DPT-003', 'Bwana Mkubwa Depot', 'Ndola', 'Copperbelt', true, NOW())
            ON CONFLICT (depot_code) DO NOTHING
            """);

        // Crop types
        jdbc.update("""
            INSERT INTO crop_types (name) VALUES
              ('White Maize'),
              ('Paddy Rice')
            ON CONFLICT (name) DO NOTHING
            """);

        // 1. Close historical open-ended prices that PREDATE the FRA 2026 season.
        jdbc.update("""
            UPDATE crop_prices
            SET effective_to = '2026-07-28 23:59:59.999999+02'
            WHERE effective_to IS NULL
              AND effective_from < '2026-07-29 00:00:00+02'
            """);

        // 2. Correct any lingering open-ended PLACEHOLDER for a FRA crop into the
        //    official 2026 row IN PLACE. (Paddy Rice corrected to 7.50/kg per FRA announcement).
        jdbc.update("""
            UPDATE crop_prices cp
            SET season               = '2026',
                price_per_kg         = v.price,
                max_moisture_pct     = v.moisture,
                board_resolution_ref = 'FRA-BOARD-RES-2026-07',
                effective_from       = '2026-07-29 00:00:00+02',
                effective_to         = NULL
            FROM (VALUES
                    ('White Maize', 6.9400::numeric, 13.50::numeric),
                    ('Paddy Rice',  7.5000::numeric, 14.00::numeric)
                 ) AS v(crop, price, moisture)
            JOIN crop_types ct ON ct.name = v.crop
            WHERE cp.crop_type_id = ct.crop_type_id
              AND cp.effective_to IS NULL
              AND (cp.board_resolution_ref IS NULL
                   OR cp.board_resolution_ref <> 'FRA-BOARD-RES-2026-07')
            """);

        // 3. Insert any official 2026 price still missing (crop had no placeholder).
        jdbc.update("""
            INSERT INTO crop_prices (crop_type_id, season, price_per_kg, max_moisture_pct,
                                     board_resolution_ref, effective_from)
            SELECT ct.crop_type_id, '2026', v.price, v.moisture,
                   'FRA-BOARD-RES-2026-07', '2026-07-29 00:00:00+02'
            FROM (VALUES
                    ('White Maize', 6.9400::numeric, 13.50::numeric),
                    ('Paddy Rice',  7.5000::numeric, 14.00::numeric)
                 ) AS v(crop, price, moisture)
            JOIN crop_types ct ON ct.name = v.crop
            WHERE NOT EXISTS (
                SELECT 1 FROM crop_prices cp
                WHERE cp.crop_type_id = ct.crop_type_id
                  AND cp.board_resolution_ref = 'FRA-BOARD-RES-2026-07'
            )
            """);

        log.info("Reference data seeded: depots DPT-005 (Lusaka), DPT-003; FRA 2026 prices active "
                + "(White Maize K6.94/kg, Paddy Rice K7.50/kg from 2026-07-29).");
    }

    private void seedChartOfAccounts() {
        log.info("Seeding chart of accounts...");

        jdbc.update("""
            INSERT INTO chart_of_accounts (account_code, account_name, account_category) VALUES
              ('FARMER_PAYABLE',  'Farmer Payable',           'LIABILITY'),
              ('BANK_SETTLEMENT', 'Bank Settlement Clearing', 'ASSET'),
              ('TREASURY_CASH',   'Treasury Cash',            'ASSET')
            ON CONFLICT (account_code) DO NOTHING
            """);

        log.info("Chart of accounts seeded: FARMER_PAYABLE, BANK_SETTLEMENT, TREASURY_CASH");
    }

    private void seedPayoutInfrastructure() throws GeneralSecurityException {
        log.info("Seeding payout infrastructure...");

        jdbc.update("""
            INSERT INTO payment_providers (provider_type, provider_name, provider_code, is_active, health_status)
            VALUES ('MOBILE_MONEY', 'Airtel Money', 'AIRTEL', true, 'HEALTHY')
            ON CONFLICT (provider_code) DO NOTHING
            """);

        Integer accountCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM farmer_payment_accounts WHERE farmer_id = ? AND is_preferred = true",
                Integer.class, FARMER_ID);

        if (accountCount != null && accountCount > 0) {
            log.debug("Farmer already has a preferred payout account, skipping.");
            return;
        }

        Short providerId = jdbc.queryForObject(
                "SELECT provider_id FROM payment_providers WHERE provider_code = 'AIRTEL'",
                Short.class);

        if (providerId == null) {
            log.error("Airtel provider not found in database!");
            return;
        }

        String msisdn = "0978602368";

        try {
            jdbc.update("""
        INSERT INTO farmer_payment_accounts
          (farmer_id, provider_id, account_type, account_owner_type,
           account_number_ciphertext, account_number_hash,
           is_preferred, is_verified, is_verified_by_zechl, is_active, created_at)
        VALUES (?, ?, 'MOBILE_MONEY', 'FARMER', ?, ?,
                true, true, true, true, NOW())
        """,
                    FARMER_ID, providerId,
                    pii.encrypt(msisdn), pii.generateBlindIndex(msisdn));

            log.info("Created preferred MOBILE_MONEY account for farmer {}", FARMER_ID);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to encrypt payment account PII", e);
        }
    }
}
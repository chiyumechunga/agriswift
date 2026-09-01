package zm.agriswift.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import zm.agriswift.farmer.internal.PiiCipher;

import java.security.GeneralSecurityException;
import java.util.UUID;

/**
 * Dev-only bootstrap data so the auth endpoints can be exercised.
 * Enable with: agriswift.seed.enabled=true
 */
@Component
@ConditionalOnProperty(name = "agriswift.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    static final UUID STAFF_ID  = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID FARMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final PiiCipher pii;

    public DevDataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder, PiiCipher pii) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.pii = pii;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedStaff();
        seedFarmer();
    }

    private void seedStaff() {
        if (jdbc.queryForObject("SELECT count(*) FROM users WHERE username = 'admin'", Integer.class) > 0) return;

        jdbc.update("INSERT INTO roles (role_name, description) VALUES ('ADMIN', 'System administrator') " +
                "ON CONFLICT (role_name) DO NOTHING");
        short roleId = jdbc.queryForObject("SELECT role_id FROM roles WHERE role_name = 'ADMIN'", Short.class);

        // password_hash encoded with the SAME Argon2 encoder the DaoAuthenticationProvider uses
        jdbc.update("INSERT INTO users (user_id, external_subject, username, email, password_hash) " +
                        "VALUES (?, 'local:admin', 'admin', 'admin@agriswift.zm', ?)",
                STAFF_ID, encoder.encode("Password123!"));

        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                STAFF_ID, roleId);
    }

    private void seedFarmer() throws GeneralSecurityException {
        if (jdbc.queryForObject("SELECT count(*) FROM farmers WHERE farmer_id = ?", Integer.class, FARMER_ID) > 0) return;

        String nrc    = "123456/78/9";
        String mobile = "0977123456";

        // PII encrypted + blind-indexed with the SAME PiiCipher the FarmerDirectory uses for lookups
        jdbc.update("""
                INSERT INTO farmers
                  (farmer_id, farmer_code, first_name, last_name,
                   national_id_ciphertext, national_id_hash,
                   mobile_number_ciphertext, mobile_number_hash,
                   email, kyc_status, onboarding_channel)
                VALUES (?, 'FRA-0001', 'Test', 'Farmer', ?, ?, ?, ?,
                        'farmer@example.com', 'VERIFIED', 'SELF_REGISTRATION')
                """,
                FARMER_ID,
                pii.encrypt(nrc),    pii.generateBlindIndex(nrc),
                pii.encrypt(mobile), pii.generateBlindIndex(mobile));

        // PIN encoded with the SAME Argon2 encoder FarmerAuthService uses for matches()
        jdbc.update("INSERT INTO farmer_credentials (farmer_id, pin_hash) VALUES (?, ?)",
                FARMER_ID, encoder.encode("1234"));
    }
}
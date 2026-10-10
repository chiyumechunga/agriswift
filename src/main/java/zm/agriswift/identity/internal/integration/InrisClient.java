package zm.agriswift.identity.internal.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Slf4j
public class InrisClient {

    public Optional<InrisProfile> fetchProfile(String nrc) {
        // TODO: Replace with actual HTTP POST to ZAMIS/INRIS API
        log.info("Mocking INRIS e-KYC fetch for NRC: {}", nrc);

        if (nrc.equals("000000/00/0")) return Optional.empty();

        // Mocked response from Government DB
        return Optional.of(new InrisProfile(
                "Chiyume", "M", "Chunga",
                LocalDate.of(1995, 5, 14),
                "Southern", "Monze"
        ));
    }
}
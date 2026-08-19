package zm.agriswift.systemconfig;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemConfigService {

    private final SystemConfigRepository repository;

    public SystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    public Optional<String> get(String key) {
        return repository.findById(key).map(SystemConfigEntry::getConfigValue);
    }
}

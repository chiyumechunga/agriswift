package zm.agriswift.identity.internal.service;

import org.springframework.stereotype.Component;
import zm.agriswift.identity.api.UserDirectory;
import zm.agriswift.identity.internal.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserDirectoryImpl implements UserDirectory {

    private final UserRepository userRepository;

    UserDirectoryImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSummary> findById(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> new UserSummary(u.getUserId(), u.getUsername(), u.isActive()));
    }
}
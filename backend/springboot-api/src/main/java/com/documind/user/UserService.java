package com.documind.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.documind.common.ResourceNotFoundException;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(getEntity(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getEntity(userId);
        user.setFullName(request.fullName().trim());
        return UserResponse.from(user);
    }

    private User getEntity(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}

package com.bigbaldy.poker.resource;

import com.bigbaldy.poker.model.User;
import lombok.Data;

@Data
public class UserLoginResponseResource {
    private Long userId;
    private String accessToken;

    public UserLoginResponseResource(User user, String accessToken) {
        this.userId = user.getId();
        this.accessToken = accessToken;
    }
}

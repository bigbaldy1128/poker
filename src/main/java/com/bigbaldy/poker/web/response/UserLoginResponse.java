package com.bigbaldy.poker.web.response;

import com.bigbaldy.poker.model.User;
import lombok.Data;

@Data
public class UserLoginResponse {
    private Long userId;
    private String accessToken;

    public UserLoginResponse(User user, String accessToken) {
        this.userId = user.getId();
        this.accessToken = accessToken;
    }
}

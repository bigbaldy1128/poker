package com.bigbaldy.poker.web.v1;

import com.bigbaldy.poker.config.SecurityConfiguration;
import com.bigbaldy.poker.model.User;
import com.bigbaldy.poker.service.IUserService;
import com.bigbaldy.poker.util.JWTTokenUtil;
import com.bigbaldy.poker.resource.UserLoginRequestResource;
import com.bigbaldy.poker.resource.ResponseResource;
import com.bigbaldy.poker.resource.UserLoginResponseResource;
import org.springframework.web.bind.annotation.*;

import static com.bigbaldy.poker.constant.constant.CURRENT_USER_ATTRIBUTE;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final SecurityConfiguration securityConfiguration;
    private final IUserService userService;

    private static final long ACCESS_TOKEN_TTL_IN_SECONDS = Integer.MAX_VALUE;
    private static final long ACCESS_REFRESH_TOKEN_TTL_IN_SECONDS = Integer.MAX_VALUE;

    public UserController(SecurityConfiguration securityConfiguration,
                          IUserService userService) {
        this.securityConfiguration = securityConfiguration;
        this.userService = userService;
    }

    @PostMapping("login")
    public ResponseResource<UserLoginResponseResource> login(@RequestBody UserLoginRequestResource userLoginRequestResource) {
        User user = userService.getOrCreate(userLoginRequestResource.getToken());
        JWTTokenUtil.JWTToken jwtToken =
                JWTTokenUtil.getJWTToken(
                        user.getId(),
                        securityConfiguration.getAuth().getJwtSecret(),
                        ACCESS_TOKEN_TTL_IN_SECONDS,
                        ACCESS_REFRESH_TOKEN_TTL_IN_SECONDS);

        user.setAccessTokenSignature(JWTTokenUtil.getAccessTokenSignature(jwtToken.getAccessToken()));
        userService.saveToDb(user);

        UserLoginResponseResource ret = new UserLoginResponseResource(user, jwtToken.getAccessToken());
        return ResponseResource.create(ret);
    }

    @GetMapping("/test")
    public ResponseResource<User> test(@RequestAttribute(CURRENT_USER_ATTRIBUTE) User currentUser) {
        return ResponseResource.create(currentUser);
    }
}

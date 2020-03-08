package com.bigbaldy.poker.web.v1;

import com.bigbaldy.poker.config.SecurityConfiguration;
import com.bigbaldy.poker.model.User;
import com.bigbaldy.poker.service.IUserService;
import com.bigbaldy.poker.util.JWTTokenUtil;
import com.bigbaldy.poker.web.request.UserLoginRequest;
import com.bigbaldy.poker.web.response.ResultVO;
import com.bigbaldy.poker.web.response.UserLoginResponse;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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
    public ResultVO<UserLoginResponse> login(@RequestBody UserLoginRequest userLoginRequest) {
        User user = userService.getOrCreate(userLoginRequest.getToken());
        JWTTokenUtil.JWTToken jwtToken =
                JWTTokenUtil.getJWTToken(
                        user.getId(),
                        securityConfiguration.getAuth().getJwtSecret(),
                        ACCESS_TOKEN_TTL_IN_SECONDS,
                        ACCESS_REFRESH_TOKEN_TTL_IN_SECONDS);

        user.setAccessTokenSignature(JWTTokenUtil.getAccessTokenSignature(jwtToken.getAccessToken()));
        userService.saveToDb(user);

        UserLoginResponse ret = new UserLoginResponse(user, jwtToken.getAccessToken());
        return ResultVO.create(ret);
    }

    @GetMapping("/test")
    public ResultVO<User> test(@RequestAttribute(CURRENT_USER_ATTRIBUTE) User currentUser) {
        return ResultVO.create(currentUser);
    }
}

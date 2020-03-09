package com.bigbaldy.poker.web.v1;

import com.bigbaldy.poker.helper.UserHelper;
import com.bigbaldy.poker.model.User;
import com.bigbaldy.poker.resource.UserLoginRequestResource;
import com.bigbaldy.poker.resource.ResponseResource;
import com.bigbaldy.poker.resource.UserLoginResponseResource;
import org.springframework.web.bind.annotation.*;

import static com.bigbaldy.poker.constant.constant.CURRENT_USER_ATTRIBUTE;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserHelper userHelper;

    public UserController(UserHelper userHelper) {
        this.userHelper = userHelper;
    }

    @PostMapping("login")
    public ResponseResource<UserLoginResponseResource> login(@RequestBody UserLoginRequestResource userLoginRequestResource) {
        return userHelper.login(userLoginRequestResource);
    }

    @GetMapping("/test")
    public ResponseResource<User> test(@RequestAttribute(CURRENT_USER_ATTRIBUTE) User currentUser) {
        return ResponseResource.create(currentUser);
    }
}

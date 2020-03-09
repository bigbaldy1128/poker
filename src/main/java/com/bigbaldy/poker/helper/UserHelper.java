package com.bigbaldy.poker.helper;

import com.bigbaldy.poker.config.SecurityConfiguration;
import com.bigbaldy.poker.exception.UserException;
import com.bigbaldy.poker.lib.AbstractFactory;
import com.bigbaldy.poker.lib.thirdpartyuser.IThirdPartyUserGetter;
import com.bigbaldy.poker.lib.thirdpartyuser.ThirdPartyUserGetterFactory;
import com.bigbaldy.poker.model.ThirdPartyUser;
import com.bigbaldy.poker.model.User;
import com.bigbaldy.poker.model.type.ThirdPartyUserType;
import com.bigbaldy.poker.resource.ResponseResource;
import com.bigbaldy.poker.resource.UserLoginRequestResource;
import com.bigbaldy.poker.resource.UserLoginResponseResource;
import com.bigbaldy.poker.service.IThirdPartyUserService;
import com.bigbaldy.poker.service.IUserService;
import com.bigbaldy.poker.util.JWTTokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author wangjinzhao on 2020/3/9
 */
@Component
public class UserHelper {
    private final IUserService userService;
    private final IThirdPartyUserService thirdPartyUserService;
    private final SecurityConfiguration securityConfiguration;

    private static final long ACCESS_TOKEN_TTL_IN_SECONDS = Integer.MAX_VALUE;
    private static final long ACCESS_REFRESH_TOKEN_TTL_IN_SECONDS = Integer.MAX_VALUE;

    public UserHelper(IUserService userService,
                      IThirdPartyUserService thirdPartyUserService,
                      SecurityConfiguration securityConfiguration) {
        this.userService = userService;
        this.thirdPartyUserService = thirdPartyUserService;
        this.securityConfiguration = securityConfiguration;
    }

    @Transactional
    public ResponseResource<UserLoginResponseResource> login(UserLoginRequestResource userLoginRequestResource) {
        ThirdPartyUserType thirdPartyUserType = userLoginRequestResource.getThirdPartyUserType();
        IThirdPartyUserGetter thirdPartyUserGetter = AbstractFactory.getInstance(ThirdPartyUserGetterFactory.class).get(thirdPartyUserType);
        String thirdPartyId = thirdPartyUserGetter.getUserId(userLoginRequestResource.getToken());
        User user;
        Optional<ThirdPartyUser> thirdPartyUserOptional = thirdPartyUserService.getByThirdPartyIdAndThirdPartyType(thirdPartyId, thirdPartyUserType);
        if (!thirdPartyUserOptional.isPresent()) {
            ThirdPartyUser thirdPartyUser = new ThirdPartyUser();
            thirdPartyUser.setThirdPartyUserId(thirdPartyId);
            thirdPartyUser.setThirdPartyUserType(thirdPartyUserType);
            user = userService.create();
            thirdPartyUser.setUserId(user.getId());
            thirdPartyUserService.saveToDb(thirdPartyUser);
        } else {
            ThirdPartyUser thirdPartyUser = thirdPartyUserOptional.get();
            Optional<User> userOptional = userService.get(thirdPartyUser.getUserId());
            if (!userOptional.isPresent()) {
                throw new UserException(UserException.UserErrorInfo.NOT_EXISTS);
            }
            user = userOptional.get();
        }

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
}

package com.bigbaldy.poker.filter;

import com.bigbaldy.poker.config.SecurityConfiguration;
import com.bigbaldy.poker.exception.AuthException;
import com.bigbaldy.poker.exception.UserException;
import com.bigbaldy.poker.model.User;
import com.bigbaldy.poker.service.IUserService;
import com.bigbaldy.poker.util.JWTTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Optional;

import static com.bigbaldy.poker.constant.constant.EXCEPTION_ATTRIBUTE;
import static com.bigbaldy.poker.constant.constant.CURRENT_USER_ATTRIBUTE;

@Slf4j
public class ClientAuthorizationFilter extends AbstractAuthorizationFilter {

    private IUserService userService;
    private SecurityConfiguration securityConfiguration;

    public ClientAuthorizationFilter(IUserService userService, SecurityConfiguration securityConfiguration) {
        this.userService = userService;
        this.securityConfiguration = securityConfiguration;
    }

    @Override
    protected UsernamePasswordAuthenticationToken authenticate(HttpServletRequest request) {
        String authorization = request.getHeader(AbstractAuthorizationFilter.AUTHORIZATION_HEADER);
        if (!StringUtils.isEmpty(authorization)) {
            Long userId;
            try {
                userId = (long) (int) JWTTokenUtil
                        .getClaimFromAuthorization(authorization, "user_id", securityConfiguration.getAuth().getJwtSecret());
                Optional<User> userOptional = userService.getFromDb(userId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    if (JWTTokenUtil.getAccessTokenSignature(JWTTokenUtil.getTokenString(authorization))
                            .equals(user.getAccessTokenSignature())) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                new ArrayList<>());
                        request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
                        return authentication;
                    } else {
                        request.setAttribute(EXCEPTION_ATTRIBUTE, new AuthException(AuthException.AuthErrorInfo.AUTH_FAILURE));
                    }
                } else {
                    request.setAttribute(EXCEPTION_ATTRIBUTE, new UserException(UserException.UserErrorInfo.NOT_EXISTS));
                }
            } catch (Exception ex) {
                log.error("Authorization failure", ex);
                request.setAttribute(EXCEPTION_ATTRIBUTE, new AuthException(AuthException.AuthErrorInfo.AUTH_FAILURE));
            }
        } else {
            request.setAttribute(EXCEPTION_ATTRIBUTE, new AuthException(AuthException.AuthErrorInfo.NO_AUTH_HEADER));
        }
        return null;
    }
}

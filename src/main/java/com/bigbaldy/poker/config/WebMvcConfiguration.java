package com.bigbaldy.poker.config;

import com.bigbaldy.poker.exception.AbstractUncheckedException;
import com.bigbaldy.poker.exception.AuthException;
import com.bigbaldy.poker.filter.ClientAuthorizationFilter;
import com.bigbaldy.poker.resource.ResponseResource;
import com.bigbaldy.poker.service.IUserService;
import com.bigbaldy.poker.util.JsonUtil;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.bigbaldy.poker.constant.constant.EXCEPTION_ATTRIBUTE;

@Configuration
@Order(1)
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Configuration
    @Order(2)
    @ConfigurationProperties(prefix = "poker.web")
    public static class ClientApiSecurityConfiguration extends WebSecurityConfigurerAdapter {
        private final IUserService userService;
        private final SecurityConfiguration securityConfiguration;
        @Setter
        private String[] publicAPIs;

        public ClientApiSecurityConfiguration(IUserService userService,
                                              SecurityConfiguration securityConfiguration) {
            this.userService = userService;
            this.securityConfiguration = securityConfiguration;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.cors()
                    .and()
                    .csrf()
                    .disable()
                    .antMatcher("/api/**")
                    .addFilterBefore(
                            new ClientAuthorizationFilter(userService, securityConfiguration),
                            UsernamePasswordAuthenticationFilter.class)
                    .authorizeRequests()
                    .antMatchers(publicAPIs)
                    .permitAll()
                    .and()
                    .authorizeRequests()
                    .antMatchers("/api/**")
                    .authenticated()
                    .and()
                    .exceptionHandling()
                    .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }
    }

    private static class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

        @Override
        public void commence(
                HttpServletRequest req, HttpServletResponse res, AuthenticationException authException)
                throws IOException {

            AbstractUncheckedException exception = (AbstractUncheckedException) req.getAttribute(EXCEPTION_ATTRIBUTE);
            if (exception == null) {
                exception = new AuthException(AuthException.AuthErrorInfo.NO_AUTH_HEADER);
            }
            ResponseResource result = exception.toResult();
            res.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            res.setStatus(HttpStatus.OK.value());
            res.getWriter().write(JsonUtil.toJson(result).get());
        }
    }
}

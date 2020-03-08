package com.bigbaldy.poker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "secret")
public class SecurityConfiguration {
    private SecurityConfiguration.Auth auth;

    public SecurityConfiguration() {
    }

    public SecurityConfiguration.Auth getAuth() {
        return this.auth;
    }

    public void setAuth(final SecurityConfiguration.Auth auth) {
        this.auth = auth;
    }

    public static class Auth {
        private String jwtSecret;
        private String internalSecretToken;

        public Auth() {
        }

        public String getJwtSecret() {
            return this.jwtSecret;
        }

        public String getInternalSecretToken() {
            return this.internalSecretToken;
        }

        public void setJwtSecret(final String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public void setInternalSecretToken(final String internalSecretToken) {
            this.internalSecretToken = internalSecretToken;
        }
    }
}

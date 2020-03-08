package com.bigbaldy.poker.filter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractAuthorizationFilter extends OncePerRequestFilter {

  public static final String AUTHORIZATION_HEADER = "Authorization";

  @Override
  protected void doFilterInternal(
          HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String authorization = req.getHeader(AUTHORIZATION_HEADER);

    if (authorization == null) {
      chain.doFilter(req, res);
      return;
    }

    UsernamePasswordAuthenticationToken authentication = authenticate(req);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    chain.doFilter(req, res);
  }

  protected abstract UsernamePasswordAuthenticationToken authenticate(HttpServletRequest req);
}

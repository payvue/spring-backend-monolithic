package com.payvue.app.infrastructure.security;

import com.payvue.app.domain.security.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.payvue.app.domain.security.SessionTokenRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.dao.DataAccessException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final TokenProvider jwtProvider;
  private final UserDetailsService userDetailsService;
  private final SessionTokenRepository sessionTokenRepository;
  private final TokenHeaderResolver tokenHeaderResolver;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String token = tokenHeaderResolver.resolve(request);
    if (token != null && jwtProvider.validate(token) && sessionTokenRepository.isActive(token)) {
      String email = jwtProvider.getSubject(token);
      UserDetails userDetails;
      try {
        userDetails = userDetailsService.loadUserByUsername(email);
      } catch (UsernameNotFoundException | DataAccessException ex) {
        userDetails = org.springframework.security.core.userdetails.User
            .withUsername(email)
            .password("")
            .authorities(java.util.List.of())
            .build();
      }
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              userDetails.getAuthorities()
          );
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }
}

package com.payvue.app.infrastructure.security;

import java.util.Collection;
import java.util.List;

import com.payvue.supporting.user.command.PasswordPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@RequiredArgsConstructor
class AuthUserDetails implements UserDetails {

  private final PasswordPrincipal authenticatedUser;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return authenticatedUser.passwordHash().value();
  }

  @Override
  public String getUsername() {
    return authenticatedUser.email();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}

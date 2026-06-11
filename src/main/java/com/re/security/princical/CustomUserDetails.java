package com.re.security.princical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
@Builder
public class CustomUserDetails implements UserDetails {

    private Long id;

    private String username;

    private String password;

    private Boolean isActive;

    private Collection<? extends GrantedAuthority> authorities;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * Tài khoản có hết hạn hay không (Mặc định không hết hạn)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Mật khẩu có hết hạn hay không (Mặc định không hết hạn)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Quyết định xem người dùng có được đăng nhập hay không
     */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
}
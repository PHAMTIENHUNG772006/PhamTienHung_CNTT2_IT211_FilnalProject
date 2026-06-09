package com.re.security.princical;

import com.re.model.entity.enums.UserStatus;
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

    private Collection<? extends GrantedAuthority> authorities;

    private UserStatus status;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Tài khoản có hết hạn hay không
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Tài khoản có bị khóa hay không
     */
    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    /**
     * Mật khẩu có hết hạn hay không
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Tài khoản có đang hoạt động hay không
     */
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
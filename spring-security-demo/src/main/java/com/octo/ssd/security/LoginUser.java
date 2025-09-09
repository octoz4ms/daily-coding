package com.octo.ssd.security;

import com.octo.ssd.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser implements UserDetails {
    @Serial
    private static final long serialVersionUID = 6624878247090935917L;

    // 业务用户信息
    private User user;

    // 权限信息，反序列化可以动态生成
    private List<SimpleGrantedAuthority> authorities;

    public LoginUser(final User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities != null) {
            return authorities;
        }
        // 反序列化后再手动还原
        return createAuthorities(user.getRoles(), user.getPermissions());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
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

    /**
     * 根据用户角色和权限创建Spring Security所需的权限集合
     *
     * @param roles       用户角色列表
     * @param permissions 用户权限列表
     * @return 包含角色和权限的SimpleGrantedAuthority集合
     */
    public static List<SimpleGrantedAuthority> createAuthorities(
            List<String> roles, List<String> permissions) {
        return Stream.concat(
                        roles == null ? Stream.empty() : roles.stream()
                                .filter(Objects::nonNull)
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
                        permissions == null ? Stream.empty() : permissions.stream()
                                .filter(Objects::nonNull)
                                .map(SimpleGrantedAuthority::new)
                )
                .distinct() // 去重，避免重复权限
                .collect(Collectors.toList());
    }
}

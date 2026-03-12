package com.payvue.app.domain.community;

import java.util.Set;

import com.payvue.core.community.RoleDescriptor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Role - 커뮤니티 내 역할 값 객체 (애플리케이션 전용)
 */
@Getter
@EqualsAndHashCode
public class Role implements RoleDescriptor {

    public static final Role ADMIN = new Role("ADMIN", "관리자", Set.of(
            Permission.ALL
    ));

    public static final Role MEMBER = new Role("MEMBER", "멤버", Set.of(
        Permission.READ,
        Permission.REQUEST
    ));

    public static final Role GUEST = new Role("GUEST", "게스트", Set.of(
        Permission.READ
    ));

    private final String name;
    private final String displayName;
    private final Set<Permission> permissions;

    private Role(String name, String displayName, Set<Permission> permissions) {
        this.name = name;
        this.displayName = displayName;
        this.permissions = Set.copyOf(permissions);
    }

    public static Role of(String name, String displayName, Set<Permission> permissions) {
        return new Role(name, displayName, permissions);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(Permission.ALL) || permissions.contains(permission);
    }

    @Override
    public boolean isAdmin() {
        return "ADMIN".equals(name);
    }

    public enum Permission {
        ALL,
        READ,
        REQUEST,
        APPLY
    }
}

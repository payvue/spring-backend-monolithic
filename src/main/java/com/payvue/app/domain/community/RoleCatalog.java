package com.payvue.app.domain.community;

import com.payvue.core.community.RoleDescriptor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public interface RoleCatalog {

  RoleDescriptor admin();

  RoleDescriptor member();

  RoleDescriptor guest();

  RoleDescriptor resolve(String name);

  static Builder builder() {
    return new Builder();
  }

  final class Builder {
    private final Map<String, RoleDescriptor> registry = new LinkedHashMap<>();

    public Builder register(RoleDescriptor role) {
      Objects.requireNonNull(role, "role is required");
      registry.put(role.getName().toUpperCase(Locale.ROOT), role);
      return this;
    }

    public RoleCatalog build() {
      return new RegistryRoleCatalog(registry);
    }
  }

  final class RegistryRoleCatalog implements RoleCatalog {
    private final Map<String, RoleDescriptor> registry;

    RegistryRoleCatalog(Map<String, RoleDescriptor> registry) {
      this.registry = Map.copyOf(registry);
    }

    @Override
    public RoleDescriptor admin() {
      return resolve("ADMIN");
    }

    @Override
    public RoleDescriptor member() {
      return resolve("MEMBER");
    }

    @Override
    public RoleDescriptor guest() {
      return resolve("GUEST");
    }

    @Override
    public RoleDescriptor resolve(String name) {
      if (name == null || name.isBlank()) {
        return registry.get("GUEST");
      }
      RoleDescriptor role = registry.get(name.toUpperCase(Locale.ROOT));
      if (role != null) {
        return role;
      }
      return registry.get("GUEST");
    }
  }
}

package com.payvue.app.infrastructure.persistence.assetholding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.characteristic.AssetCharacteristic;
import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.HasBalance;
import com.payvue.characteristic.Ownable;
import com.payvue.shared.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class AssetCharacteristicMapper {

  private final ObjectMapper objectMapper;

  public AssetCharacteristicMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AssetItemCapabilityTBL toEntity(AssetItemTBL assetItem, Set<AssetCharacteristic> capabilities) {
    if (assetItem == null || capabilities == null || capabilities.isEmpty()) {
      return null;
    }
    String configJson = writeConfig(capabilities);
    return AssetItemCapabilityTBL.create(assetItem, configJson);
  }

  public Set<AssetCharacteristic> toDomain(AssetItemCapabilityTBL entity) {
    if (entity == null) {
      return Set.of();
    }
    Map<String, Object> config = readConfig(entity.getConfigJson());
    Set<AssetCharacteristic> capabilities = new LinkedHashSet<>();
    for (Map.Entry<String, Object> entry : config.entrySet()) {
      Class<?> type = resolveCapabilityType(entry.getKey());
      Map<String, Object> values = (Map<String, Object>) entry.getValue();
      capabilities.add(createCapability(type, values));
    }

    if (capabilities.isEmpty()) {
      throw new IllegalArgumentException("Unsupported capability config: " + entity.getConfigJson());
    }
    return capabilities;
  }

  private String writeConfig(Set<AssetCharacteristic> capabilities) {
    Map<String, Object> wrapper = new LinkedHashMap<>();
    for (AssetCharacteristic capability : capabilities) {
      if (capability instanceof HasBalance hasBalance) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("balance", hasBalance.balance().getAmount());
        value.put("currency", hasBalance.balance().getCurrencyCode());
        wrapper.put(hasBalance.getClass().getName(), value);
      } else if (capability instanceof Consumable consumable) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("quantity", consumable.quantity());
        value.put("reusable", consumable.reusable());
        wrapper.put(consumable.getClass().getName(), value);
      } else if (capability instanceof Ownable ownable) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("type", ownable.type().name());
        wrapper.put(ownable.getClass().getName(), value);
      } else {
        throw new IllegalArgumentException("Unsupported capability: " + capability.getClass().getSimpleName());
      }
    }

    try {
      return objectMapper.writeValueAsString(wrapper);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize capability config", e);
    }
  }

  private Map<String, Object> readConfig(String json) {
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid capability config JSON", e);
    }
  }

  private Class<?> resolveCapabilityType(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Unknown capability type: " + className, e);
    }
  }

  private AssetCharacteristic createCapability(Class<?> type, Map<String, Object> values) {
    if (type.equals(HasBalance.class)) {
      Number balance = (Number) values.get("balance");
      String currency = (String) values.get("currency");
      return HasBalance.of(Money.of(toBigDecimal(balance), currency));
    }
    if (type.equals(Consumable.class)) {
      Boolean reusable = (Boolean) values.getOrDefault("reusable", Boolean.TRUE);
      Number quantity = (Number) values.getOrDefault("quantity", 1);
      return reusable ? Consumable.multipleUse(quantity.intValue()) : Consumable.singleUse(quantity.intValue());
    }
    if (type.equals(Ownable.class)) {
      String typeName = (String) values.get("type");
      return "SHARED".equalsIgnoreCase(typeName) ? Ownable.shared() : Ownable.personal();
    }

    throw new IllegalArgumentException("Unsupported capability type: " + type.getName());
  }

  private BigDecimal toBigDecimal(Number number) {
    if (number instanceof BigDecimal bigDecimal) {
      return bigDecimal;
    }
    return BigDecimal.valueOf(number.doubleValue());
  }
}

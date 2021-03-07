package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Team implements MinecraftPacket {
  public static final byte FRIENDLY_FLAG_ALLOW_FRIENDLY_FIRE = 0x01;
  public static final byte FRIENDLY_FLAG_CAN_SEE_INVISIBLE_PLAYERS_ON_SAME_TEAM = 0x02;

  private @MonotonicNonNull String name;
  private @MonotonicNonNull Mode mode;
  private @Nullable Component displayName;
  private byte friendlyFlags;
  private @Nullable CollisionRule collisionRule; // 1.9+
  private @Nullable NameTagVisibility nameTagVisibility; // 1.8+
  private int color; // 1.8+
  private @MonotonicNonNull Component prefix;
  private @MonotonicNonNull Component suffix;
  private @MonotonicNonNull String[] entities; // List of player usernames, or entity UUIDs (latter 1.8+)

  public Team() {

  }

  public String getName() {
    return name;
  }

  public Mode getMode() {
    return mode;
  }

  public Component getDisplayName() {
    if (mode != Mode.CREATE && mode != Mode.UPDATE_INFO) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return displayName;
  }

  public byte getFriendlyFlags() {
    if (mode != Mode.CREATE && mode != Mode.UPDATE_INFO) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return friendlyFlags;
  }

  /**
   * Present since 1.9
   *
   * @return
   */
  @Nullable
  public CollisionRule getCollisionRule() {
    if (mode != Mode.CREATE && mode != Mode.UPDATE_INFO) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return collisionRule;
  }

  /**
   * Present since 1.8
   *
   * @return
   */
  @Nullable
  public NameTagVisibility getNameTagVisibility() {
    if (mode != Mode.CREATE && mode != Mode.UPDATE_INFO) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return nameTagVisibility;
  }

  /**
   * Present since 1.8
   *
   * @return
   */
  public int getColor() {
    if (mode != Mode.CREATE && mode != Mode.UPDATE_INFO) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return color;
  }

  public Component getPrefix() {
    if (mode != Mode.CREATE && mode != Mode.UPDATE_INFO) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return prefix;
  }

  public Component getSuffix() {
    if (mode != Mode.CREATE && mode != Mode.UPDATE_INFO) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return suffix;
  }

  public Set<String> getEntities() {
    if (mode != Mode.CREATE && mode != Mode.ADD_ENTITIES && mode != Mode.REMOVE_ENTITIES) {
      throw new IllegalArgumentException("Not available in this mode");
    }
    return ImmutableSet.copyOf(entities);
  }

  public static Team newCreate(String name, Component displayName, byte friendlyFlags,
                               @Nullable NameTagVisibility nameTagVisibility,
                               @Nullable CollisionRule collisionRule, int color,
                               Component prefix, Component suffix, Set<String> entities) {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(prefix, "prefix");
    Preconditions.checkNotNull(suffix, "suffix");
    Preconditions.checkNotNull(entities, "entities");
    Preconditions.checkArgument(name.length() <= 16, "Team name cannot be longer than 16 characters");

    Team team = new Team();
    team.name = name;
    team.mode = Mode.CREATE;
    team.displayName = displayName;
    team.friendlyFlags = friendlyFlags;
    team.nameTagVisibility = nameTagVisibility;
    team.collisionRule = collisionRule;
    team.color = color;
    team.prefix = prefix;
    team.suffix = suffix;
    team.entities = ImmutableSet.copyOf(entities).toArray(new String[0]);;

    return team;
  }

  public static Team newRemove(String name) {
    Preconditions.checkNotNull(name, "Team name cannot be null");
    Preconditions.checkArgument(name.length() <= 16, "Team name cannot be longer than 16 characters");

    Team team = new Team();
    team.name = name;
    team.mode = Mode.REMOVE;

    return team;
  }

  public static Team newUpdate(String name, Component displayName, byte friendlyFlags,
                               @Nullable NameTagVisibility nameTagVisibility,
                               @Nullable CollisionRule collisionRule, int color,
                               Component prefix, Component suffix) {
    Team team = newCreate(name, displayName, friendlyFlags, nameTagVisibility,
        collisionRule, color, prefix, suffix, Collections.emptySet());
    team.mode = Mode.UPDATE_INFO;

    return team;
  }

  public static Team newAddEntities(String name, Collection<String> entities) {
    Preconditions.checkNotNull(name, "Team name cannot be null");
    Preconditions.checkArgument(name.length() <= 16, "Team name cannot be longer than 16 characters");
    Preconditions.checkArgument(!entities.isEmpty(), "Entities array must not be empty");

    Team team = new Team();
    team.name = name;
    team.mode = Mode.ADD_ENTITIES;
    team.entities = ImmutableSet.copyOf(entities).toArray(new String[0]);

    return team;
  }

  public static Team newRemoveEntities(String name, Collection<String> entities) {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(entities, "entities");
    Preconditions.checkArgument(name.length() <= 16, "Team name cannot be longer than 16 characters");
    Preconditions.checkArgument(!entities.isEmpty(), "Entities array must not be empty");

    Team team = new Team();
    team.name = name;
    team.mode = Mode.REMOVE_ENTITIES;
    team.entities = ImmutableSet.copyOf(entities).toArray(new String[0]);

    return team;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    final GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(protocolVersion);
    name = ProtocolUtils.readString(buf, 16);
    mode = Preconditions.checkNotNull(Mode.of(buf.readByte()), "mode");

    if (mode == Mode.CREATE || mode == Mode.UPDATE_INFO) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
        displayName = serializer.deserialize(ProtocolUtils.readString(buf));
      } else {
        displayName = LegacyComponentSerializer.legacySection().deserialize(ProtocolUtils.readString(buf, 32));
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        prefix = LegacyComponentSerializer.legacySection().deserialize(ProtocolUtils.readString(buf, 16));
        suffix = LegacyComponentSerializer.legacySection().deserialize(ProtocolUtils.readString(buf, 16));
      }

      friendlyFlags = buf.readByte();
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        nameTagVisibility = NameTagVisibility.VALUES.get(ProtocolUtils.readString(buf, 32));
      }
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
        collisionRule = CollisionRule.VALUES.get(ProtocolUtils.readString(buf, 32));
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        color = (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0
            ? ProtocolUtils.readVarInt(buf) : buf.readByte());
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
        prefix = serializer.deserialize(ProtocolUtils.readString(buf));
        suffix = serializer.deserialize(ProtocolUtils.readString(buf));
      }
    }

    if (mode == Mode.CREATE || mode == Mode.ADD_ENTITIES || mode == Mode.REMOVE_ENTITIES) {
      // TODO: max cap?
      int players = protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0
          ? ProtocolUtils.readVarInt(buf) : buf.readShort();

      Set<String> collectedEntities = new HashSet<>(players);
      for (int i = 0; i < players; i++) {
        // Cap is 40 since 1.8, covers both usernames and entity UUIDs
        collectedEntities.add(ProtocolUtils.readString(buf, 40));
      }
      entities = collectedEntities.toArray(new String[0]);
    }
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    final GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(protocolVersion);
    ProtocolUtils.writeString(buf, name);
    buf.writeByte(mode.ordinal());

    if (mode == Mode.CREATE || mode == Mode.UPDATE_INFO) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
        ProtocolUtils.writeString(buf, serializer.serialize(displayName));
      } else {
        ProtocolUtils.writeString(buf, LegacyComponentSerializer.legacySection().serialize(displayName));
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        ProtocolUtils.writeString(buf, LegacyComponentSerializer.legacySection().serialize(prefix));
        ProtocolUtils.writeString(buf, LegacyComponentSerializer.legacySection().serialize(suffix));
      }

      buf.writeByte(friendlyFlags);
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        ProtocolUtils.writeString(buf, nameTagVisibility.getValue());
      }
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
        ProtocolUtils.writeString(buf, collisionRule.getValue());
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
          ProtocolUtils.writeVarInt(buf, color);
        } else {
          buf.writeByte(color);
        }
      }

      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
        ProtocolUtils.writeString(buf, serializer.serialize(prefix));
        ProtocolUtils.writeString(buf, serializer.serialize(suffix));
      }
    }

    if (mode == Mode.CREATE || mode == Mode.ADD_ENTITIES || mode == Mode.REMOVE_ENTITIES) {
      if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
        buf.writeShort(entities.length);
      } else {
        ProtocolUtils.writeVarInt(buf, entities.length);
      }
      for (String entity : entities) {
        ProtocolUtils.writeString(buf, entity);
      }
    }
  }

  @Override
  public String toString() {
    return "Team{" +
        "name='" + name + '\'' +
        ", mode=" + mode +
        ", displayName=" + displayName +
        ", friendlyFlags=" + friendlyFlags +
        ", collisionRule=" + collisionRule +
        ", nameTagVisibility=" + nameTagVisibility +
        ", color=" + color +
        ", prefix=" + prefix +
        ", suffix=" + suffix +
        ", entities=" + Arrays.toString(entities) +
        '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public enum Mode {
    CREATE,
    REMOVE,
    UPDATE_INFO,
    ADD_ENTITIES,
    REMOVE_ENTITIES,
    ;

    @Nullable
    public static Mode of(byte value) {
      Mode[] values = Mode.values();
      if (value < 0 || value > values.length) {
        return null;
      }
      return values[value];
    }
  }

  public enum CollisionRule {
    ALWAYS("always"),
    PUSH_OTHER_TEAMS("pushOtherTeams"),
    PUSH_OWN_TEAM("pushOwnTeam"),
    NEVER("never"),
    ;

    private final String value;

    CollisionRule(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static final java.util.Map<String, CollisionRule> VALUES;

    static {
      Map<String, CollisionRule> values = new HashMap<>();
      for (CollisionRule value : values()) {
        values.put(value.getValue(), value);
      }
      VALUES = Collections.unmodifiableMap(values);
    }
  }

  public enum NameTagVisibility {
    ALWAYS("always"),
    HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
    HIDE_FOR_OWN_TEAM("hideForOwnTeam"),
    NEVER("never"),
    EMPTY_STRING(""), // TODO: ViaVersion does this, default to what?
    ;

    private final String value;

    NameTagVisibility(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static final java.util.Map<String, NameTagVisibility> VALUES;

    static {
      Map<String, NameTagVisibility> values = new HashMap<>();
      for (NameTagVisibility value : values()) {
        values.put(value.getValue(), value);
      }
      VALUES = Collections.unmodifiableMap(values);
    }
  }
}

package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class Handshake implements MinecraftPacket {

  // This size was chosen to ensure Forge clients can still connect even with very long hostnames.
  // While DNS technically allows any character to be used, in practice ASCII is used.
  private static final int MAXIMUM_HOSTNAME_LENGTH = 255 + HANDSHAKE_HOSTNAME_TOKEN.length() + 1;
  private ProtocolVersion protocolVersion;
  private String serverAddress = "";
  private int port;
  private int nextStatus;

  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public void setServerAddress(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getNextStatus() {
    return nextStatus;
  }

  public void setNextStatus(int nextStatus) {
    this.nextStatus = nextStatus;
  }

  @Override
  public String toString() {
    return "Handshake{"
        + "protocolVersion=" + protocolVersion
        + ", serverAddress='" + serverAddress + '\''
        + ", port=" + port
        + ", nextStatus=" + nextStatus
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion ignored) {
    int realProtocolVersion = ProtocolUtils.readVarInt(buf);
    this.protocolVersion = ProtocolVersion.getProtocolVersion(realProtocolVersion);
    this.serverAddress = ProtocolUtils.readAsciiString(buf, MAXIMUM_HOSTNAME_LENGTH);
    this.port = buf.readUnsignedShort();
    this.nextStatus = ProtocolUtils.readVarInt(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion ignored) {
    ProtocolUtils.writeVarInt(buf, this.protocolVersion.getProtocol());
    ProtocolUtils.writeString(buf, this.serverAddress);
    buf.writeShort(this.port);
    ProtocolUtils.writeVarInt(buf, this.nextStatus);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}

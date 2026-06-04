package app.clearspace.network;

interface IAetherIpc {
    String getIndex();
    String getContent(String hash);
    String getProfile();
    String getRelayIndex(long currentTime);
    String getRelayPacket(long currentTime, String packetId);
    
    // For sync
    long getRelayUsage();
    String getMyHashedId();
    boolean hasRelayPacket(long currentTime, String packetId);
    void insertRelayPacket(String packetJson);
    void enforceStorageQuota();
}

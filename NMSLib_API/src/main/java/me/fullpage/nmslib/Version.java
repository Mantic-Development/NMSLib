package me.fullpage.nmslib;

public enum Version {
    /**
     * Represents the 1.8.8 version of Minecraft
     */
    v1_8_R3,
    /**
     * Represents the 1.12.2 version of Minecraft
     */
    v1_12_R1,
    /**
     * Represents the 1.13.2 version of Minecraft
     */
    v1_13_R2,
    v1_14_R1,
    v1_16_R3,
    v1_17_R1,
    v1_18_R1,
    v1_18_R2,
    v1_19_R1,
    v1_19_R2,
    v1_19_R3,
    v1_20_R1,
    v1_20_R2,
    v1_20_R3,
    /**
     * Represents the 1.20.5 & 1.20.6 version of Minecraft
     */
    v1_20_R4,
    /**
     * Represents the 1.21 version of Minecraft
     */
    v1_21_R1,

    UNKNOWN;

    static Version current = UNKNOWN;
    public static Version getCurrent() {
        return current;
    }

    public static Version getVersion(String version) {
        try {
            return Version.valueOf(version);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }



}

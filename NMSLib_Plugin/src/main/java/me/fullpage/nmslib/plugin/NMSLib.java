package me.fullpage.nmslib.plugin;

import me.fullpage.nmslib.NMSHandler;
import me.fullpage.nmslib_fallback.NMSLib_Fallback;
import me.fullpage.nmslib_v1_12_r1.NMSLib_V1_12_R1;
import me.fullpage.nmslib_v1_13_r2.NMSLib_V1_13_R2;
import me.fullpage.nmslib_v1_14_r1.NMSLib_V1_14_R1;
import me.fullpage.nmslib_v1_16_r3.NMSLib_V1_16_R3;
import me.fullpage.nmslib_v1_17_r1.NMSLib_V1_17_R1;
import me.fullpage.nmslib_v1_18_r1.NMSLib_V1_18_R1;
import me.fullpage.nmslib_v1_18_r2.NMSLib_V1_18_R2;
import me.fullpage.nmslib_v1_19_r1.NMSLib_V1_19_R1;
import me.fullpage.nmslib_v1_19_r2.NMSLib_V1_19_R2;
import me.fullpage.nmslib_v1_19_r3.NMSLib_V1_19_R3;
import me.fullpage.nmslib_v1_8_r3.NMSLib_V1_8_R3;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class NMSLib {

    private static final String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static NMSHandler nmsHandler;

    public static NMSHandler init(Plugin plugin) {
        plugin.getLogger().info("Found NMS Version: " + nmsVersion);
        switch (nmsVersion) {
            case "v1_8_R3":
                nmsHandler = new NMSLib_V1_8_R3();
                break;
            case "v1_12_R1":
                nmsHandler = new NMSLib_V1_12_R1();
                break;
            case "v1_13_R2":
                nmsHandler = new NMSLib_V1_13_R2();
                break;
            case "v1_14_R1":
                nmsHandler = new NMSLib_V1_14_R1();
                break;
            case "v1_16_R3":
                nmsHandler = new NMSLib_V1_16_R3();
                break;
            case "v1_17_R1":
                nmsHandler = new NMSLib_V1_17_R1();
                break;
            case "v1_18_R1":
                nmsHandler = new NMSLib_V1_18_R1();
                break;
            case "v1_18_R2":
                nmsHandler = new NMSLib_V1_18_R2();
                break;
            case "v1_19_R1":
                nmsHandler = new NMSLib_V1_19_R1();
                break;
            case "v1_19_R2":
                nmsHandler = new NMSLib_V1_19_R2();
                break;
            case "v1_19_R3":
                nmsHandler = new NMSLib_V1_19_R3();
            default:
                plugin.getLogger().info("Cannot find NMS Support, fall backing to api methods. ");
                nmsHandler = new NMSLib_Fallback();
                break;
        }
        return nmsHandler;
    }

    public static String getNmsVersion() {
        return nmsVersion;
    }

    public static NMSHandler getNmsHandler() {
        if (nmsHandler == null) {
            throw new IllegalStateException("NMSLib is not initialized yet!");
        }
        return nmsHandler;
    }

}

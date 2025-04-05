package me.fullpage.nmslib.plugin;

import me.fullpage.nmslib.NMSHandler;
import me.fullpage.nmslib.Version;
import me.fullpage.nmslib.fallback.NMSLib_Fallback;
import me.fullpage.nmslib.v1_12_r1.NMSLib_V1_12_R1;
import me.fullpage.nmslib.v1_13_r2.NMSLib_V1_13_R2;
import me.fullpage.nmslib.v1_14_r1.NMSLib_V1_14_R1;
import me.fullpage.nmslib.v1_16_r3.NMSLib_V1_16_R3;
import me.fullpage.nmslib.v1_17_r1.NMSLib_V1_17_R1;
import me.fullpage.nmslib.v1_18_r1.NMSLib_V1_18_R1;
import me.fullpage.nmslib.v1_18_r2.NMSLib_V1_18_R2;
import me.fullpage.nmslib.v1_19_r1.NMSLib_V1_19_R1;
import me.fullpage.nmslib.v1_19_r2.NMSLib_V1_19_R2;
import me.fullpage.nmslib.v1_19_r3.NMSLib_V1_19_R3;
import me.fullpage.nmslib.v1_20_r1.NMSLib_V1_20_R1;
import me.fullpage.nmslib.v1_20_r2.NMSLib_V1_20_R2;
import me.fullpage.nmslib.v1_20_r3.NMSLib_V1_20_R3;
import me.fullpage.nmslib.v1_20_r4.NMSLib_V1_20_R4;
import me.fullpage.nmslib.v1_21_r1.NMSLib_V1_21_R1;
import me.fullpage.nmslib.v1_21_r2.NMSLib_V1_21_R2;
import me.fullpage.nmslib.v1_21_r3.NMSLib_V1_21_R3;
import me.fullpage.nmslib.v1_8_r3.NMSLib_V1_8_R3;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

public class NMSLib {

    private static final String nmsVersion;

    static {
        String ver = null;
        try {
            ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e) {
            ver = Bukkit.getBukkitVersion().split("-")[0];
            if (ver.equals("1.20.5") || ver.equals("1.20.6")) {
                ver = "v1_20_R4";
            } if (ver.equals("1.21") || ver.equals("1.21.1")) {
                ver = "v1_21_R1";
            } if (ver.equals("1.21.3") || ver.equals("1.21.2")) {
                ver = "v1_21_R2";
            }if (ver.equals("1.21.4")) {
                ver = "v1_21_R3";
            }
        } finally {
            nmsVersion = ver;
        }
    }

    private static NMSHandler nmsHandler;

    public static NMSHandler init(Plugin plugin) {
        plugin.getLogger().info("Found NMS Version: " + nmsVersion);
        Version version = Version.getVersion(nmsVersion);
        setCurrent(version);
        switch (version) {
            case v1_8_R3:
                nmsHandler = new NMSLib_V1_8_R3();
                break;
            case v1_12_R1:
                nmsHandler = new NMSLib_V1_12_R1();
                break;
            case v1_13_R2:
                nmsHandler = new NMSLib_V1_13_R2();
                break;
            case v1_14_R1:
                nmsHandler = new NMSLib_V1_14_R1();
                break;
            case v1_16_R3:
                nmsHandler = new NMSLib_V1_16_R3();
                break;
            case v1_17_R1:
                nmsHandler = new NMSLib_V1_17_R1();
                break;
            case v1_18_R1:
                nmsHandler = new NMSLib_V1_18_R1();
                break;
            case v1_18_R2:
                nmsHandler = new NMSLib_V1_18_R2();
                break;
            case v1_19_R1:
                nmsHandler = new NMSLib_V1_19_R1();
                break;
            case v1_19_R2:
                nmsHandler = new NMSLib_V1_19_R2();
                break;
            case v1_19_R3:
                nmsHandler = new NMSLib_V1_19_R3();
                break;
            case v1_20_R1:
                nmsHandler = new NMSLib_V1_20_R1();
                break;
            case v1_20_R2:
                nmsHandler = new NMSLib_V1_20_R2();
                break;
            case v1_20_R3:
                nmsHandler = new NMSLib_V1_20_R3();
                break;
            case v1_20_R4:
                nmsHandler = new NMSLib_V1_20_R4();
                break;
            case v1_21_R1:
                nmsHandler = new NMSLib_V1_21_R1();
                break;
            case v1_21_R2:
                nmsHandler = new NMSLib_V1_21_R2();
                break;
            case v1_21_R3:
                nmsHandler = new NMSLib_V1_21_R3();
                break;
            default:
                plugin.getLogger().info("Cannot find NMS Support, fall backing to api methods. ");
                nmsHandler = new NMSLib_Fallback();
                break;
        }
        return nmsHandler;
    }

    private static void setCurrent(Version version) {
        try {
            Field field = Version.class.getDeclaredField("current");
            field.setAccessible(true);
            field.set(null, version);
            field.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

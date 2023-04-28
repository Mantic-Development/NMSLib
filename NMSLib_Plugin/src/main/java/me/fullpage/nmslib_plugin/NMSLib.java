package me.fullpage.nmslib_plugin;

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
import me.fullpage.nmslib_v1_8_r3.NMSLib_V1_8_R3;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * @see me.fullpage.nmslib.plugin.NMSLib
 */
@Deprecated
public class NMSLib {

    public static NMSHandler init(Plugin plugin) {
        return me.fullpage.nmslib.plugin.NMSLib.init(plugin);
    }

    public static String getNmsVersion() {
        return me.fullpage.nmslib.plugin.NMSLib.getNmsVersion();
    }

    public static NMSHandler getNmsHandler() {
       return me.fullpage.nmslib.plugin.NMSLib.getNmsHandler();
    }

}

package me.fullpage.nmslib_plugin;

import me.fullpage.nmslib.NMSHandler;
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

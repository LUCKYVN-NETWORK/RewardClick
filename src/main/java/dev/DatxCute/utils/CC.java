package dev.DatxCute.utils;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class CC {
    public static final ChatColor BLUE = ChatColor.BLUE;
    public static final ChatColor AQUA = ChatColor.AQUA;
    public static final ChatColor GREEN = ChatColor.GREEN;
    public static final ChatColor RED = ChatColor.RED;
    public static final ChatColor WHITE = ChatColor.WHITE;
    public static final ChatColor YELLOW = ChatColor.YELLOW;
    public static final ChatColor LIGHT_PURPLE = ChatColor.LIGHT_PURPLE;
    public static final ChatColor GRAY = ChatColor.GRAY;
    public static final ChatColor DARK_GRAY = ChatColor.DARK_GRAY;
    public static final ChatColor GOLD = ChatColor.GOLD;
    public static final ChatColor DARK_PURPLE = ChatColor.DARK_PURPLE;
    public static final ChatColor DARK_RED = ChatColor.DARK_RED;
    public static final ChatColor DARK_AQUA = ChatColor.DARK_AQUA;
    public static final ChatColor DARK_GREEN = ChatColor.DARK_GREEN;
    public static final ChatColor DARK_BLUE = ChatColor.DARK_BLUE;
    public static final ChatColor BLACK = ChatColor.BLACK;

    public static String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> translate(List<String> list) {
        return list.stream().map(CC::translate).collect(Collectors.toList());
    }
}

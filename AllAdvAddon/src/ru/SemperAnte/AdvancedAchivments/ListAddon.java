package ru.SemperAnte.AdvancedAchivments;

import com.hm.achievement.api.AdvancedAchievementsAPI;
import com.hm.achievement.api.AdvancedAchievementsAPIFetcher;
import com.hm.achievement.utils.PlayerAdvancedAchievementEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.SemperAnte.API.Commands.CommandRegisterer;
import ru.SemperAnte.API.Commands.RegisterCommand;
import ru.SemperAnte.API.Interface.Classes.Sender;
import ru.SemperAnte.API.Interfaces.APICommandSender;
import ru.SemperAnte.API.Plugins.APIPlugin;

import java.util.*;

public class ListAddon extends APIPlugin implements Listener
{
    private static Map<String, Set<String>> needed = new HashMap<>();
    private static AdvancedAchievementsAPI api;

    @Override
    public boolean postInit()
    {

        if (!AdvancedAchievementsAPIFetcher.fetchInstance().isPresent())
        {
            loggerUtils.logWarning("I can't work without Advanced Achivements API");
            return false;
        }
        api = AdvancedAchievementsAPIFetcher.fetchInstance().get();
        configReload();
        CommandRegisterer.registerCommands(this, this, Sender.class);
        Bukkit.getPluginManager().registerEvents(this, this);
        return true;
    }

    @RegisterCommand(name = "achivmentaddon", aliases = {"laddon"}, permission = "laddon.reload")
    public boolean reload(APICommandSender<CommandSender> sender)
    {
        configReload();
        sender.sendPrefixedMessage("Done.");
        return false;
    }

    private void configReload()
    {
        ConfigurationSection cs = configUtils.getConfigurationSection("achivments");
        for (String s : cs.getKeys(false))
        {
            List<String> strs = cs.getStringList(s);
            if (strs != null && !strs.isEmpty())
            {
                Set<String> set = new HashSet<>(strs);
                needed.put(s, set);
            }
        }
    }

    @EventHandler
    public void onAchivment(PlayerAdvancedAchievementEvent event)
    {
        for (Map.Entry<String, Set<String>> entry : needed.entrySet())
        {
            if (entry.getValue().contains(event.getName()))
            {
                boolean all = true;
                for (String s : entry.getValue())
                {

                    if (!api.hasPlayerReceivedAchievement(event.getPlayer().getUniqueId(), s) && !s.equalsIgnoreCase(event.getName()))
                    {
                        all = false;
                        break;
                    }
                }
                if (all)
                {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "aach give " + entry.getKey() + " " + event.getPlayer().getName());
                }
            }
        }
        event.getPlayer().sendMessage(ChatColor.YELLOW + "[Achievement]" + ChatColor.GREEN + " Сообщение к достижению: " + ChatColor.GOLD + event.getMessage());
    }
}

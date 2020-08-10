package com.hm.achievement.lifecycle;

import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitTask;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.category.Category;
import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.command.completer.CommandTabCompleter;
import com.hm.achievement.command.executable.ReloadCommand;
import com.hm.achievement.command.executor.PluginCommandExecutor;
import com.hm.achievement.config.ConfigurationParser;
import com.hm.achievement.db.AbstractDatabaseManager;
import com.hm.achievement.db.AsyncCachedRequestsSender;
import com.hm.achievement.exception.PluginLoadError;
import com.hm.achievement.listener.FireworkListener;
import com.hm.achievement.listener.JoinListener;
import com.hm.achievement.listener.ListGUIListener;
import com.hm.achievement.listener.PlayerAdvancedAchievementListener;
import com.hm.achievement.listener.QuitListener;
import com.hm.achievement.listener.TeleportListener;
import com.hm.achievement.listener.statistics.AbstractListener;
import com.hm.achievement.placeholder.AchievementCountBungeeTabListPlusVariable;
import com.hm.achievement.placeholder.AchievementPlaceholderHook;
import com.hm.achievement.runnable.AchieveDistanceRunnable;
import com.hm.achievement.runnable.AchievePlayTimeRunnable;
import com.hm.mcshared.file.CommentedYamlConfiguration;
import com.hm.mcshared.update.UpdateChecker;

import codecrafter47.bungeetablistplus.api.bukkit.BungeeTabListPlusBukkitAPI;
import dagger.Lazy;

/**
 * Class in charge of loading/reloading the plugin. Orchestrates the different plugin components together.
 *
 * @author Pyves
 */
@Singleton
public class PluginLoader {

	private final AdvancedAchievements advancedAchievements;
	private final Logger logger;
	private final Lazy<UpdateChecker> updateChecker;
	private final ReloadCommand reloadCommand;
	private final Set<Reloadable> reloadables;

	// Listeners, to monitor various events.
	private final FireworkListener fireworkListener;
	private final JoinListener joinListener;
	private final ListGUIListener listGUIListener;
	private final PlayerAdvancedAchievementListener playerAdvancedAchievementListener;
	private final QuitListener quitListener;
	private final TeleportListener teleportListener;

	// Integrations with other plugins. Use lazy injection as these may or may not be used depending on runtime
	// conditions.
	private final Lazy<AchievementPlaceholderHook> achievementPlaceholderHook;
	private final Lazy<AchievementCountBungeeTabListPlusVariable> achievementCountBungeeTabListPlusVariable;

	// Database related.
	private final AbstractDatabaseManager databaseManager;
	private final AsyncCachedRequestsSender asyncCachedRequestsSender;

	// Various other fields and parameters.
	private final PluginCommandExecutor pluginCommandExecutor;
	private final CommandTabCompleter commandTabCompleter;
	private final Set<Category> disabledCategories;
	private final CommentedYamlConfiguration mainConfig;
	private final ConfigurationParser configurationParser;

	// Plugin runnable classes.
	private final AchieveDistanceRunnable distanceRunnable;
	private final AchievePlayTimeRunnable playTimeRunnable;

	// Bukkit scheduler tasks.
	private BukkitTask asyncCachedRequestsSenderTask;
	private BukkitTask playedTimeTask;
	private BukkitTask distanceTask;

	@Inject
	public PluginLoader(AdvancedAchievements advancedAchievements, Logger logger, Set<Reloadable> reloadables,
			FireworkListener fireworkListener, JoinListener joinListener, ListGUIListener listGUIListener,
			PlayerAdvancedAchievementListener playerAdvancedAchievementListener, QuitListener quitListener,
			TeleportListener teleportListener, Lazy<AchievementPlaceholderHook> achievementPlaceholderHook,
			Lazy<AchievementCountBungeeTabListPlusVariable> achievementCountBungeeTabListPlusVariable,
			AbstractDatabaseManager databaseManager, AsyncCachedRequestsSender asyncCachedRequestsSender,
			PluginCommandExecutor pluginCommandExecutor, CommandTabCompleter commandTabCompleter,
			Set<Category> disabledCategories, @Named("main") CommentedYamlConfiguration mainConfig,
			ConfigurationParser configurationParser, AchieveDistanceRunnable distanceRunnable,
			AchievePlayTimeRunnable playTimeRunnable, Lazy<UpdateChecker> updateChecker, ReloadCommand reloadCommand) {
		this.advancedAchievements = advancedAchievements;
		this.logger = logger;
		this.reloadables = reloadables;
		this.fireworkListener = fireworkListener;
		this.joinListener = joinListener;
		this.listGUIListener = listGUIListener;
		this.playerAdvancedAchievementListener = playerAdvancedAchievementListener;
		this.quitListener = quitListener;
		this.teleportListener = teleportListener;
		this.achievementPlaceholderHook = achievementPlaceholderHook;
		this.achievementCountBungeeTabListPlusVariable = achievementCountBungeeTabListPlusVariable;
		this.databaseManager = databaseManager;
		this.asyncCachedRequestsSender = asyncCachedRequestsSender;
		this.pluginCommandExecutor = pluginCommandExecutor;
		this.commandTabCompleter = commandTabCompleter;
		this.disabledCategories = disabledCategories;
		this.mainConfig = mainConfig;
		this.configurationParser = configurationParser;
		this.distanceRunnable = distanceRunnable;
		this.playTimeRunnable = playTimeRunnable;
		this.updateChecker = updateChecker;
		this.reloadCommand = reloadCommand;
	}

	/**
	 * Loads the plugin.
	 *
	 * @param firstLoad
	 * @throws PluginLoadError
	 */
	public void loadAdvancedAchievements(boolean firstLoad) throws PluginLoadError {
		configurationParser.loadAndParseConfiguration();
		registerListeners();
		if (firstLoad) {
			databaseManager.initialise();
			initialiseCommands();
		}
		launchScheduledTasks();
		launchUpdateChecker();
		registerPermissions();
		reloadCommand.notifyObservers();
		if (firstLoad) {
			linkPlaceholders();
		}
	}

	/**
	 * Disables the plugin.
	 */
	public void disableAdvancedAchievements() {
		// Cancel scheduled tasks.
		if (asyncCachedRequestsSenderTask != null) {
			asyncCachedRequestsSenderTask.cancel();
		}
		if (playedTimeTask != null) {
			playedTimeTask.cancel();
		}
		if (distanceTask != null) {
			distanceTask.cancel();
		}

		// Send remaining statistics to the database and close DatabaseManager.
		asyncCachedRequestsSender.sendBatchedRequests();
		databaseManager.shutdown();

		logger.info("Remaining requests sent to the database, plugin successfully disabled.");
	}

	/**
	 * Registers the different event listeners so they can monitor server events. If relevant categories are disabled,
	 * listeners aren't registered.
	 */
	private void registerListeners() {
		logger.info("Registering event listeners...");
		PluginManager pluginManager = advancedAchievements.getServer().getPluginManager();
		reloadables.forEach(r -> {
			if (r instanceof AbstractListener) {
				AbstractListener listener = (AbstractListener) r;
				HandlerList.unregisterAll(listener);
				if (!disabledCategories.contains(listener.getCategory())) {
					pluginManager.registerEvents(listener, advancedAchievements);
				}
			}
		});
		HandlerList.unregisterAll(fireworkListener);
		pluginManager.registerEvents(fireworkListener, advancedAchievements);
		HandlerList.unregisterAll(joinListener);
		pluginManager.registerEvents(joinListener, advancedAchievements);
		HandlerList.unregisterAll(listGUIListener);
		pluginManager.registerEvents(listGUIListener, advancedAchievements);
		HandlerList.unregisterAll(playerAdvancedAchievementListener);
		pluginManager.registerEvents(playerAdvancedAchievementListener, advancedAchievements);
		HandlerList.unregisterAll(quitListener);
		pluginManager.registerEvents(quitListener, advancedAchievements);
		HandlerList.unregisterAll(teleportListener);
		pluginManager.registerEvents(teleportListener, advancedAchievements);
	}

	/**
	 * Links the plugin's custom command tab completer and command executor.
	 */
	private void initialiseCommands() {
		logger.info("Setting up command executor and custom tab completers...");

		PluginCommand pluginCommand = Bukkit.getPluginCommand("aach");
		pluginCommand.setTabCompleter(commandTabCompleter);
		pluginCommand.setExecutor(pluginCommandExecutor);
	}

	/**
	 * Launches asynchronous scheduled tasks.
	 */
	private void launchScheduledTasks() {
		logger.info("Launching scheduled tasks...");

		// Schedule a repeating task to group database queries when statistics are modified.
		if (asyncCachedRequestsSenderTask == null) {
			int configPooledRequestsTaskInterval = mainConfig.getInt("PooledRequestsTaskInterval", 10);
			asyncCachedRequestsSenderTask = Bukkit.getScheduler().runTaskTimerAsynchronously(advancedAchievements,
					asyncCachedRequestsSender, configPooledRequestsTaskInterval * 40L,
					configPooledRequestsTaskInterval * 20L);
		}

		// Schedule a repeating task to monitor played time for each player (not directly related to an event).
		if (playedTimeTask != null) {
			playedTimeTask.cancel();
		}
		if (!disabledCategories.contains(NormalAchievements.PLAYEDTIME)) {
			int configPlaytimeTaskInterval = mainConfig.getInt("PlaytimeTaskInterval", 60);
			playedTimeTask = Bukkit.getScheduler().runTaskTimer(advancedAchievements, playTimeRunnable,
					configPlaytimeTaskInterval * 10L, configPlaytimeTaskInterval * 20L);
		}

		// Schedule a repeating task to monitor distances travelled by each player (not directly related to an event).
		if (distanceTask != null) {
			distanceTask.cancel();
		}
		if (!disabledCategories.contains(NormalAchievements.DISTANCEFOOT)
				|| !disabledCategories.contains(NormalAchievements.DISTANCEPIG)
				|| !disabledCategories.contains(NormalAchievements.DISTANCEHORSE)
				|| !disabledCategories.contains(NormalAchievements.DISTANCEMINECART)
				|| !disabledCategories.contains(NormalAchievements.DISTANCEBOAT)
				|| !disabledCategories.contains(NormalAchievements.DISTANCEGLIDING)
				|| !disabledCategories.contains(NormalAchievements.DISTANCELLAMA)) {
			int configDistanceTaskInterval = mainConfig.getInt("DistanceTaskInterval", 5);
			distanceTask = Bukkit.getScheduler().runTaskTimer(advancedAchievements, distanceRunnable,
					configDistanceTaskInterval * 40L, configDistanceTaskInterval * 20L);
		}
	}

	/**
	 * Launches an update check task. If updateChecker already registered (i.e. reload), does not check for update
	 * again. If CheckForUpdate switched to false unregisters listener.
	 */
	private void launchUpdateChecker() {
		if (!mainConfig.getBoolean("CheckForUpdate", true)) {
			PlayerJoinEvent.getHandlerList().unregister(updateChecker.get());
		} else {
			for (RegisteredListener registeredListener : PlayerJoinEvent.getHandlerList().getRegisteredListeners()) {
				if (registeredListener.getListener() == updateChecker) {
					return;
				}
			}
			advancedAchievements.getServer().getPluginManager().registerEvents(updateChecker.get(), advancedAchievements);
			updateChecker.get().launchUpdateCheckerTask();
		}

	}

	/**
	 * Registers permissions that depend on the user's configuration file (for MultipleAchievements; for instance for
	 * stone breaks, achievement.count.breaks.stone will be registered).
	 */
	private void registerPermissions() {
		logger.info("Registering permissions...");

		PluginManager pluginManager = Bukkit.getPluginManager();
		for (MultipleAchievements category : MultipleAchievements.values()) {
			for (String section : mainConfig.getShallowKeys(category.toString())) {
				// Permission ignores metadata (eg. sand:1) for Breaks, Places and Crafts categories and don't take
				// spaces into account.
				section = StringUtils.deleteWhitespace(StringUtils.substringBefore(section, ":"));

				// Bukkit only allows permissions to be set once, check to ensure they were not previously set when
				// performing /aach reload.
				for (String groupElement : StringUtils.split(section, '|')) {
					String permissionNode = category.toPermName() + "." + groupElement;
					if (pluginManager.getPermission(permissionNode) == null) {
						pluginManager.addPermission(new Permission(permissionNode, PermissionDefault.TRUE));
					}
				}
			}
		}
	}

	/**
	 * Links third-party placeholder plugins (PlaceholderAPI and BungeeTabListPlus currently supported).
	 */
	private void linkPlaceholders() {
		if (Bukkit.getPluginManager().isPluginEnabled("BungeeTabListPlus")) {
			BungeeTabListPlusBukkitAPI.registerVariable(advancedAchievements,
					achievementCountBungeeTabListPlusVariable.get());
		}

		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			achievementPlaceholderHook.get().register();
		}
	}
}

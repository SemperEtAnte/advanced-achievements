package com.hm.achievement.command.executable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.hm.achievement.db.AbstractDatabaseManager;
import com.hm.achievement.db.CacheManager;
import com.hm.achievement.lang.LangHelper;
import com.hm.achievement.lang.command.CmdLang;
import com.hm.mcshared.file.CommentedYamlConfiguration;

/**
 * Class in charge of handling the /aach delete command, which deletes an achievement from a player.
 * 
 * @author Pyves
 */
@Singleton
@CommandSpec(name = "delete", permission = "delete", minArgs = 3, maxArgs = Integer.MAX_VALUE)
public class DeleteCommand extends AbstractParsableCommand {

	private final CacheManager cacheManager;
	private final AbstractDatabaseManager databaseManager;

	private String langCheckAchievementFalse;
	private String langDeleteAchievements;

	@Inject
	public DeleteCommand(@Named("main") CommentedYamlConfiguration mainConfig,
			@Named("lang") CommentedYamlConfiguration langConfig, StringBuilder pluginHeader, CacheManager cacheManager,
			AbstractDatabaseManager databaseManager) {
		super(mainConfig, langConfig, pluginHeader);
		this.cacheManager = cacheManager;
		this.databaseManager = databaseManager;
	}

	@Override
	public void extractConfigurationParameters() {
		super.extractConfigurationParameters();

		langCheckAchievementFalse = pluginHeader + LangHelper.get(CmdLang.CHECK_ACHIEVEMENTS_FALSE, langConfig);
		langDeleteAchievements = pluginHeader + LangHelper.get(CmdLang.DELETE_ACHIEVEMENTS, langConfig);
	}

	@Override
	void onExecuteForPlayer(CommandSender sender, String[] args, Player player) {
		String achievementName = parseAchievementName(args);

		// Check if achievement exists in database and display message accordingly; if received, delete it.
		if (!cacheManager.hasPlayerAchievement(player.getUniqueId(), achievementName)) {
			sender.sendMessage(StringUtils.replaceEach(langCheckAchievementFalse, new String[] { "PLAYER", "ACH" },
					new String[] { args[args.length - 1], achievementName }));
		} else {
			cacheManager.removePreviouslyReceivedAchievement(player.getUniqueId(), achievementName);
			databaseManager.deletePlayerAchievement(player.getUniqueId(), achievementName);

			sender.sendMessage(StringUtils.replaceEach(langDeleteAchievements, new String[] { "PLAYER", "ACH" },
					new String[] { args[args.length - 1], achievementName }));
		}
	}
}

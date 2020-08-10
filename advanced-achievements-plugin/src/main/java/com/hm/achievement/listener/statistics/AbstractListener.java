package com.hm.achievement.listener.statistics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.hm.achievement.category.Category;
import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.db.CacheManager;
import com.hm.achievement.utils.RewardParser;
import com.hm.achievement.utils.StatisticIncreaseHandler;
import com.hm.mcshared.file.CommentedYamlConfiguration;

/**
 * Abstract class in charge of factoring out common functionality for the listener classes.
 * 
 * @author Pyves
 */
public abstract class AbstractListener extends StatisticIncreaseHandler implements Listener {

	final Category category;

	AbstractListener(Category category, CommentedYamlConfiguration mainConfig, int serverVersion,
			Map<String, List<Long>> sortedThresholds, CacheManager cacheManager, RewardParser rewardParser) {
		super(mainConfig, serverVersion, sortedThresholds, cacheManager, rewardParser);
		this.category = category;
	}

	public Category getCategory() {
		return category;
	}

	/**
	 * Updates the statistic in the database for a NormalAchievement and awards an achievement if an available one is
	 * found.
	 * 
	 * @param player
	 * @param incrementValue
	 */
	void updateStatisticAndAwardAchievementsIfAvailable(Player player, int incrementValue) {
		if (shouldIncreaseBeTakenIntoAccount(player, category)) {
			long amount = cacheManager.getAndIncrementStatisticAmount((NormalAchievements) category, player.getUniqueId(),
					incrementValue);
			checkThresholdsAndAchievements(player, category.toString(), amount);
		}
	}

	/**
	 * Updates the statistic in the database for a MultipleAchievement and awards an achievement if an available one is
	 * found.
	 * 
	 * @param player
	 * @param subcategories
	 * @param incrementValue
	 */
	void updateStatisticAndAwardAchievementsIfAvailable(Player player, Set<String> subcategories, int incrementValue) {
		if (shouldIncreaseBeTakenIntoAccount(player, category)) {
			subcategories.forEach(subcategory -> {
				long amount = cacheManager.getAndIncrementStatisticAmount((MultipleAchievements) category, subcategory,
						player.getUniqueId(), incrementValue);
				checkThresholdsAndAchievements(player, category + "." + subcategory, amount);
			});
		}
	}

	/**
	 * Returns all achievements that match the provided identifier.
	 * 
	 * @param identifier the identifier to match
	 * 
	 * @return all matched achievements
	 * @author tassu
	 */
	Set<String> findAchievementsByCategoryAndName(String identifier) {
		return mainConfig.getShallowKeys(category.toString()).stream()
				.filter(keys -> ArrayUtils.contains(StringUtils.split(keys, '|'), identifier))
				.collect(Collectors.toSet());
	}

}

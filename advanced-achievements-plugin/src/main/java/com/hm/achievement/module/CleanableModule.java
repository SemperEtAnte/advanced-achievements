package com.hm.achievement.module;

import com.hm.achievement.command.executable.BookCommand;
import com.hm.achievement.db.CacheManager;
import com.hm.achievement.lifecycle.Cleanable;
import com.hm.achievement.listener.JoinListener;
import com.hm.achievement.listener.statistics.BedsListener;
import com.hm.achievement.listener.statistics.ConnectionsListener;
import com.hm.achievement.listener.statistics.LavaBucketsListener;
import com.hm.achievement.listener.statistics.MilksListener;
import com.hm.achievement.listener.statistics.MusicDiscsListener;
import com.hm.achievement.listener.statistics.WaterBucketsListener;
import com.hm.achievement.runnable.AchieveDistanceRunnable;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;

@Module
public interface CleanableModule {

	@Binds
	@IntoSet
	Cleanable bindAchieveDistanceRunnable(AchieveDistanceRunnable achieveDistanceRunnable);

	@Binds
	@IntoSet
	Cleanable bindBedsListener(BedsListener bedsListener);

	@Binds
	@IntoSet
	Cleanable bindBookCommand(BookCommand bookCommand);

	@Binds
	@IntoSet
	Cleanable bindCacheManager(CacheManager cacheManager);

	@Binds
	@IntoSet
	Cleanable bindConnectionsListener(ConnectionsListener connectionsListener);

	@Binds
	@IntoSet
	Cleanable bindJoinListener(JoinListener joinListener);

	@Binds
	@IntoSet
	Cleanable bindLavaBucketsListener(LavaBucketsListener lavaBucketsListener);

	@Binds
	@IntoSet
	Cleanable bindMilksListener(MilksListener milksListener);

	@Binds
	@IntoSet
	Cleanable bindMusicDiscsListener(MusicDiscsListener musicDiscsListener);

	@Binds
	@IntoSet
	Cleanable bindWaterBucketsListener(WaterBucketsListener waterBucketsListener);

}

package com.preeocxp;

import net.runelite.api.Skill;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("preeocxp")

public interface PreEocXpConfig extends Config
{

	@ConfigItem(
			keyName = "enableTooltips",
			name = "Enable Tooltips",
			description = "Configures whether or not to show tooltips",
			position = 0
	)
	default boolean enableTooltips()
	{
		return true;
	}

	@ConfigItem(
			keyName = "displaySkill",
			name = "Display Xp in this Skill",
			description = "Choose which Skill to display",
			position = 1
	)
	default Skill displaySkill()
	{
		return Skill.OVERALL;
	}

	@ConfigItem(
			keyName = "lotsLimit",
			name = "Set cutoff for Lots!",
			description = "If the xp in your chosen skill exceeds this limit, lots! will be displayed instead",
			position = 2
	)
	default int lotsLimit()
	{
		return 214748364;
	}

	enum OptionEnum
	{
		REGULAR,
		LARGE,
		MASSIVE
	}
	@ConfigItem(
			keyName = "dropSize",
			name = "Xp Drop Size",
			description = "Choose the size of your xp drops",
			position = 3
	)
	default OptionEnum dropSize()
	{ return OptionEnum.REGULAR; }

	@ConfigItem(
			keyName = "enableTwelve",
			name = "2012-Style Xp Drops",
			description = "Enable 2012-Style Xp Drops instead",
			position = 4
	)
	default boolean enableTwelve()
	{ return false; }

	@ConfigItem(
			keyName = "onlyCounter",
			name = "Counter Only",
			description = "Hides the Xp Drops and only displays the Counter",
			position = 5
	)
	default boolean onlyCounter()
	{ return false; }
	@ConfigItem(
			keyName = "onlyDrops",
			name = "Xp Drops Only",
			description = "Hides the Counter and only displays Xp Drops",
			position = 6
	)
	default boolean onlyDrops()
	{ return false; }
}

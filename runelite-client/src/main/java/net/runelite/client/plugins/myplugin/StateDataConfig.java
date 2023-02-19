package net.runelite.client.plugins.myplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("State Data")
public interface StateDataConfig extends Config
{
	@ConfigItem(
		keyName = "gameStateData",
		name = "Game State Data",
		description = "Data"
	)
	default String gameStateData()
	{
		return "Hello";
	}

	@ConfigItem(
			keyName = "showSidePanel",
			name = "Show side panel",
			description = "Shows the side panel"
	)
	default boolean showSidePanel(){return false;}

}

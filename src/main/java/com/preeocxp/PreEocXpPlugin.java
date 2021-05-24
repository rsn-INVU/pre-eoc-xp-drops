/*
 * Copyright (c) 2021, Alex Sander <alex@alexsanderarts.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.preeocxp;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.xptracker.XpTrackerPlugin;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import java.time.temporal.ChronoUnit;

@PluginDescriptor(
		name = "Pre Eoc Xp Drops",
		description = "Customize Xp drops and display the 2010 xp Counter (WIP - currently just 2010 xp drops)",
		tags = {"experience", "levels", "overlay", "xp drop"},
		enabledByDefault = false
)

@PluginDependency(XpTrackerPlugin.class)

public class PreEocXpPlugin extends Plugin
{
	private static long loginXp = 0;
	long preXp = 0;
	public static int xpDrop;
	public static int tickCounter = 0;
	public static boolean sentXp = true;
	private static boolean widgetState = false;

	@Inject
	private PreEocXpConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PreEocXpOverlay overlay;

	@Inject
	Client client;

	@Provides
	PreEocXpConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PreEocXpConfig.class);
	}
	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}
	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}
	//onGameTick cause it works past 200m xp, and longs so people with more than 2b xp can use this
	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		tickCounter ++;
		long overallXp = client.getOverallExperience();
		preXp = loginXp;

		if (loginXp != 0 && (overallXp - preXp <= 0))
		{
			return;
		}
		if (loginXp != 0)
		{
			xpDrop = (int) (overallXp - preXp);
			sentXp = true;
			preXp = overallXp;
			loginXp = preXp;
		}
		else
		{
			loginXp = client.getOverallExperience();
			sentXp = true;
		}
	}

	public static long getLoginXp()
	{
		return loginXp;
	}

	@Schedule(
			period = 1,
			unit = ChronoUnit.SECONDS
	)
	private void resetState()
	{
		loginXp = 0;
	}

	@Subscribe
	public void onOverlayMenuClicked(final OverlayMenuClicked event)
	{
		if (!(event.getEntry().getMenuAction() == MenuAction.RUNELITE_OVERLAY
				&& event.getOverlay() == overlay))
		{
			return;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) throws Exception
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				loginXp = client.getOverallExperience();
				startUp();

			case HOPPING:
			case CONNECTION_LOST:
			case LOGGING_IN:
			case LOGIN_SCREEN:
				resetState();
				break;
		}
	}
}

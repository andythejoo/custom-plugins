/*
 * Copyright (c) 2020, c17 <https://github.com/cyborg-17/c17-plugins>
 * Copyright (c) 2020, Shieldeh <https://github.com/oplosthee>
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
package net.runelite.client.plugins.leaguesZMI;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.time.Instant;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.iutils.BankUtils;
import net.runelite.client.plugins.iutils.CalculationUtils;
import net.runelite.client.plugins.iutils.InterfaceUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.MenuUtils;
import net.runelite.client.plugins.iutils.MouseUtils;
import net.runelite.client.plugins.iutils.NPCUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.iutils.PlayerUtils;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Leagues - ZMI",
	enabledByDefault = false,
	description = "Leagues ZMI runecrafter, banks at Ver Sinhaza, Seers Village with Kandarin Hard, or Crafting Guild.",
	tags = {"zmi", "skill", "boat"},
	type = PluginType.SKILLING
)
public class leaguesZMIPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(leaguesZMIPlugin.class);
	@Inject
	private Client client;
	@Inject
	private iUtils utils;
	@Inject
	private MouseUtils mouse;
	@Inject
	private PlayerUtils playerUtils;
	@Inject
	private InventoryUtils inventory;
	@Inject
	private InterfaceUtils interfaceUtils;
	@Inject
	private CalculationUtils calc;
	@Inject
	private MenuUtils menu;
	@Inject
	private ObjectUtils object;
	@Inject
	private BankUtils bank;
	@Inject
	private NPCUtils npc;
	@Inject
	private leaguesZMIConfig config;
	@Inject
	PluginManager pluginManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	leaguesZMIOverlay overlay;
	@Inject
	private ChinBreakHandler chinBreakHandler;
	leaguesZMIState state;
	Instant botTimer;
	MenuEntry targetMenu;
	Player player;
	int timeout = 0;
	long sleepLength = 0L;
	boolean startZMI;
	public static Set<Integer> RUNES = Set.of(
		ItemID.AIR_RUNE, ItemID.WATER_RUNE, ItemID.EARTH_RUNE, ItemID.FIRE_RUNE,
		ItemID.MIND_RUNE, ItemID.CHAOS_RUNE, ItemID.DEATH_RUNE, ItemID.BLOOD_RUNE,
		ItemID.BODY_RUNE, ItemID.COSMIC_RUNE, ItemID.LAW_RUNE, ItemID.NATURE_RUNE);
	public static Set<Integer> STAVES = Set.of(
		ItemID.MYSTIC_AIR_STAFF, ItemID.STAFF_OF_AIR, ItemID.AIR_BATTLESTAFF,
		ItemID.SMOKE_BATTLESTAFF, ItemID.MYSTIC_SMOKE_STAFF, ItemID.MIST_BATTLESTAFF,
		ItemID.MYSTIC_MIST_STAFF, ItemID.DUST_BATTLESTAFF, ItemID.MYSTIC_DUST_STAFF);
	public static final int skillingRegionID = 12119;

	public leaguesZMIPlugin()
	{
	}

	protected void startUp()
	{
		chinBreakHandler.registerPlugin(this);
	}

	protected void shutDown()
	{
		resetVals();
		chinBreakHandler.unregisterPlugin(this);
	}

	public void resetVals()
	{
		overlayManager.remove(overlay);
		chinBreakHandler.stopPlugin(this);
		startZMI = false;
		botTimer = null;
		timeout = 0;
		targetMenu = null;
	}

	@Provides
	leaguesZMIConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(leaguesZMIConfig.class);
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (configButtonClicked.getGroup().equalsIgnoreCase("leaguesZMI"))
		{
			String var2 = configButtonClicked.getKey();
			byte var3 = -1;
			switch (var2.hashCode())
			{
				case 1943111220:
					if (var2.equals("startButton"))
					{
						var3 = 0;
					}
				default:
					switch (var3)
					{
						case 0:
							if (!startZMI)
							{
								startZMI = true;
								chinBreakHandler.startPlugin(this);
								botTimer = Instant.now();
								state = null;
								targetMenu = null;
								timeout = 0;
								overlayManager.add(overlay);
								initVals();
							}
							else
							{
								resetVals();
							}
						default:
					}
			}
		}
	}

	public void initVals()
	{
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup() == "leaguesZMI")
		{
		}
	}

	private long sleepDelay()
	{
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private void teleportCrystal()
	{
		targetMenu = new MenuEntry("", "", 25104, 33, inventory.getWidgetItem(25104).getIndex(), 9764864, false);
		menu.setEntry(targetMenu);
		mouse.delayMouseClick(inventory.getWidgetItem(25104).getCanvasBounds(), sleepDelay());
	}

	private void teleportBank()
	{
		switch (config.banks())
		{
			case VER_SINHAZA:
				targetMenu = new MenuEntry("", "", 2, MenuOpcode.CC_OP.getId(), -1, 25362448, false);
				break;
			case CRAFTING_GUILD:
				targetMenu = new MenuEntry("", "", 3, MenuOpcode.CC_OP.getId(), -1, 25362447, false);
				break;
			case SEERS:
				if (playerUtils.isItemEquipped(STAVES))
				{
					targetMenu = new MenuEntry("", "", 2, MenuOpcode.CC_OP.getId(), -1, WidgetInfo.SPELL_CAMELOT_TELEPORT.getId(), false);
				}
				else
				{
					utils.sendGameMessage("You must equip an infinite source of air runes.");
					startZMI = false;
				}
				break;
		}

		menu.setEntry(targetMenu);
		mouse.delayClickRandomPointCenter(100, 100, sleepDelay());
	}

	private void craft()
	{
		GameObject zmi = object.findNearestGameObject(ObjectID.RUNECRAFTING_ALTAR);
		if (zmi != null)
		{
			targetMenu = new MenuEntry("", "", zmi.getId(), 3, zmi.getLocalLocation().getSceneX() - 1, zmi.getLocalLocation().getSceneY() - 1, false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(zmi.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			utils.sendGameMessage("ZMI altar not found.");
			startZMI = false;
		}

	}

	private void openBank()
	{
		GameObject bankTarget;
		switch (config.banks())

		{
			case VER_SINHAZA:
				bankTarget = object.findNearestGameObjectWithin(Banks.VER_SINHAZA.getBankLoc(), 0, Banks.VER_SINHAZA.getBankObjID());

				if (bankTarget != null)
				{
					targetMenu =
						new MenuEntry("", "", bankTarget.getId(), bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(), bankTarget.getSceneMinLocation().getY(),
							false);
					menu.setEntry(targetMenu);
					mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), sleepDelay());
				}

				break;


			case CRAFTING_GUILD:
				bankTarget = object.findNearestGameObject(Banks.CRAFTING_GUILD.getBankObjID());

				if (bankTarget != null)
				{
					targetMenu =
						new MenuEntry("", "", bankTarget.getId(), bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(), bankTarget.getSceneMinLocation().getY(),
							false);
					menu.setEntry(targetMenu);
					mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), sleepDelay());
				}

				break;


			case SEERS:
				bankTarget = object.findNearestGameObject(Banks.SEERS.getBankObjID());

				if (bankTarget != null)
				{
					targetMenu =
						new MenuEntry("", "", bankTarget.getId(), bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(), bankTarget.getSceneMinLocation().getY(),
							false);
					menu.setEntry(targetMenu);
					mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), sleepDelay());
				}

				break;
		}
	}

	private void withdrawEss()
	{
		if (bank.contains(ItemID.DAEYALT_ESSENCE, 27))
		{
			bank.withdrawAllItem(ItemID.DAEYALT_ESSENCE);
		}
		else
		{
			bank.withdrawAllItem(ItemID.DAEYALT_ESSENCE);
		}

		timeout = 0 + tickDelay();
	}

	public leaguesZMIState getState()
	{
		if (timeout > 0)
		{
			playerUtils.handleRun(20, 30);
			return leaguesZMIState.TIMEOUT;
		}
		else if (player.getPoseAnimation() != 819 && player.getPoseAnimation() != 824 && player.getPoseAnimation() != 1205 && player.getPoseAnimation() != 1210)
		{
			if (!bank.isOpen())
			{
				if (!inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CRAFTING_GUILD.getRegionID() ||
					!inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.VER_SINHAZA.getRegionID() ||
					!inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.SEERS.getRegionID()
				)
				{
					return leaguesZMIState.OPEN_BANK;
				}

				if (inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.CRAFTING_GUILD.getRegionID() ||
					inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.VER_SINHAZA.getRegionID() ||
					inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == Banks.SEERS.getRegionID()
				)
				{
					return leaguesZMIState.TELEPORT_CRYSTAL;
				}

				if (!inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == skillingRegionID)
				{
					return leaguesZMIState.TELEPORT_BANK;
				}

				if (inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() == skillingRegionID)
				{
					return leaguesZMIState.CRAFT;
				}
			}

			if (bank.isOpen())
			{
				if (!bank.contains(ItemID.PURE_ESSENCE, 27))
				{
					utils.sendGameMessage("get more pure essence");
					return leaguesZMIState.OUT_OF_ESSENCE;
				}

				if (inventory.containsItem(RUNES))
				{
					return leaguesZMIState.DEPOSIT_ALL;
				}

				if (!inventory.isFull() && !inventory.containsItem(RUNES))
				{
					return leaguesZMIState.WITHDRAW_ESSENCE;
				}

				if (inventory.isFull())
				{
					return leaguesZMIState.CLOSE_BANK;
				}

				if (chinBreakHandler.shouldBreak(this))
				{
					return leaguesZMIState.HANDLE_BREAK;
				}
			}

			return leaguesZMIState.IDLING;
		}
		else
		{
			return leaguesZMIState.MOVING;
		}
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (startZMI && !chinBreakHandler.isBreakActive(this))
		{
			player = client.getLocalPlayer();
			if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN)
			{
				if (!client.isResized())
				{
					utils.sendGameMessage("client must be set to resizable");
					startZMI = false;
					return;
				}

				playerUtils.handleRun(40, 20);
				state = getState();
				switch (state)
				{
					case TIMEOUT:
						--timeout;
					case ITERATING:
					default:
						break;
					case IDLING:
						timeout = 1;
						break;
					case MOVING:
						timeout = 1;
						break;
					case OPEN_BANK:
						openBank();
						timeout = tickDelay();
						break;
					case CLOSE_BANK:
						bank.close();
						timeout = tickDelay();
						break;
					case CRAFT:
						craft();
						timeout = tickDelay();
						break;
					case TELEPORT_CRYSTAL:
						teleportCrystal();
						timeout = 3 + tickDelay();
						break;
					case TELEPORT_BANK:
						teleportBank();
						timeout = 3 + tickDelay();
						break;
					case DEPOSIT_ALL:
						bank.depositAll();
						timeout = tickDelay();
						break;
					case WITHDRAW_ESSENCE:
						withdrawEss();
						break;
					case OUT_OF_ESSENCE:
						if (config.logout())
						{
							interfaceUtils.logout();
						}

						startZMI = false;
						resetVals();
						break;
					case HANDLE_BREAK:
						chinBreakHandler.startBreak(this);
						timeout = 8;
				}
			}

		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (startZMI)
		{
			if (event.getGameState() == GameState.LOGGED_IN)
			{
				state = leaguesZMIState.IDLING;
				timeout = 2;
			}

		}
	}
}

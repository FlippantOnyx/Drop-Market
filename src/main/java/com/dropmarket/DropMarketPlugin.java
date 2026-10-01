package com.dropmarket;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemPrice;

@Slf4j
@PluginDescriptor(
	name = "Drop Market",
	description = "A tiny prediction-market graph betting on your next big drop",
	tags = {"drop", "loot", "odds", "graph", "market", "raids", "fun"}
)
public class DropMarketPlugin extends Plugin
{
	static final String LIVE_ODDS = "LIVE ODDS";
	private static final String UNKNOWN_SOURCE = "Collection log";
	private static final int LOOT_SOURCE_TICKS = 5;
	private static final int UPDATE_TICKS = 5;
	private static final int POLY_PHASE = 2;
	private static final int IDLE_TICKS = 50;
	// Rock/tree respawns and moving between fishing spots leave bigger gaps than combat idling does.
	private static final int SKILL_IDLE_TICKS = 150;
	private static final int CELEBRATION_TICKS = 100;
	private static final int VOLUME_POP_TICKS = 8;
	private static final double VOLUME_POP_CHANCE = 0.3;
	private static final long VOLUME_POP_MIN = 100_000L;
	private static final long VOLUME_POP_MAX = 2_147_483_647L;
	// Clue steps involve a lot of running around, so the clue market outlives normal idling.
	private static final int CLUE_IDLE_TICKS = 500;
	private static final String THIRD_AGE = "3rd age";
	private static final String GUARD_ITEM = "Armadyl helmet";
	private static final String GUARD_DISPLAY = "Armadylean helmet";
	private static final Set<String> GUARD_NAMES = ImmutableSet.of("armadylean guard", "bandosian guard");
	private static final String CLOG_MESSAGE = "New item added to your collection log:";
	private static final Set<Integer> TOA_REGIONS = ImmutableSet.of(
		14160, 15698, 15700, 14162, 14164, 15186, 15188, 14674, 14676, 15184, 15696, 14672);
	private static final int WINTERTODT_REGION = 6461;
	private static final int TEMPOROSS_REGION = 12588;
	private static final int GOTR_REGION = 14484;
	private static final Map<Skill, String> SKILL_MARKETS = ImmutableMap.<Skill, String>builder()
		.put(Skill.MINING, "Mining")
		.put(Skill.WOODCUTTING, "Woodcutting")
		.put(Skill.FISHING, "Fishing")
		.put(Skill.AGILITY, "Agility")
		.put(Skill.THIEVING, "Thieving")
		.put(Skill.FARMING, "Farming")
		.put(Skill.RUNECRAFT, "Runecraft")
		.put(Skill.HUNTER, "Hunter")
		.put(Skill.SAILING, "Sailing")
		.build();
	// The badge counter's unit for markets driven by training rather than kills/loot.
	private static final Map<String, String> SKILL_UNITS = ImmutableMap.<String, String>builder()
		.put("Mining", "rocks")
		.put("Woodcutting", "logs")
		.put("Fishing", "catches")
		.put("Agility", "laps")
		.put("Thieving", "attempts")
		.put("Farming", "harvests")
		.put("Runecraft", "runs")
		.put("Hunter", "catches")
		.put("Sailing", "voyages")
		.put(DropTables.HERBIBOAR, "harvests")
		.build();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DropMarketConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DropMarketOverlay overlay;

	@Inject
	private ItemManager itemManager;

	@Inject
	private WikiDropTableClient wiki;

	@Inject
	private CollectionLogTracker clog;

	@Getter
	private final MarketGraph graph = new MarketGraph(new Random());

	@Getter
	private final MarketGraph polyGraph = new MarketGraph(new Random());

	@Getter
	private String title = LIVE_ODDS;

	@Getter
	private boolean resolved;

	@Getter
	private String badgeText = "";

	@Getter
	private boolean badgeSkull;

	// The % gained at the moment of victory, frozen until the market resets (the graph itself keeps
	// pinning to 100 every tick afterward, which would otherwise wash the delta back down to 0).
	@Getter
	private int resolvedDelta;

	private final Random random = new Random();
	private long volumePop;
	private int volumePopTick;
	private final Map<String, Long> priceCache = new HashMap<>();
	private final Map<String, Integer> killCounts = new HashMap<>();
	private final Map<String, Integer> skillCounts = new HashMap<>();
	private Map<String, String> priorities = Collections.emptyMap();
	@Getter
	private int ticks;
	private int loginTick;
	private int lastCombatTick;
	private String targetSource;
	// Last target with a known drop table, so boss adds (bodyguards, snakelings) don't hijack the market.
	private String notableSource;
	private int lastNotableTick;
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private String skillSource;
	private int lastSkillTick;
	private int lastHunterXpTick;
	private int lastHerbloreXpTick;
	private boolean corruptedGauntlet;
	private Set<Integer> trailItemIds = Collections.emptySet();
	private int lastClueProgressTick;
	private String guardSource;
	private int lastGuardTick;
	private String runeProfileFetchedFor;
	private String lastClogPage;
	// Prices load asynchronously after startup; markets opened before then get re-picked once they arrive.
	private boolean pickedWithoutPrices;
	// The last opened market; kept while idle so the same monster resumes it and late loot still counts.
	private String marketSource;
	private String marketItem;
	private boolean showingMarket;
	private int resolvedTick;
	private String lastLootSource;
	private int lastLootTick;
	// A collection log / valuable drop celebration whose source isn't known yet, shown as "[PLAYER] GETS [ITEM]".
	private boolean personalOverride;

	@Override
	protected void startUp()
	{
		priorities = DropTables.parsePriorities(config.priorityItems());
		ticks = 0;
		loginTick = 0;
		killCounts.clear();
		skillCounts.clear();
		lastCombatTick = -IDLE_TICKS - 1;
		lastNotableTick = -IDLE_TICKS - 1;
		lastSkillTick = -SKILL_IDLE_TICKS - 1;
		lastHunterXpTick = -1;
		lastHerbloreXpTick = -1;
		targetSource = null;
		notableSource = null;
		skillSource = null;
		corruptedGauntlet = false;
		lastXp.clear();
		trailItemIds = Collections.emptySet();
		lastClueProgressTick = -CLUE_IDLE_TICKS - 1;
		guardSource = null;
		lastGuardTick = -IDLE_TICKS - 1;
		runeProfileFetchedFor = null;
		lastClogPage = null;
		pickedWithoutPrices = false;
		marketSource = null;
		marketItem = null;
		showingMarket = false;
		resolved = false;
		resolvedDelta = 0;
		personalOverride = false;
		volumePop = 0;
		volumePopTick = -VOLUME_POP_TICKS - 1;
		lastLootSource = null;
		lastLootTick = -LOOT_SOURCE_TICKS - 1;
		priceCache.clear();
		graph.reset();
		polyGraph.reset();
		updateTitle();
		overlayManager.add(overlay);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(clog::load);
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		ticks++;
		trackInteracting();
		scanCollectionLog();
		maybeFetchRuneProfile();

		if (pickedWithoutPrices && pricesLoaded() && marketSource != null && !resolved)
		{
			pickedWithoutPrices = false;
			marketItem = chooseItem(marketSource);
		}

		String source;
		if (guardSource != null && ticks - lastGuardTick <= IDLE_TICKS)
		{
			source = guardSource;
		}
		else if (!trailItemIds.isEmpty() && ticks - lastClueProgressTick <= CLUE_IDLE_TICKS)
		{
			source = DropTables.TREASURE_TRAILS;
		}
		else
		{
			source = activitySource();
			if (source == null)
			{
				source = recentSource();
			}
		}

		if (resolved)
		{
			boolean sameMonster = source != null && source.equals(marketSource);
			boolean differentMonster = source != null && !sameMonster;
			if (differentMonster || (ticks - resolvedTick >= CELEBRATION_TICKS && !sameMonster))
			{
				resolved = false;
				personalOverride = false;
				marketSource = null;
				marketItem = null;
				graph.reset();
				polyGraph.reset();
			}
			else
			{
				source = marketSource;
			}
		}

		if (source == null)
		{
			showingMarket = false;
		}
		else if (!source.equals(marketSource))
		{
			openMarket(source);
		}
		else
		{
			showingMarket = true;
		}
		updateTitle();
		updateBadge();

		if (ticks % UPDATE_TICKS == 0)
		{
			if (resolved)
			{
				graph.pinToCertain();
			}
			else
			{
				graph.step();
			}
		}
		if (ticks % UPDATE_TICKS == 0 && ticks - volumePopTick > VOLUME_POP_TICKS
			&& random.nextDouble() < VOLUME_POP_CHANCE)
		{
			double span = Math.log((double) VOLUME_POP_MAX / VOLUME_POP_MIN);
			volumePop = (long) (VOLUME_POP_MIN * Math.exp(random.nextDouble() * span));
			volumePopTick = ticks;
			graph.addVolume(volumePop);
		}
		if (ticks % UPDATE_TICKS == POLY_PHASE)
		{
			if (resolved)
			{
				polyGraph.pinToCertain();
			}
			else
			{
				polyGraph.step();
			}
		}
	}

	private String recentSource()
	{
		String combat = null;
		if (notableSource != null && ticks - lastNotableTick <= IDLE_TICKS)
		{
			combat = notableSource;
		}
		else if (targetSource != null && ticks - lastCombatTick <= IDLE_TICKS)
		{
			combat = targetSource;
		}
		boolean skilling = skillSource != null && ticks - lastSkillTick <= SKILL_IDLE_TICKS;
		if (skilling && (combat == null || lastSkillTick > lastCombatTick))
		{
			return skillSource;
		}
		return combat;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		Integer previous = lastXp.put(skill, event.getXp());
		if (previous == null || event.getXp() <= previous)
		{
			return;
		}

		if (skill == Skill.HUNTER)
		{
			lastHunterXpTick = ticks;
		}
		else if (skill == Skill.HERBLORE)
		{
			lastHerbloreXpTick = ticks;
		}

		String market;
		if (lastHunterXpTick == ticks && lastHerbloreXpTick == ticks)
		{
			// Harvesting the herbiboar is the only thing granting Hunter and Herblore XP together.
			market = DropTables.HERBIBOAR;
		}
		else if (skill == Skill.HUNTER && DropTables.HERBIBOAR.equals(skillSource)
			&& ticks - lastSkillTick <= SKILL_IDLE_TICKS)
		{
			market = DropTables.HERBIBOAR;
		}
		else
		{
			market = SKILL_MARKETS.get(skill);
		}

		if (market != null)
		{
			skillSource = market;
			lastSkillTick = ticks;
			if (SKILL_UNITS.containsKey(market))
			{
				skillCounts.merge(market, 1, Integer::sum);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			lastXp.clear();
			runeProfileFetchedFor = null;
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			loginTick = ticks;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		clog.load();
		runeProfileFetchedFor = null;
		repickMarket();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}
		Set<Integer> ids = new HashSet<>();
		for (Item item : event.getItemContainer().getItems())
		{
			if (item.getId() > 0 && DropTables.isTrailItem(itemManager.getItemComposition(item.getId()).getName()))
			{
				ids.add(item.getId());
			}
		}
		// A clue turning into the next step, a casket, etc. means it's being worked on; merely picking one up doesn't.
		if (!ids.isEmpty() && !trailItemIds.isEmpty() && !ids.equals(trailItemIds))
		{
			lastClueProgressTick = ticks;
		}
		trailItemIds = ids;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (trailItemIds.isEmpty())
		{
			return;
		}
		String option = event.getMenuOption();
		String target = Text.removeTags(event.getMenuTarget()).toLowerCase();
		boolean readsClue = "Read".equals(option) && (target.contains("clue scroll") || target.contains("challenge scroll"));
		boolean opensCasket = "Open".equals(option) && target.contains("casket");
		if (readsClue || opensCasket || "Dig".equals(option))
		{
			lastClueProgressTick = ticks;
		}
	}

	private void scanCollectionLog()
	{
		Widget items = clogItemGrid();
		if (items == null)
		{
			return;
		}
		StringBuilder page = new StringBuilder();
		for (Widget w : items.getDynamicChildren())
		{
			page.append(w.getItemId()).append(':').append(w.getOpacity()).append(',');
		}
		String signature = page.toString();
		if (signature.equals(lastClogPage))
		{
			return;
		}
		lastClogPage = signature;

		boolean changed = false;
		for (Widget w : items.getDynamicChildren())
		{
			if (w.getItemId() > 0)
			{
				// Unlocked slots draw fully opaque; locked ones are faded.
				changed |= clog.record(itemManager.getItemComposition(w.getItemId()).getName(), w.getOpacity() == 0);
			}
		}
		if (changed)
		{
			clog.flush();
			repickMarket();
		}
	}

	private Widget clogItemGrid()
	{
		for (int id : new int[]{InterfaceID.Collection.ITEMS, InterfaceID.Collection.ITEMS_CONTENTS})
		{
			Widget w = client.getWidget(id);
			if (w != null && !w.isHidden() && w.getDynamicChildren() != null && w.getDynamicChildren().length > 0)
			{
				return w;
			}
		}
		return null;
	}

	private void maybeFetchRuneProfile()
	{
		Player player = client.getLocalPlayer();
		String name = player == null ? null : player.getName();
		if (!config.runeProfileSync() || name == null || name.equals(runeProfileFetchedFor))
		{
			return;
		}
		runeProfileFetchedFor = name;
		clog.fetchRuneProfile(name, this::repickMarket);
	}

	private void repickMarket()
	{
		if (marketSource != null && !resolved)
		{
			marketItem = chooseItem(marketSource);
			updateTitle();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getHitsplat().isMine() && event.getActor() instanceof NPC)
		{
			markCombat((NPC) event.getActor());
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		checkLoot(event.getItems());
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		checkLoot(event.getItems());
		if (event.getName() != null)
		{
			String source = DropTables.canonicalSource(event.getName());
			if (DropTables.isCompletion(source, event.getName()))
			{
				killCounts.merge(source, 1, Integer::sum);
			}
			lastLootSource = source;
			lastLootTick = ticks;
			if (resolved && personalOverride && ticks - resolvedTick <= LOOT_SOURCE_TICKS)
			{
				personalOverride = false;
				marketSource = source;
				updateTitle();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		switch (event.getType())
		{
			case GAMEMESSAGE:
			case FRIENDSCHATNOTIFICATION:
			case CLAN_MESSAGE:
			case CLAN_GIM_MESSAGE:
				break;
			default:
				return;
		}

		String raw = Text.removeTags(event.getMessage());
		if (event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			if (raw.startsWith(CLOG_MESSAGE))
			{
				String item = raw.substring(CLOG_MESSAGE.length()).trim();
				clog.recordAndFlush(item, true);
				celebrate(item);
			}
			else if (raw.regionMatches(true, 0, "Valuable drop:", 0, 14)
				|| raw.regionMatches(true, 0, "Untradeable drop:", 0, 17))
			{
				celebrate(parseDropItem(raw));
			}
		}

		if (marketItem == null || resolved)
		{
			return;
		}

		String item = DropTables.normalize(marketItem);
		String msg = DropTables.normalize(raw);

		if (DropTables.isPet(marketItem)
			&& (msg.startsWith("you have a funny feeling like") || msg.startsWith("you feel something weird sneaking")))
		{
			resolve();
			return;
		}

		if (!containsWords(msg, item))
		{
			return;
		}

		if (msg.startsWith("valuable drop")
			|| msg.startsWith("untradeable drop")
			|| msg.startsWith("new item added to your collection log")
			|| mentionsLocalPlayer(msg))
		{
			resolve();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!DropMarketConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		priorities = DropTables.parsePriorities(config.priorityItems());
		repickMarket();
	}

	private void trackInteracting()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		Actor target = player.getInteracting();
		// Requiring the NPC to target us back skips pickpocketing, which would otherwise beat Thieving XP.
		if (target instanceof NPC && target.getInteracting() == player && isAttackable((NPC) target))
		{
			markCombat((NPC) target);
		}
	}

	private static boolean isAttackable(NPC npc)
	{
		NPCComposition comp = npc.getTransformedComposition();
		if (comp == null)
		{
			return false;
		}
		if (comp.getCombatLevel() > 0)
		{
			return true;
		}
		for (String action : comp.getActions())
		{
			if ("Attack".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	private void markCombat(NPC npc)
	{
		String name = npc.getName();
		if (name == null)
		{
			return;
		}
		lastCombatTick = ticks;
		String cleanName = Text.removeTags(name).replace(' ', ' ');
		if (GUARD_NAMES.contains(DropTables.normalize(cleanName)))
		{
			guardSource = cleanName;
			lastGuardTick = ticks;
		}
		if (cleanName.startsWith("Corrupted"))
		{
			corruptedGauntlet = true;
		}
		else if (cleanName.startsWith("Crystalline"))
		{
			corruptedGauntlet = false;
		}
		targetSource = DropTables.canonicalSource(cleanName);
		if (DropTables.lookup(targetSource) != null || priorities.containsKey(DropTables.normalize(targetSource)))
		{
			notableSource = targetSource;
			lastNotableTick = ticks;
		}
	}

	private String activitySource()
	{
		if (client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1)
		{
			return DropTables.CHAMBERS_OF_XERIC;
		}
		// 1 = in party outside, 2 = inside/spectating, 3 = dead spectating
		if (client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS) >= 2)
		{
			return DropTables.THEATRE_OF_BLOOD;
		}
		if (client.getVarbitValue(VarbitID.PLAYER_IN_GAUNTLET) == 1)
		{
			return corruptedGauntlet ? DropTables.CORRUPTED_GAUNTLET : DropTables.THE_GAUNTLET;
		}

		Player player = client.getLocalPlayer();
		LocalPoint lp = player == null ? null : player.getLocalLocation();
		if (lp == null)
		{
			return null;
		}
		int region = WorldPoint.fromLocalInstance(client, lp).getRegionID();
		if (TOA_REGIONS.contains(region))
		{
			return DropTables.TOMBS_OF_AMASCUT;
		}
		switch (region)
		{
			case WINTERTODT_REGION:
				return DropTables.WINTERTODT;
			case TEMPOROSS_REGION:
				return DropTables.TEMPOROSS;
			case GOTR_REGION:
				return DropTables.GUARDIANS_OF_THE_RIFT;
			default:
				return null;
		}
	}

	private void openMarket(String source)
	{
		marketSource = source;
		marketItem = chooseItem(source);
		showingMarket = true;
		graph.reset();
		polyGraph.reset();
	}

	private boolean isPersonalMarket(String source)
	{
		return DropTables.TREASURE_TRAILS.equals(source) || source.equals(guardSource);
	}

	/** Returns null when nothing is worth betting on, which shows LIVE ODDS. */
	private String chooseItem(String source)
	{
		List<String> table = DropTables.lookup(source);
		String priority = priorities.get(DropTables.normalize(source));
		if (priority != null)
		{
			return DropTables.matchItem(priority, table);
		}
		if (DropTables.TREASURE_TRAILS.equals(source))
		{
			return THIRD_AGE;
		}
		if (source.equals(guardSource))
		{
			return GUARD_ITEM;
		}

		boolean fromWiki = false;
		if (table == null && config.wikiLookup())
		{
			table = wiki.getCached(source);
			fromWiki = true;
			if (table == null)
			{
				wiki.fetch(source, drops ->
				{
					if (source.equals(marketSource) && marketItem == null && !resolved)
					{
						marketItem = pickItem(source, drops, true);
						updateTitle();
					}
				});
			}
		}
		if (!pricesLoaded())
		{
			pickedWithoutPrices = true;
		}
		return table == null ? null : pickItem(source, table, fromWiki);
	}

	private String pickItem(String source, List<String> table, boolean fromWiki)
	{
		return MarketPicker.pick(table, DropTables.isPetFirst(source), config.minimumDropValue(), this::priceOf,
			item -> isLockedSlot(item, fromWiki));
	}

	/** An untradeable collection log slot this account hasn't unlocked (clue scrolls never count). */
	private boolean isLockedSlot(String item, boolean fromWiki)
	{
		if (DropTables.isClueItem(item) || clog.isObtained(item))
		{
			return false;
		}
		if (clog.isSlot(item))
		{
			return true;
		}
		// Without a full log, trust that curated tables only list collection log items; wiki tables don't qualify.
		return !fromWiki && !clog.hasFullLog();
	}

	private boolean pricesLoaded()
	{
		return !itemManager.search("Coins").isEmpty();
	}

	/** GE price, or -1 if the item isn't on the GE (untradeable). */
	private long priceOf(String name)
	{
		String key = name.toLowerCase();
		Long cached = priceCache.get(key);
		if (cached != null)
		{
			return cached;
		}
		if (!pricesLoaded())
		{
			return -1;
		}
		long price = -1;
		for (ItemPrice ip : itemManager.search(name))
		{
			if (ip.getName().equalsIgnoreCase(name))
			{
				price = itemManager.getItemPrice(ip.getId());
				break;
			}
		}
		priceCache.put(key, price);
		return price;
	}

	private boolean matchesMarketItem(String itemName)
	{
		String n = DropTables.normalize(itemName);
		if (THIRD_AGE.equals(marketItem))
		{
			return n.startsWith(THIRD_AGE);
		}
		return n.equals(DropTables.normalize(marketItem));
	}

	private void checkLoot(Collection<ItemStack> items)
	{
		boolean hit = false;
		boolean clogChanged = false;
		for (ItemStack stack : items)
		{
			String name = itemManager.getItemComposition(itemManager.canonicalize(stack.getId())).getName();
			boolean isTarget = marketItem != null && matchesMarketItem(name);
			if (clog.isSlot(name) || (isTarget && priceOf(name) < 0))
			{
				clogChanged |= clog.record(name, true);
			}
			hit |= isTarget;
		}
		if (clogChanged)
		{
			clog.flush();
		}
		if (hit && !resolved)
		{
			resolve();
		}
	}

	private void resolve()
	{
		resolvedDelta = (int) Math.round(100 - graph.current());
		resolved = true;
		resolvedTick = ticks;
		showingMarket = true;
		graph.pinToCertain();
		polyGraph.pinToCertain();
		updateTitle();
	}

	/** A collection log slot or valuable drop overrides whatever the ticker was showing. */
	private void celebrate(String item)
	{
		String source = ticks - lastLootTick <= LOOT_SOURCE_TICKS ? lastLootSource : null;
		if (source == null && showingMarket)
		{
			source = marketSource;
		}
		personalOverride = source == null;
		marketSource = source == null ? UNKNOWN_SOURCE : source;
		marketItem = item;
		resolve();
	}

	private static String parseDropItem(String message)
	{
		String item = message.substring(message.indexOf(':') + 1).trim();
		item = item.replaceFirst("^[\\d,]+ x ", "");
		return item.replaceFirst("\\s*\\([\\d,]+ coins\\)$", "").trim();
	}

	private boolean mentionsLocalPlayer(String normalizedMessage)
	{
		Player player = client.getLocalPlayer();
		String name = player == null ? null : player.getName();
		return name != null && containsWords(normalizedMessage, DropTables.normalize(name));
	}

	private static boolean containsWords(String haystack, String needle)
	{
		return !needle.isEmpty() && (" " + haystack + " ").contains(" " + needle + " ");
	}

	private void updateTitle()
	{
		if (!showingMarket || marketSource == null || marketItem == null)
		{
			title = LIVE_ODDS;
			return;
		}
		if (personalOverride)
		{
			Player player = client.getLocalPlayer();
			String name = player == null || player.getName() == null ? "You" : player.getName();
			title = name + " GETS " + DropTables.displayName(marketItem);
			return;
		}
		if (isPersonalMarket(marketSource))
		{
			String item = THIRD_AGE.equals(marketItem) ? "Third age"
				: GUARD_ITEM.equals(marketItem) ? GUARD_DISPLAY
				: DropTables.displayName(marketItem);
			Player player = client.getLocalPlayer();
			String name = player == null || player.getName() == null ? "You" : player.getName();
			title = name + " GETS " + item;
			return;
		}
		title = marketSource + " DROPS " + DropTables.displayName(marketItem);
	}

	/** Amount of the volume bump currently on screen, or 0. */
	long getVolumePop()
	{
		return ticks - volumePopTick <= VOLUME_POP_TICKS ? volumePop : 0;
	}

	private void updateBadge()
	{
		if (marketSource == null)
		{
			badgeSkull = false;
			badgeText = formatElapsed(ticks - loginTick);
			return;
		}
		String unit = SKILL_UNITS.get(marketSource);
		if (unit != null)
		{
			badgeSkull = false;
			badgeText = skillCounts.getOrDefault(marketSource, 0) + " " + unit;
		}
		else
		{
			badgeSkull = true;
			badgeText = killCounts.getOrDefault(marketSource, 0) + " KC";
		}
	}

	private static String formatElapsed(int elapsedTicks)
	{
		int totalSeconds = (int) Math.round(Math.max(0, elapsedTicks) * 0.6);
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds)
			: String.format("%d:%02d", minutes, seconds);
	}

	@Provides
	DropMarketConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DropMarketConfig.class);
	}
}

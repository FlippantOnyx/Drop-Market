package com.dropmarket;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameObject;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.item.ItemPrice;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;

/**
 * Runs the real plugin, wired by the real Guice graph, against stand-ins for the game. The unit tests only cover the
 * pure rules, which is how a package-private config enum slipped through; this is the test that sees the plugin the way
 * RuneLite does. In the normal runs nothing may fail inside the guard; in the hostile runs the game hands over nulls or
 * throws, and nothing may escape to the caller.
 */
public class PluginIntegrationTest
{
	private enum Mode
	{
		NORMAL, NULLS, THROWING
	}

	/** Creates an instance without running a constructor, for RuneLite classes that need a whole client to build. */
	@SuppressWarnings("unchecked")
	private static <T> T allocate(Class<T> type) throws Exception
	{
		Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return (T) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type);
	}

	// The constructors below never run (allocate skips them); they only satisfy the compiler.
	private static class StubClientThread extends ClientThread
	{
		@Override
		public void invoke(Runnable r)
		{
			r.run();
		}

		@Override
		public void invokeLater(Runnable r)
		{
			r.run();
		}
	}

	/** The overlay manager can't be stubbed (private constructor); the real one is never asked to do anything real. */
	private static final java.util.Set<String> OVERLAY_PLACES = new java.util.HashSet<>(
		Arrays.asList("startUp overlay", "shutDown overlay"));

	private static class StubProfileStore extends ProfileStore
	{
		private final Map<String, String> values = new HashMap<>();
		private final Map<String, String> settings = new HashMap<>();

		StubProfileStore()
		{
			super(null);
		}

		@Override
		String get(String key)
		{
			return values.get(key);
		}

		@Override
		void set(String key, String value)
		{
			values.put(key, value);
		}

		@Override
		String getSetting(String key)
		{
			return settings.get(key);
		}

		@Override
		void setSetting(String key, String value)
		{
			settings.put(key, value);
		}
	}

	private static class StubItemLookup extends ItemLookup
	{
		private final Mode mode;

		StubItemLookup(Mode mode)
		{
			super(null);
			this.mode = mode;
		}

		@Override
		String nameOf(int id)
		{
			return mode == Mode.NULLS ? "" : NAMES[Math.floorMod(id, NAMES.length)];
		}

		@Override
		String canonicalNameOf(int id)
		{
			return nameOf(id);
		}

		@Override
		List<ItemPrice> search(String name)
		{
			return mode == Mode.NORMAL ? Collections.singletonList(price(name)) : Collections.emptyList();
		}

		@Override
		long priceOf(int id)
		{
			return mode == Mode.NORMAL ? id * 1_000L : 0;
		}

		private static ItemPrice price(String name)
		{
			ItemPrice price = new ItemPrice();
			price.setName(name);
			price.setId(name.length());
			return price;
		}
	}

	private static final String[] NAMES = {"Tanzanite fang", "Dragon bones", "Coins", "Scythe of vitur", "Emerald",
		"Pet snakeling", "Abyssal whip", "Uncut diamond"};

	private interface Answer
	{
		Object apply(java.lang.reflect.Method method, Object[] args) throws Throwable;
	}

	@SuppressWarnings("unchecked")
	private static <T> T stub(Class<T> type, Answer answer)
	{
		InvocationHandler handler = (proxy, method, args) ->
		{
			if (method.getDeclaringClass() == Object.class)
			{
				return method.getName().equals("hashCode") ? 1 : method.getName().equals("equals") ? proxy == args[0] : "stub";
			}
			return answer.apply(method, args);
		};
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
	}

	private static Object defaultFor(Class<?> type)
	{
		if (type == boolean.class)
		{
			return false;
		}
		if (type == int.class || type == short.class || type == byte.class)
		{
			return 0;
		}
		if (type == long.class)
		{
			return 0L;
		}
		if (type == double.class)
		{
			return 0d;
		}
		if (type == float.class)
		{
			return 0f;
		}
		return null;
	}

	private static NPC npc(String name, int id)
	{
		return stub(NPC.class, (m, args) ->
		{
			switch (m.getName())
			{
				case "getName":
					return name;
				case "getId":
					return id;
				case "getCombatLevel":
					return 500;
				default:
					return defaultFor(m.getReturnType());
			}
		});
	}

	private static Client client(Mode mode)
	{
		Player player = stub(Player.class, (m, args) ->
		{
			switch (m.getName())
			{
				case "getName":
					return "Taiki Shuttle";
				case "getWorldLocation":
					return new WorldPoint(3000, 3000, 0);
				default:
					return defaultFor(m.getReturnType());
			}
		});
		return stub(Client.class, (m, args) ->
		{
			if (mode == Mode.THROWING && !m.getName().equals("toString"))
			{
				throw new IllegalStateException("client not ready: " + m.getName());
			}
			if (mode == Mode.NULLS)
			{
				return defaultFor(m.getReturnType());
			}
			switch (m.getName())
			{
				case "getGameState":
					return GameState.LOGGED_IN;
				case "getLocalPlayer":
					return player;
				case "getTickCount":
					return 100;
				default:
					return defaultFor(m.getReturnType());
			}
		});
	}

	/** The real config, built the way RuneLite does it: a proxy of the interface that answers with the defaults. */
	@SuppressWarnings("unchecked")
	private static DropMarketConfig config()
	{
		InvocationHandler handler = (proxy, method, args) ->
		{
			if (method.isDefault())
			{
				return java.lang.invoke.MethodHandles.privateLookupIn(DropMarketConfig.class,
						java.lang.invoke.MethodHandles.lookup())
					.unreflectSpecial(method, DropMarketConfig.class).bindTo(proxy).invokeWithArguments(args);
			}
			return defaultFor(method.getReturnType());
		};
		return (DropMarketConfig) Proxy.newProxyInstance(DropMarketConfig.class.getClassLoader(),
			new Class<?>[]{DropMarketConfig.class}, handler);
	}

	private static class Rig
	{
		final DropMarketPlugin plugin;
		final DropMarketOverlay overlay;
		final Guard guard;
		final Client client;
		final StubProfileStore store = new StubProfileStore();

		Rig(Mode mode) throws Exception
		{
			client = client(mode);
			OverlayManager overlays = allocate(OverlayManager.class);
			ClientThread thread = allocate(StubClientThread.class);
			ItemLookup items = new StubItemLookup(mode);
			plugin = new DropMarketPlugin();
			DropMarketConfig config = config();
			Injector injector = Guice.createInjector(new AbstractModule()
			{
				@Override
				protected void configure()
				{
					bind(Client.class).toInstance(client);
					bind(ClientThread.class).toInstance(thread);
					bind(OverlayManager.class).toInstance(overlays);
					bind(ProfileStore.class).toInstance(store);
					bind(ItemLookup.class).toInstance(items);
					bind(DropMarketConfig.class).toInstance(config);
					bind(DropMarketPlugin.class).toInstance(plugin);
					bind(Gson.class).toInstance(new Gson());
					bind(OkHttpClient.class).toInstance(new OkHttpClient());
				}
			});
			injector.injectMembers(plugin);
			overlay = injector.getInstance(DropMarketOverlay.class);
			guard = injector.getInstance(Guard.class);
		}

		/** Places that failed inside the guard, other than the overlay manager calls the stand-in can't serve. */
		List<String> unexpectedFailures()
		{
			List<String> places = new ArrayList<>(guard.failedPlaces());
			places.removeAll(OVERLAY_PLACES);
			return places;
		}

		void render()
		{
			BufferedImage image = new BufferedImage(300, 220, BufferedImage.TYPE_INT_ARGB);
			overlay.render(image.createGraphics());
		}
	}

	private static List<ItemStack> loot(Random random)
	{
		List<ItemStack> items = new ArrayList<>();
		int count = 1 + random.nextInt(4);
		for (int i = 0; i < count; i++)
		{
			items.add(new ItemStack(random.nextInt(8_000), 1 + random.nextInt(50)));
		}
		return items;
	}

	/** One event picked at random from everything the game sends the plugin. */
	private static void fire(DropMarketPlugin plugin, Random random, int i)
	{
		String[] sources = {"Zulrah", "Vorkath", "Abyssal Sire", "Vardorvis", "Tombs of Amascut", "Chambers of Xeric",
			"Theatre of Blood", "Cow", "Barrows", "Yama"};
		String[] messages = {"Valuable drop: Scythe of vitur (1,200,000,000 coins)", "Untradeable drop: Pet snakeling",
			"New item added to your collection log: Tanzanite fang", "You catch a shark.", "", "Your Zulrah kill count is: 12.",
			"Congratulations, you've just advanced your Mining level.", "Valuable drop: Emerald (500 coins)"};
		switch (random.nextInt(12))
		{
			case 0:
				plugin.onNpcLootReceived(new NpcLootReceived(npc(sources[random.nextInt(sources.length)], 1 + random.nextInt(900)),
					loot(random)));
				break;
			case 1:
				plugin.onLootReceived(new LootReceived(sources[random.nextInt(sources.length)], 700, LootRecordType.NPC,
					loot(random), 1, null));
				break;
			case 2:
				plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", messages[random.nextInt(messages.length)],
					null, i));
				break;
			case 3:
				plugin.onStatChanged(new StatChanged(Skill.values()[random.nextInt(Skill.values().length)], i * 10, 50, 50));
				break;
			case 4:
				plugin.onItemContainerChanged(new ItemContainerChanged(random.nextBoolean() ? 93 : 95, container(random)));
				break;
			case 5:
				HitsplatApplied hit = new HitsplatApplied();
				hit.setActor(npc("Zulrah", 2042));
				hit.setHitsplat(stub(Hitsplat.class, (m, args) -> m.getName().equals("isMine") ? true : defaultFor(m.getReturnType())));
				plugin.onHitsplatApplied(hit);
				break;
			case 6:
				ConfigChanged changed = new ConfigChanged();
				changed.setGroup(DropMarketConfig.GROUP);
				changed.setKey(new String[]{"priorityDrops", "blacklistedDrops", "overlayLayout", "winSound"}[random.nextInt(4)]);
				plugin.onConfigChanged(changed);
				break;
			case 7:
				GameStateChanged state = new GameStateChanged();
				state.setGameState(random.nextBoolean() ? GameState.LOGGED_IN : GameState.LOADING);
				plugin.onGameStateChanged(state);
				break;
			case 8:
				plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged(null, "profile"));
				break;
			case 9:
				String[] options = {"Read", "Open", "Dig", "Attack", "Use"};
				String[] targets = {"<col=ff9040>Clue scroll (hard)</col>", "Casket", null, ""};
				plugin.onMenuOptionClicked(new MenuOptionClicked(stub(MenuEntry.class, (m, args) ->
				{
					switch (m.getName())
					{
						case "getOption":
							return options[random.nextInt(options.length)];
						case "getTarget":
							return targets[random.nextInt(targets.length)];
						default:
							return defaultFor(m.getReturnType());
					}
				})));
				break;
			case 10:
				GameObjectSpawned spawned = new GameObjectSpawned();
				spawned.setGameObject(gameObject(random.nextBoolean() ? 27029 : 1));
				plugin.onGameObjectSpawned(spawned);
				break;
			default:
				GameObjectDespawned despawned = new GameObjectDespawned();
				despawned.setGameObject(gameObject(random.nextBoolean() ? 27029 : 1));
				plugin.onGameObjectDespawned(despawned);
				break;
		}
	}

	private static GameObject gameObject(int id)
	{
		return stub(GameObject.class, (m, args) ->
		{
			switch (m.getName())
			{
				case "getId":
					return id;
				case "getWorldLocation":
					return new WorldPoint(3000, 3000, 0);
				default:
					return defaultFor(m.getReturnType());
			}
		});
	}

	private static ItemContainer container(Random random)
	{
		Item[] contents = new Item[1 + random.nextInt(6)];
		for (int i = 0; i < contents.length; i++)
		{
			int id = random.nextInt(4) == 0 ? -1 : random.nextInt(8_000);
			contents[i] = new Item(id, 1);
		}
		return stub(ItemContainer.class, (m, args) -> m.getName().equals("getItems") ? contents : defaultFor(m.getReturnType()));
	}

	private void playSession(Rig rig, long seed) throws Exception
	{
		Random random = new Random(seed);
		rig.plugin.startUp();
		GameStateChanged login = new GameStateChanged();
		login.setGameState(GameState.LOGGED_IN);
		rig.plugin.onGameStateChanged(login);
		for (int i = 0; i < 3_000; i++)
		{
			rig.plugin.onGameTick(new GameTick());
			if (i % 7 == 0)
			{
				fire(rig.plugin, random, i);
			}
			if (i % 3 == 0)
			{
				rig.render();
			}
		}
		rig.plugin.shutDown();
	}

	@Test
	public void normalSessionNeverTripsTheGuard() throws Exception
	{
		for (long seed = 1; seed <= 5; seed++)
		{
			Rig rig = new Rig(Mode.NORMAL);
			playSession(rig, seed);
			Assert.assertEquals("failed places: " + rig.unexpectedFailures(), Collections.emptyList(), rig.unexpectedFailures());
		}
	}

	@Test
	public void nothingEscapesWhenTheGameHandsOverNulls() throws Exception
	{
		Rig rig = new Rig(Mode.NULLS);
		playSession(rig, 7);
		System.out.println("null-game failures swallowed: " + rig.guard.failedPlaces());
	}

	@Test
	public void nothingEscapesWhenTheGameThrows() throws Exception
	{
		Rig rig = new Rig(Mode.THROWING);
		playSession(rig, 11);
	}

	/** Events whose every field is null, which the real game never sends, fired at every handler. Nothing may escape. */
	@Test
	public void emptyEventsNeverEscape() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.plugin.startUp();
		for (int i = 0; i < 50; i++)
		{
			rig.plugin.onGameTick(allocate(GameTick.class));
			rig.plugin.onGameStateChanged(allocate(GameStateChanged.class));
			rig.plugin.onChatMessage(allocate(ChatMessage.class));
			rig.plugin.onStatChanged(allocate(StatChanged.class));
			rig.plugin.onItemContainerChanged(allocate(ItemContainerChanged.class));
			rig.plugin.onHitsplatApplied(allocate(HitsplatApplied.class));
			rig.plugin.onConfigChanged(allocate(ConfigChanged.class));
			rig.plugin.onGameObjectSpawned(allocate(GameObjectSpawned.class));
			rig.plugin.onGameObjectDespawned(allocate(GameObjectDespawned.class));
			rig.plugin.onNpcLootReceived(allocate(NpcLootReceived.class));
			rig.plugin.onLootReceived(allocate(LootReceived.class));
			rig.plugin.onRuneScapeProfileChanged(allocate(RuneScapeProfileChanged.class));
			rig.plugin.onMenuOptionClicked(allocate(MenuOptionClicked.class));
			rig.render();
		}
		System.out.println("empty-event failures swallowed: " + rig.unexpectedFailures());
	}

	/** Older versions saved the two drop values in gp under other keys; they carry over into thousands, once. */
	@Test
	public void oldGpValuesMoveOverIntoThousands() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.store.settings.put("minimumDropValue", "250000");
		rig.store.settings.put("valuableDropThreshold", "2000000");
		rig.plugin.startUp();
		Assert.assertEquals("250", rig.store.settings.get("marketMinimumAmount"));
		Assert.assertEquals("2000", rig.store.settings.get("winMinimumAmount"));

		// A value already chosen in the new setting is never overwritten by the old one.
		rig.store.settings.put("winMinimumAmount", "5000");
		rig.plugin.startUp();
		Assert.assertEquals("5000", rig.store.settings.get("winMinimumAmount"));
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	/** The change shown during a celebration is the jump to 100%, held steady, even if more drops land meanwhile. */
	@Test
	public void winDeltasHoldSteadyThroughTheCelebration() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.plugin.startUp();
		ChatMessage first = new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
			"New item added to your collection log: Tanzanite fang", null, 1);
		rig.plugin.onChatMessage(first);
		Assert.assertTrue("the drop resolved the market", rig.plugin.isResolved());
		int delta = rig.plugin.getDisplayDelta();
		int delta2 = rig.plugin.getDisplayDelta2();
		Assert.assertTrue("jump is shown, not 0%: " + delta, delta > 0);
		for (int i = 0; i < 30; i++)
		{
			rig.plugin.onGameTick(new GameTick());
			if (i % 5 == 0)
			{
				rig.plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "",
					"New item added to your collection log: Tanzanite fang", null, 2 + i));
			}
			Assert.assertEquals(delta, rig.plugin.getDisplayDelta());
			Assert.assertEquals(delta2, rig.plugin.getDisplayDelta2());
		}
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}

	@Test
	public void survivesStartingTwiceAndStoppingFirst() throws Exception
	{
		Rig rig = new Rig(Mode.NORMAL);
		rig.plugin.shutDown();
		rig.plugin.startUp();
		rig.plugin.startUp();
		rig.plugin.onGameTick(new GameTick());
		rig.plugin.shutDown();
		rig.plugin.shutDown();
		Assert.assertEquals(Collections.emptyList(), rig.unexpectedFailures());
	}
}

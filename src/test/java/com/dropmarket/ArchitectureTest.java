package com.dropmarket;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.client.eventbus.Subscribe;
import org.junit.Assert;
import org.junit.Test;

/** Structural rules that keep a plugin bug from reaching the game client. */
public class ArchitectureTest
{
	private static final Path SOURCES = Paths.get("src", "main", "java", "com", "dropmarket");

	/** RuneLite finds handlers by reflection and calls them straight from the event bus, so they must be public. */
	@Test
	public void everyEventHandlerIsPublicWithOneParameter()
	{
		int handlers = 0;
		for (Method method : DropMarketPlugin.class.getDeclaredMethods())
		{
			if (method.isAnnotationPresent(Subscribe.class))
			{
				handlers++;
				Assert.assertTrue(method.getName() + " must be public", Modifier.isPublic(method.getModifiers()));
				Assert.assertEquals(method.getName() + " takes one event", 1, method.getParameterCount());
			}
		}
		Assert.assertTrue("found the handlers", handlers >= 13);
	}

	/** The body of every handler must run inside the guard; the handler itself is just the one line that calls it. */
	@Test
	public void everyEventHandlerRunsInsideTheGuard() throws Exception
	{
		String source = read("DropMarketPlugin.java");
		for (Method method : DropMarketPlugin.class.getDeclaredMethods())
		{
			if (method.isAnnotationPresent(Subscribe.class))
			{
				Assert.assertTrue(method.getName() + " must call guard.run",
					source.contains("guard.run(\"" + method.getName() + "\""));
			}
		}
	}

	/** RuneLite's config proxy lives in another package, so everything the config touches has to be public. */
	@Test
	public void configAndItsTypesArePublic()
	{
		Assert.assertTrue(Modifier.isPublic(DropMarketConfig.class.getModifiers()));
		for (Method method : DropMarketConfig.class.getDeclaredMethods())
		{
			for (Class<?> type : types(method))
			{
				Assert.assertTrue(method.getName() + " uses non-public " + type, type.isPrimitive() || Modifier.isPublic(type.getModifiers()));
			}
		}
	}

	private static List<Class<?>> types(Method method)
	{
		List<Class<?>> types = new ArrayList<>();
		types.add(method.getReturnType());
		for (Class<?> parameter : method.getParameterTypes())
		{
			types.add(parameter);
		}
		return types;
	}

	/** The overlay's render call is the one RuneLite makes every frame; it must stay behind the guard. */
	@Test
	public void overlayRendersInsideTheGuard() throws Exception
	{
		Assert.assertTrue(read("DropMarketOverlay.java").contains("guard.call(\"overlay\""));
	}

	/** Only our small wrappers may touch RuneLite's item, price and settings services. */
	@Test
	public void gameServicesAreOnlyUsedThroughTheWrappers() throws Exception
	{
		for (Path file : sources())
		{
			String name = file.getFileName().toString();
			String text = read(name);
			if (!name.equals("ItemLookup.java"))
			{
				Assert.assertFalse(name + " should use ItemLookup, not ItemManager", text.contains("ItemManager;"));
			}
			if (!name.equals("ProfileStore.java") && !name.equals("DropMarketPlugin.java"))
			{
				Assert.assertFalse(name + " should use ProfileStore, not ConfigManager", text.contains("ConfigManager;"));
			}
		}
	}

	/** No threads, executors, blocking waits or system exits: everything runs on RuneLite's own threads. */
	@Test
	public void noThreadsOrBlockingCalls() throws Exception
	{
		String[] forbidden = {"new Thread", "Executors.", "Thread.sleep", "System.exit", "new Gson(", "new OkHttpClient(", ".join()"};
		for (Path file : sources())
		{
			String text = read(file.getFileName().toString());
			for (String bad : forbidden)
			{
				Assert.assertFalse(file.getFileName() + " contains " + bad, text.contains(bad));
			}
		}
	}

	/** Mutable static state leaks between plugin reloads; only constants may be static. */
	@Test
	public void noMutableStaticState()
	{
		for (Path file : sources())
		{
			String className = "com.dropmarket." + file.getFileName().toString().replace(".java", "");
			try
			{
				assertNoMutableStatics(Class.forName(className));
			}
			catch (ClassNotFoundException e)
			{
				throw new AssertionError(e);
			}
		}
	}

	private static void assertNoMutableStatics(Class<?> type)
	{
		for (Field field : type.getDeclaredFields())
		{
			int mods = field.getModifiers();
			if (Modifier.isStatic(mods) && !field.isSynthetic() && !field.getName().equals("log"))
			{
				Assert.assertTrue(type.getSimpleName() + "." + field.getName() + " is static but not final", Modifier.isFinal(mods));
			}
		}
	}

	private static List<Path> sources()
	{
		try (Stream<Path> files = Files.list(SOURCES))
		{
			return files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
		}
		catch (java.io.IOException e)
		{
			throw new AssertionError(e);
		}
	}

	private static String read(String name) throws java.io.IOException
	{
		return new String(Files.readAllBytes(SOURCES.resolve(name)), StandardCharsets.UTF_8);
	}
}

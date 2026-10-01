package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.Test;

public class CollectionLogTrackerTest
{
	@Test
	public void parsesRuneProfileShape()
	{
		String json = "{\"obtained\":1,\"total\":3,\"tabs\":[{\"name\":\"Bosses\",\"pages\":["
			+ "{\"name\":\"Abyssal Sire\",\"items\":[{\"id\":13262,\"name\":\"Abyssal orphan\",\"quantity\":0},"
			+ "{\"id\":25624,\"name\":\"Unsired\",\"quantity\":3}]}]},"
			+ "{\"name\":\"Other\",\"pages\":[{\"name\":\"Champion's challenge\",\"items\":["
			+ "{\"id\":6801,\"name\":\"Goblin champion scroll\",\"quantity\":0},"
			+ "{\"id\":25624,\"name\":\"Unsired\",\"quantity\":0}]}]}]}";

		Map<String, Boolean> slots = CollectionLogTracker.parseRuneProfile(new Gson().fromJson(json, JsonObject.class));
		assertEquals(3, slots.size());
		assertFalse(slots.get("Abyssal orphan"));
		assertTrue("obtained on any page counts", slots.get("Unsired"));
		assertFalse(slots.get("Goblin champion scroll"));
	}

	@Test
	public void missingTabsIsEmpty()
	{
		assertTrue(CollectionLogTracker.parseRuneProfile(new Gson().fromJson("{\"error\":\"x\"}", JsonObject.class))
			.isEmpty());
	}
}

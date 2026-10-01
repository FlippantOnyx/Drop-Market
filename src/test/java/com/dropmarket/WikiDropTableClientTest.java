package com.dropmarket;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import java.util.Arrays;
import org.junit.Test;

public class WikiDropTableClientTest
{
	private static String row(String item, String rarity, long value)
	{
		String drop = "{\\\"Rarity\\\":\\\"" + rarity + "\\\",\\\"Drop Value\\\":" + value
			+ ",\\\"Dropped item\\\":\\\"" + item + "\\\"}";
		return "{\"item_name\":\"" + item + "\",\"page_name\":\"Zulrah\",\"drop_json\":\"" + drop + "\"}";
	}

	@Test
	public void parseSkipsAlwaysDropsAndSortsByValue()
	{
		String json = "{\"bucketQuery\":\"...\",\"bucket\":["
			+ row("Zulrah's scales", "Always", 12) + ","
			+ row("Magic fang", "1/1,024", 61200) + ","
			+ row("Tanzanite fang", "1/1,024", 66000) + ","
			+ row("Magic fang", "1/1,024", 100) + ","
			+ row("Pet snakeling", "1/4,000", 0)
			+ "]}";

		WikiDropTableClient client = new WikiDropTableClient(null, new Gson(), null);
		assertEquals(Arrays.asList("Tanzanite fang", "Magic fang", "Pet snakeling"), client.parse(json));
	}

	@Test
	public void parseHandlesMissingBucket()
	{
		WikiDropTableClient client = new WikiDropTableClient(null, new Gson(), null);
		assertEquals(0, client.parse("{\"error\":\"bad query\"}").size());
	}
}

package com.dropmarket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class BundledDropTablesTest
{
	private final BundledDropTables bundled = new BundledDropTables(new com.google.gson.Gson());
	private final UniqueDrops uniqueDrops = new UniqueDrops(new com.google.gson.Gson());

	@Test
	public void snapshotHasMonstersAndNormalizesNames()
	{
		List<String> zulrah = bundled.lookup("Zulrah");
		assertNotNull(zulrah);
		assertTrue(zulrah.contains("Tanzanite fang"));
		assertNotNull(bundled.lookup("  ZULRAH "));
	}

	@Test
	public void snapshotSkipsDeadmanVariants()
	{
		for (String item : bundled.lookup("Yama"))
		{
			assertTrue(item, !item.contains("Deadman"));
		}
	}

	@Test
	public void unknownMonsterIsNull()
	{
		assertNull(bundled.lookup("Definitely Not A Monster"));
	}
}

package com.dropmarket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class BundledDropTablesTest
{
	static
	{
		BundledDropTables.configure(new com.google.gson.Gson());
		UniqueDrops.configure(new com.google.gson.Gson());
	}

	@Test
	public void snapshotHasMonstersAndNormalizesNames()
	{
		List<String> zulrah = BundledDropTables.lookup("Zulrah");
		assertNotNull(zulrah);
		assertTrue(zulrah.contains("Tanzanite fang"));
		assertNotNull(BundledDropTables.lookup("  ZULRAH "));
	}

	@Test
	public void snapshotSkipsDeadmanVariants()
	{
		for (String item : BundledDropTables.lookup("Yama"))
		{
			assertTrue(item, !item.contains("Deadman"));
		}
	}

	@Test
	public void unknownMonsterIsNull()
	{
		assertNull(BundledDropTables.lookup("Definitely Not A Monster"));
	}
}

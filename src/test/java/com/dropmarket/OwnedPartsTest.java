package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class OwnedPartsTest
{
	private static final List<String> HALBERD = Arrays.asList("Noxious blade", "Noxious point", "Noxious pommel");

	@Test
	public void ownedPartsCantBeMissingAndTheRestSplitTheValue()
	{
		OwnedParts parts = new OwnedParts();
		assertEquals(3, parts.missing(HALBERD));
		parts.add("Noxious pommel", HALBERD);
		assertTrue(parts.owns("noxious POMMEL"));
		assertEquals(2, parts.missing(HALBERD));
		parts.add("Noxious blade", HALBERD);
		assertEquals(1, parts.missing(HALBERD));
	}

	@Test
	public void completingTheSetStartsItOver()
	{
		OwnedParts parts = new OwnedParts();
		assertFalse(parts.add("Noxious pommel", HALBERD));
		assertFalse(parts.add("Noxious blade", HALBERD));
		assertTrue(parts.add("Noxious point", HALBERD));
		assertFalse(parts.owns("Noxious pommel"));
		assertEquals(3, parts.missing(HALBERD));
	}
}

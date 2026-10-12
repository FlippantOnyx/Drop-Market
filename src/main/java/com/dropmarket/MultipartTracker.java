package com.dropmarket;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Saves {@link OwnedParts} per RuneScape profile, the same way the collection log tracker keeps its data. */
@Singleton
class MultipartTracker
{
	private static final String KEY = "multipartOwned";

	private final ProfileStore store;
	private final OwnedParts parts = new OwnedParts();

	@Inject
	MultipartTracker(ProfileStore store)
	{
		this.store = store;
	}

	void load()
	{
		String value = store.get(KEY);
		parts.replaceAll(value == null || value.isEmpty() ? List.<String>of() : Arrays.asList(value.split(",")));
	}

	boolean owns(String part)
	{
		return parts.owns(part);
	}

	int missing(Collection<String> setParts)
	{
		return parts.missing(setParts);
	}

	/** Returns true if the part was newly recorded (so the caller knows the market may need repicking). */
	boolean add(String part, Collection<String> setParts)
	{
		boolean already = parts.owns(part);
		parts.add(part, setParts);
		if (!already)
		{
			store.set(KEY, String.join(",", parts.snapshot()));
		}
		return !already;
	}
}

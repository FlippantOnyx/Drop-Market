package com.dropmarket;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.Assert;
import org.junit.Test;

public class DropMarketConfigTest
{
	/** RuneLite's config proxy lives in another package, so every type the config returns must be public. */
	@Test
	public void configReturnTypesArePublic()
	{
		for (Method method : DropMarketConfig.class.getDeclaredMethods())
		{
			Class<?> type = method.getReturnType();
			Assert.assertTrue(method.getName() + " returns a non-public type", type.isPrimitive() || Modifier.isPublic(type.getModifiers()));
		}
	}
}

package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;
import org.junit.Test;

public class MarketPainterTest
{
	// Set DROPMARKET_PREVIEW_DIR=/some/dir to dump PNGs of each state for eyeballing.
	private static final String PREVIEW_DIR = System.getenv("DROPMARKET_PREVIEW_DIR");
	private static final int W = DropMarketOverlay.DEFAULT_SIZE.width;
	private static final int H = DropMarketOverlay.DEFAULT_SIZE.height;

	private static final class TestConfig implements DropMarketConfig
	{
		Color line1Color = DEFAULT_LINE_COLOR;
		Color line2Color = DEFAULT_LINE2_COLOR;
		Color line1FallingColor = DEFAULT_NEGATIVE_DELTA_COLOR;
		Color line2FallingColor = DEFAULT_NEGATIVE_DELTA_COLOR;
		boolean showBar1;
		boolean showBar2;
		boolean showSecondLine;
		Color bar1Color = DEFAULT_BAR1_COLOR;
		Color bar2Color = DEFAULT_BAR2_COLOR;
		Color bar1TextColor = Color.WHITE;
		Color bar2TextColor = Color.BLACK;
		String bar1Text = "";
		String bar2Text = "";
		OverlayLayout layout = OverlayLayout.FULL;

		@Override
		public OverlayLayout overlayLayout()
		{
			return layout;
		}

		@Override
		public Color line1Color()
		{
			return line1Color;
		}

		@Override
		public Color line2Color()
		{
			return line2Color;
		}

		@Override
		public Color line1FallingColor()
		{
			return line1FallingColor;
		}

		@Override
		public Color line2FallingColor()
		{
			return line2FallingColor;
		}

		@Override
		public boolean showBar1()
		{
			return showBar1;
		}

		@Override
		public boolean showBar2()
		{
			return showBar2;
		}

		@Override
		public boolean showSecondLine()
		{
			return showSecondLine;
		}

		@Override
		public Color bar1Color()
		{
			return bar1Color;
		}

		@Override
		public Color bar2Color()
		{
			return bar2Color;
		}

		@Override
		public Color bar1TextColor()
		{
			return bar1TextColor;
		}

		@Override
		public Color bar2TextColor()
		{
			return bar2TextColor;
		}

		@Override
		public String bar1Text()
		{
			return bar1Text;
		}

		@Override
		public String bar2Text()
		{
			return bar2Text;
		}
	}

	private static BufferedImage render(String title, boolean resolved, long nowTick, int w, int h) throws Exception
	{
		return render(title, resolved, nowTick, w, h, new TestConfig(), null, 0);
	}

	private static BufferedImage render(String title, boolean resolved, long nowTick, int w, int h,
		TestConfig config, MarketGraph polyGraph, long volumePop) throws Exception
	{
		MarketGraph graph = new MarketGraph(new Random(1234));
		graph.reset();
		if (resolved)
		{
			graph.pinToCertain();
			graph.pinToCertain();
		}

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		MarketPainter.paint(g, w, h, title, graph, polyGraph, config, resolved, nowTick,
			FontManager.getRunescapeBoldFont(), FontManager.getRunescapeSmallFont(), "3 KC", true, volumePop, 12, 9);
		g.dispose();
		return img;
	}

	private static void save(BufferedImage img, String name) throws Exception
	{
		if (PREVIEW_DIR != null)
		{
			ImageIO.write(img, "png", new File(PREVIEW_DIR, name + ".png"));
		}
	}

	private static boolean containsColor(BufferedImage img, Color c)
	{
		for (int y = 0; y < img.getHeight(); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if ((img.getRGB(x, y) & 0xFFFFFF) == (c.getRGB() & 0xFFFFFF))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Test
	public void drawsLineInGraphColor() throws Exception
	{
		BufferedImage img = render("Zulrah drops Tanzanite fang", false, 0, W, H);
		save(img, "market");
		assertTrue(containsColor(img, DropMarketConfig.DEFAULT_LINE_COLOR));
	}

	@Test
	public void resolvedFlashesRaidPurple() throws Exception
	{
		BufferedImage on = render("Chambers of Xeric drops Twisted bow", true, 0, W, H);
		BufferedImage off = render("Chambers of Xeric drops Twisted bow", true, 2, W, H);
		save(on, "resolved-flash-on");
		save(off, "resolved-flash-off");
		assertTrue(containsColor(on, MarketPainter.RAID_PURPLE));
	}

	@Test
	public void longTitlesAndOtherSizesRender() throws Exception
	{
		save(render(DropMarketPlugin.LIVE_ODDS, false, 7, W, H), "live-odds");
		save(render("Theatre of Blood drops Sanguinesti staff and an extremely long suffix", false, 0, W, H),
			"long-title");
		save(render("Tombs of Amascut drops Tumeken's shadow", false, 0, 320, 160), "large");
		save(render("Goblin drops Anything good", false, 0, 120, 80), "tiny");
	}

	@Test
	public void brandBarsRender() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		TestConfig config = new TestConfig();
		config.showBar1 = true;
		config.showBar2 = true;
		config.showSecondLine = true;
		config.bar1Text = "Line 1 Odds";
		config.bar2Text = "Line 2 Odds";
		BufferedImage img = render("Zulrah drops Tanzanite fang", false, 0, W, H, config, poly, 1_400_000L);
		save(img, "brand-bars");
		assertTrue(containsColor(img, DropMarketConfig.DEFAULT_LINE_COLOR));
	}

	@Test
	public void flashTurnsBothLinesPurple() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		TestConfig config = new TestConfig();
		config.showSecondLine = true;
		BufferedImage idle = render("Zulrah drops Tanzanite fang", false, 0, W, H, config, poly, 0);
		BufferedImage flashing = render("Zulrah drops Tanzanite fang", true, 0, W, H, config, poly, 0);
		assertTrue(containsColor(idle, DropMarketConfig.DEFAULT_LINE2_COLOR));
		assertTrue(containsColor(flashing, MarketPainter.RAID_PURPLE));
		assertFalse(containsColor(flashing, DropMarketConfig.DEFAULT_LINE2_COLOR));
		assertFalse(containsColor(flashing, DropMarketConfig.DEFAULT_LINE_COLOR));
	}

	@Test
	public void compactAndTickerLayoutsRenderTheOddsAndFlash() throws Exception
	{
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		for (OverlayLayout layout : new OverlayLayout[]{OverlayLayout.COMPACT, OverlayLayout.TICKER})
		{
			TestConfig config = new TestConfig();
			config.layout = layout;
			config.showSecondLine = true;
			int h = MarketPainter.layoutHeight(config);
			BufferedImage idle = render("Theatre of Blood drops Scythe of vitur", false, 0, 300, h, config, poly, 0);
			BufferedImage flashing = render("Theatre of Blood drops Scythe of vitur", true, 0, 300, h, config, poly, 0);
			assertTrue(layout + " idle shows line 1", containsColor(idle, DropMarketConfig.DEFAULT_LINE_COLOR));
			assertTrue(layout + " idle shows line 2", containsColor(idle, DropMarketConfig.DEFAULT_LINE2_COLOR));
			assertTrue(layout + " flashes purple", containsColor(flashing, MarketPainter.RAID_PURPLE));
			assertFalse(layout + " flash covers line 2", containsColor(flashing, DropMarketConfig.DEFAULT_LINE2_COLOR));
		}
	}

	@Test
	public void layoutHeightsShrinkAndBarsAddToCompactOnly()
	{
		TestConfig config = new TestConfig();
		assertTrue(MarketPainter.layoutHeight(config) < 0);
		config.layout = OverlayLayout.COMPACT;
		int plain = MarketPainter.layoutHeight(config);
		config.showBar1 = true;
		config.showBar2 = true;
		assertTrue(MarketPainter.layoutHeight(config) > plain);
		config.layout = OverlayLayout.TICKER;
		assertTrue(MarketPainter.layoutHeight(config) < plain);
	}

	@Test
	public void compactTitleUsesWikiAbbreviationsThenDropsTheSource()
	{
		// One unit per character keeps the widths easy to reason about.
		java.util.function.ToIntFunction<String> chars = String::length;
		String full = "Theatre of Blood drops Scythe of vitur";
		assertEquals("TOB DROPS SCYTHE OF VITUR", MarketPainter.compactTitle(full, chars, 40));
		assertEquals("DROPS SCYTHE OF VITUR", MarketPainter.compactTitle(full, chars, 24));
		assertEquals("DROPS SCYTHE", MarketPainter.compactTitle(full, chars, 14));
		assertEquals("TOB DROPS TWISTED BOW",
			MarketPainter.compactTitle("Chambers of Xeric drops Twisted bow", chars, 40).replace("COX", "TOB"));
		assertEquals("COX DROPS TWISTED BOW", MarketPainter.compactTitle("Chambers of Xeric drops Twisted bow", chars, 40));
		assertEquals("ZULRAH DROPS TANZANITE FANG", MarketPainter.compactTitle("Zulrah drops Tanzanite fang", chars, 40));
		assertEquals("KBD DROPS DRAGON PICKAXE", MarketPainter.compactTitle("King Black Dragon drops Dragon pickaxe", chars, 40));
		assertEquals("LIVE ODDS", MarketPainter.compactTitle("LIVE ODDS", chars, 40));
		assertEquals("GETS SCYTHE", MarketPainter.compactTitle("Bob GETS Scythe of vitur", chars, 12));
	}

	@Test
	public void longTickerTitlesScrollEachTick() throws Exception
	{
		TestConfig config = new TestConfig();
		config.layout = OverlayLayout.TICKER;
		int h = MarketPainter.layoutHeight(config);
		BufferedImage first = render("Theatre of Blood drops Scythe of vitur", false, 0, 200, h, config, null, 0);
		BufferedImage later = render("Theatre of Blood drops Scythe of vitur", false, 3, 200, h, config, null, 0);
		assertTrue(differs(first, later));
		assertEquals("TOB DROPS SCYTHE OF VITUR", MarketPainter.abbreviatedTitle("Theatre of Blood drops Scythe of vitur"));
	}

	private static boolean differs(BufferedImage a, BufferedImage b)
	{
		for (int y = 0; y < a.getHeight(); y++)
		{
			for (int x = 0; x < a.getWidth(); x++)
			{
				if (a.getRGB(x, y) != b.getRGB(x, y))
				{
					return true;
				}
			}
		}
		return false;
	}
}

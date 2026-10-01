package com.dropmarket;

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

	private static BufferedImage render(String title, boolean resolved, long now, int w, int h) throws Exception
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
		MarketPainter.paint(g, w, h, title, graph, DropMarketConfig.KALSHI_GREEN, resolved, now,
			FontManager.getRunescapeBoldFont(), FontManager.getRunescapeSmallFont(), "3 KC", true, false, false, null, 0);
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
		assertTrue(containsColor(img, DropMarketConfig.KALSHI_GREEN));
	}

	@Test
	public void resolvedFlashesRaidPurple() throws Exception
	{
		BufferedImage on = render("Chambers of Xeric drops Twisted bow", true, 0, W, H);
		BufferedImage off = render("Chambers of Xeric drops Twisted bow", true, 400, W, H);
		save(on, "resolved-flash-on");
		save(off, "resolved-flash-off");
		assertTrue(containsColor(on, MarketPainter.RAID_PURPLE));
	}

	@Test
	public void longTitlesAndOtherSizesRender() throws Exception
	{
		save(render(DropMarketPlugin.LIVE_ODDS, false, 700, W, H), "live-odds");
		save(render("Theatre of Blood drops Sanguinesti staff and an extremely long suffix", false, 0, W, H),
			"long-title");
		save(render("Tombs of Amascut drops Tumeken's shadow", false, 0, 320, 160), "large");
		save(render("Goblin drops Anything good", false, 0, 120, 80), "tiny");
	}

	@Test
	public void brandBarsRender() throws Exception
	{
		MarketGraph graph = new MarketGraph(new Random(1234));
		graph.reset();
		MarketGraph poly = new MarketGraph(new Random(99));
		poly.reset();
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		MarketPainter.paint(g, W, H, "Zulrah drops Tanzanite fang", graph, DropMarketConfig.KALSHI_GREEN, false, 0,
			FontManager.getRunescapeBoldFont(), FontManager.getRunescapeSmallFont(), "0:42", false, true, true, poly, 1_400_000L);
		g.dispose();
		save(img, "brand-bars");
		assertTrue(containsColor(img, DropMarketConfig.KALSHI_GREEN));
	}
}

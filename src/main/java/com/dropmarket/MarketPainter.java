package com.dropmarket;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

final class MarketPainter
{
	static final Color RAID_PURPLE = new Color(0xEF20FF);

	private static final Color BACKGROUND = new Color(12, 14, 18, 200);
	private static final Color BORDER = new Color(255, 255, 255, 30);
	private static final Color GRID = new Color(255, 255, 255, 45);
	private static final Color MUTED = new Color(150, 156, 166);
	private static final Color TITLE = new Color(235, 237, 240);
	private static final Color LIVE_RED = new Color(0xFF3B5C);
	private static final Color EYE = new Color(20, 20, 24);
	private static final Font BAR_TEXT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
	private static final Stroke GRID_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
		10f, new float[]{2f, 3f}, 0f);
	private static final Stroke LINE_STROKE = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final int PAD = 6;
	// All animation here steps once per game tick (0.6s) rather than wall-clock time, so it stays in lockstep
	// with everything else OSRS does on the tick.
	private static final int FLASH_PERIOD_TICKS = 2;
	private static final int DOT_PERIOD_TICKS = 1;
	private static final int PULSE_PERIOD_TICKS = 3;
	private static final int BRAND_BAR_H = 14;
	private static final int COMPACT_BODY_H = 56;
	private static final int TICKER_H = 24;

	private MarketPainter()
	{
	}

	static void paint(Graphics2D graphics, int w, int h, String title, MarketGraph graph, MarketGraph polyGraph,
		DropMarketConfig config, boolean resolved, long nowTick, Font titleFont, Font smallFont, String badge,
		boolean badgeSkull, long volumePop, int delta1, int delta2)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			boolean flashOn = resolved && config.flashOnWin() && (nowTick / FLASH_PERIOD_TICKS) % 2 == 0;
			Color lineColor = config.line1Color();
			Color accent = flashOn ? RAID_PURPLE : lineColor;
			MarketGraph secondLine = config.showSecondLine() ? polyGraph : null;
			Color accent2 = flashOn ? RAID_PURPLE : config.line2Color();

			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			RoundRectangle2D panel = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 8, 8);
			g.setColor(BACKGROUND);
			g.fill(panel);
			if (flashOn)
			{
				g.setColor(new Color(RAID_PURPLE.getRed(), RAID_PURPLE.getGreen(), RAID_PURPLE.getBlue(), 45));
				g.fill(panel);
			}

			OverlayLayout layout = config.overlayLayout();
			boolean bars = layout != OverlayLayout.TICKER;
			int y = 0;
			if (bars && config.showBar1())
			{
				g.setColor(config.bar1Color());
				g.fillRect(0, y, w, BRAND_BAR_H);
				if (config.bar1Text() != null && !config.bar1Text().isEmpty())
				{
					drawBarText(g, config.bar1Text(), w, y, BRAND_BAR_H, config.bar1TextColor());
				}
				y += BRAND_BAR_H;
			}
			if (bars && config.showBar2())
			{
				g.setColor(config.bar2Color());
				g.fillRect(0, y, w, BRAND_BAR_H);
				if (config.bar2Text() != null && !config.bar2Text().isEmpty())
				{
					drawBarText(g, config.bar2Text(), w, y, BRAND_BAR_H, config.bar2TextColor());
				}
				y += BRAND_BAR_H;
			}

			g.setColor(flashOn ? RAID_PURPLE : BORDER);
			g.setStroke(new BasicStroke(flashOn ? 2f : 1f));
			g.draw(panel);

			if (layout == OverlayLayout.COMPACT)
			{
				paintCompact(g, w, y, title, graph, secondLine, accent, accent2, config, nowTick, titleFont, smallFont,
					badge, delta1, delta2);
				return;
			}
			if (layout == OverlayLayout.TICKER)
			{
				paintTicker(g, w, h, title, graph, secondLine, accent, accent2, config, nowTick, titleFont, smallFont,
					delta1, delta2);
				return;
			}

			Font bigFont = titleFont.deriveFont(titleFont.getSize2D() * 1.25f);
			FontMetrics titleFm = g.getFontMetrics(titleFont);
			FontMetrics smallFm = g.getFontMetrics(smallFont);
			FontMetrics bigFm = g.getFontMetrics(bigFont);

			y += PAD;
			int liveBaseline = y + smallFm.getAscent();
			int lx = PAD;
			boolean dotOn = (nowTick / DOT_PERIOD_TICKS) % 2 == 0;
			int dotR = 3;
			g.setFont(smallFont.deriveFont(Font.BOLD));
			FontMetrics liveFm = g.getFontMetrics();
			double dotCenterY = liveBaseline - liveFm.getAscent() / 2.0 - 1;
			g.setColor(dotOn ? LIVE_RED : withAlpha(LIVE_RED, 80));
			g.fill(new Ellipse2D.Double(lx, dotCenterY - dotR, dotR * 2, dotR * 2));
			lx += dotR * 2 + 4;
			text(g, "LIVE", lx, liveBaseline, LIVE_RED);
			lx += liveFm.stringWidth("LIVE") + 8;

			int skullSize = Math.max(6, smallFm.getAscent() - 2);
			if (badge != null && !badge.isEmpty())
			{
				g.setFont(smallFont);
				lx += 2;
				text(g, "·", lx, liveBaseline, MUTED);
				lx += smallFm.stringWidth("·") + 6;
				if (badgeSkull)
				{
					drawSkull(g, lx, (int) Math.round(dotCenterY + skullSize / 2.0), skullSize, MUTED);
					lx += skullSize + 3;
				}
				text(g, badge, lx, liveBaseline, MUTED);
			}
			y += Math.max(smallFm.getHeight(), skullSize + 2) + 4;

			List<String> lines = wrap(title.toUpperCase(), titleFm, w - PAD * 2, 2);
			g.setFont(titleFont);
			for (String line : lines)
			{
				y += titleFm.getAscent();
				text(g, line, PAD, y, TITLE);
				y += titleFm.getDescent();
			}

			y += 3 + bigFm.getAscent();
			int pct = (int) Math.round(graph.current());
			String pctText = pct + "%";
			g.setFont(bigFont);
			text(g, pctText, PAD, y, accent);
			int x = PAD + bigFm.stringWidth(pctText) + 10;

			int deltaW = drawDelta(g, x, y, delta1, accent, config.line1FallingColor(), smallFont);
			if (secondLine != null)
			{
				drawDelta(g, x + deltaW + 8, y, delta2, accent2, config.line2FallingColor(), smallFont);
			}
			y += bigFm.getDescent();
			g.setFont(smallFont);

			int footerH = smallFm.getAscent() + 2;
			int labelW = smallFm.stringWidth("100%") + 4;
			int chartLeft = PAD;
			int chartRight = w - PAD - labelW;
			int chartTop = y + 4 + smallFm.getAscent() / 2;
			int chartBottom = h - PAD - footerH - smallFm.getAscent() / 2;
			if (chartBottom - chartTop >= 10 && chartRight - chartLeft >= 20)
			{
				drawChart(g, graph, secondLine, accent, accent2, chartLeft, chartTop, chartRight,
					chartBottom, smallFm, nowTick);
			}

			int footY = h - PAD;
			g.setFont(smallFont);
			String volume = formatVolume(graph.volume());
			text(g, volume, PAD, footY, MUTED);
			int footRight = w - PAD;
			if (resolved)
			{
				String badgeText = "RESOLVED: YES";
				footRight -= smallFm.stringWidth(badgeText);
				text(g, badgeText, footRight, footY, flashOn ? RAID_PURPLE : TITLE);
			}
			if (volumePop > 0)
			{
				String pop = "+ " + formatPop(volumePop);
				int px = PAD + smallFm.stringWidth(volume) + 6;
				if (px + smallFm.stringWidth(pop) <= footRight - 4)
				{
					text(g, pop, px, footY, accent);
				}
			}
		}
		finally
		{
			g.dispose();
		}
	}

	/** Height of the overlay for a layout; Full keeps whatever size the player resized it to (returns -1). */
	static int layoutHeight(DropMarketConfig config)
	{
		switch (config.overlayLayout())
		{
			case COMPACT:
				return (config.showBar1() ? BRAND_BAR_H : 0) + (config.showBar2() ? BRAND_BAR_H : 0) + COMPACT_BODY_H;
			case TICKER:
				return TICKER_H;
			default:
				return -1;
		}
	}

	/** Two rows: live dot + title (+ badge), then the odds with their changes and a sparkline. */
	private static void paintCompact(Graphics2D g, int w, int top, String title, MarketGraph graph,
		MarketGraph secondLine, Color accent, Color accent2, DropMarketConfig config, long nowTick, Font titleFont,
		Font smallFont, String badge, int delta1, int delta2)
	{
		FontMetrics titleFm = g.getFontMetrics(titleFont);
		FontMetrics smallFm = g.getFontMetrics(smallFont);
		Font bigFont = titleFont.deriveFont(titleFont.getSize2D() * 1.25f);
		FontMetrics bigFm = g.getFontMetrics(bigFont);

		int y = top + PAD;
		int baseline = y + titleFm.getAscent();
		int x = PAD;
		int dotR = 3;
		boolean dotOn = (nowTick / DOT_PERIOD_TICKS) % 2 == 0;
		g.setColor(dotOn ? LIVE_RED : withAlpha(LIVE_RED, 80));
		g.fill(new Ellipse2D.Double(x, baseline - titleFm.getAscent() / 2.0 - dotR - 1, dotR * 2, dotR * 2));
		x += dotR * 2 + 5;

		int right = w - PAD;
		if (badge != null && !badge.isEmpty())
		{
			g.setFont(smallFont);
			right -= smallFm.stringWidth(badge);
			text(g, badge, right, baseline, MUTED);
			right -= 6;
		}
		g.setFont(titleFont);
		text(g, ellipsize(title.toUpperCase(), titleFm, right - x), x, baseline, TITLE);
		y += titleFm.getHeight() + 2;

		int pctBaseline = y + bigFm.getAscent();
		String pctText = Math.round(graph.current()) + "%";
		g.setFont(bigFont);
		text(g, pctText, PAD, pctBaseline, accent);
		int dx = PAD + bigFm.stringWidth(pctText) + 10;
		dx += drawDelta(g, dx, pctBaseline, delta1, accent, config.line1FallingColor(), smallFont);
		if (secondLine != null)
		{
			dx += 8 + drawDelta(g, dx + 8, pctBaseline, delta2, accent2, config.line2FallingColor(), smallFont);
		}

		int sparkLeft = dx + 12;
		int sparkRight = w - PAD - 3;
		if (sparkRight - sparkLeft >= 24)
		{
			int sparkTop = y + 2;
			int sparkBottom = pctBaseline + bigFm.getDescent() - 2;
			if (secondLine != null)
			{
				drawLine(g, secondLine, accent2, sparkLeft, sparkTop, sparkRight, sparkBottom, nowTick);
			}
			drawLine(g, graph, accent, sparkLeft, sparkTop, sparkRight, sparkBottom, nowTick);
		}
	}

	/** One line: live dot, title, odds, change(s) and a small sparkline, like a stock ticker. */
	private static void paintTicker(Graphics2D g, int w, int h, String title, MarketGraph graph,
		MarketGraph secondLine, Color accent, Color accent2, DropMarketConfig config, long nowTick, Font titleFont,
		Font smallFont, int delta1, int delta2)
	{
		FontMetrics titleFm = g.getFontMetrics(titleFont);
		int baseline = (h + titleFm.getAscent() - titleFm.getDescent()) / 2;

		int dotR = 3;
		boolean dotOn = (nowTick / DOT_PERIOD_TICKS) % 2 == 0;
		g.setColor(dotOn ? LIVE_RED : withAlpha(LIVE_RED, 80));
		g.fill(new Ellipse2D.Double(PAD, h / 2.0 - dotR, dotR * 2, dotR * 2));
		int titleLeft = PAD + dotR * 2 + 5;

		int right = w - PAD;
		int sparkW = w >= 260 ? 44 : 0;
		// Only wide strips have room for the second line's change next to the odds.
		boolean secondDelta = secondLine != null && w >= 300;
		if (sparkW > 0)
		{
			int sparkLeft = right - sparkW;
			if (secondLine != null)
			{
				drawLine(g, secondLine, accent2, sparkLeft, 4, right - 3, h - 4, nowTick);
			}
			drawLine(g, graph, accent, sparkLeft, 4, right - 3, h - 4, nowTick);
			right = sparkLeft - 8;
		}

		int deltaW = deltaWidth(g, delta1, smallFont) + (secondDelta ? 8 + deltaWidth(g, delta2, smallFont) : 0);
		int deltaX = right - deltaW;
		int dx = deltaX + drawDelta(g, deltaX, baseline, delta1, accent, config.line1FallingColor(), smallFont);
		if (secondDelta)
		{
			drawDelta(g, dx + 8, baseline, delta2, accent2, config.line2FallingColor(), smallFont);
		}

		String pctText = Math.round(graph.current()) + "%";
		g.setFont(titleFont);
		int pctX = deltaX - 8 - titleFm.stringWidth(pctText);
		text(g, pctText, pctX, baseline, accent);
		Font tickerTitleFont = smallFont.deriveFont(Font.BOLD);
		g.setFont(tickerTitleFont);
		text(g, ellipsize(title.toUpperCase(), g.getFontMetrics(tickerTitleFont), pctX - 8 - titleLeft), titleLeft,
			baseline, TITLE);
	}

	private static int deltaWidth(Graphics2D g, int delta, Font smallFont)
	{
		FontMetrics fm = g.getFontMetrics(deltaFont(smallFont));
		return (delta != 0 ? 11 : 0) + fm.stringWidth(Math.abs(delta) + "%");
	}

	/** Always renders (the delta should never disappear); the arrow is skipped only for an exact 0. */
	private static int drawDelta(Graphics2D g, int x, int y, int delta, Color positiveColor, Color negativeColor,
		Font smallFont)
	{
		Color color = delta < 0 ? negativeColor : positiveColor;
		String deltaText = Math.abs(delta) + "%";
		Font deltaFont = deltaFont(smallFont);
		FontMetrics deltaFm = g.getFontMetrics(deltaFont);
		int triW = 7;
		int mid = y - deltaFm.getAscent() / 2 - 1;
		if (delta < 0)
		{
			// Drop the down-triangle back down 1px relative to the up-triangle's nudge.
			mid += 1;
		}
		int textX = x;
		if (delta != 0)
		{
			Polygon tri = delta > 0
				? new Polygon(new int[]{x, x + triW, x + triW / 2}, new int[]{mid + 3, mid + 3, mid - 3}, 3)
				: new Polygon(new int[]{x, x + triW, x + triW / 2}, new int[]{mid - 3, mid - 3, mid + 3}, 3);
			g.setColor(color);
			g.fill(tri);
			textX = x + triW + 4;
		}
		g.setFont(deltaFont);
		text(g, deltaText, textX, y, color);
		return (textX - x) + deltaFm.stringWidth(deltaText);
	}

	private static Font deltaFont(Font smallFont)
	{
		return smallFont.deriveFont(Font.BOLD, smallFont.getSize2D() * 1.15f);
	}

	private static void drawBarText(Graphics2D g, String s, int w, int barTop, int barH, Color color)
	{
		Font old = g.getFont();
		g.setFont(BAR_TEXT_FONT);
		FontMetrics fm = g.getFontMetrics();
		int tx = (w - fm.stringWidth(s)) / 2;
		int ty = barTop + (barH + fm.getAscent() - fm.getDescent()) / 2;
		g.setColor(color);
		g.drawString(s, tx, ty);
		g.setFont(old);
	}

	private static void drawSkull(Graphics2D g, int x, int baselineY, int size, Color color)
	{
		double top = baselineY - size;
		g.setColor(color);
		g.fill(new Ellipse2D.Double(x, top, size, size * 0.78));
		g.fill(new RoundRectangle2D.Double(x + size * 0.22, top + size * 0.58, size * 0.56, size * 0.42, 2, 2));
		g.setColor(EYE);
		double eyeR = size * 0.15;
		double eyeY = top + size * 0.26;
		g.fill(new Ellipse2D.Double(x + size * 0.18, eyeY, eyeR * 2, eyeR * 2));
		g.fill(new Ellipse2D.Double(x + size * 0.82 - eyeR * 2, eyeY, eyeR * 2, eyeR * 2));
		g.fillRect((int) Math.round(x + size * 0.5 - 0.5), (int) Math.round(top + size * 0.78), 1,
			(int) Math.max(1, Math.round(size * 0.2)));
	}

	private static void drawChart(Graphics2D g, MarketGraph graph, MarketGraph secondLine, Color color,
		Color secondColor, int left, int top, int right, int bottom, FontMetrics smallFm, long nowTick)
	{
		g.setStroke(GRID_STROKE);
		int labelGap = smallFm.getAscent() + 1;
		int chartH = bottom - top;
		for (int pct : new int[]{0, 50, 100})
		{
			int gy = yFor(pct, top, bottom);
			g.setColor(GRID);
			g.drawLine(left, gy, right, gy);
			boolean fits = pct == 100 || (pct == 0 ? chartH >= labelGap : chartH / 2 >= labelGap);
			if (fits)
			{
				text(g, pct + "%", right + 4, gy + smallFm.getAscent() / 2 - 1, MUTED);
			}
		}

		if (secondLine != null)
		{
			drawLine(g, secondLine, secondColor, left, top, right, bottom, nowTick);
		}
		drawLine(g, graph, color, left, top, right, bottom, nowTick);
	}

	private static void drawLine(Graphics2D g, MarketGraph graph, Color color, int left, int top, int right,
		int bottom, long nowTick)
	{
		int n = graph.size();
		if (n < 2)
		{
			return;
		}

		Path2D.Double line = new Path2D.Double();
		double lastX = right;
		double lastY = bottom;
		for (int i = 0; i < n; i++)
		{
			double px = left + (double) (right - left) * i / (n - 1);
			double py = top + (bottom - top) * (1 - graph.get(i) / 100.0);
			if (i == 0)
			{
				line.moveTo(px, py);
			}
			else
			{
				line.lineTo(px, py);
			}
			lastX = px;
			lastY = py;
		}

		Path2D.Double area = new Path2D.Double(line);
		area.lineTo(right, bottom);
		area.lineTo(left, bottom);
		area.closePath();
		g.setPaint(new GradientPaint(0, top, withAlpha(color, 70), 0, bottom, withAlpha(color, 0)));
		g.fill(area);

		g.setStroke(LINE_STROKE);
		g.setColor(color);
		g.draw(line);

		double pulse = (nowTick % PULSE_PERIOD_TICKS) / (double) PULSE_PERIOD_TICKS;
		double haloR = 3 + 5 * pulse;
		g.setColor(withAlpha(color, (int) (130 * (1 - pulse))));
		g.fill(new Ellipse2D.Double(lastX - haloR, lastY - haloR, haloR * 2, haloR * 2));
		g.setColor(color);
		g.fill(new Ellipse2D.Double(lastX - 2.5, lastY - 2.5, 5, 5));
	}

	private static int yFor(int pct, int top, int bottom)
	{
		return (int) Math.round(top + (bottom - top) * (1 - pct / 100.0));
	}

	private static void text(Graphics2D g, String s, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(s, x + 1, y + 1);
		g.setColor(color);
		g.drawString(s, x, y);
	}

	private static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
	}

	static String formatPop(long v)
	{
		if (v >= 1_000_000_000L)
		{
			return String.format("$%.1fB", v / 1e9);
		}
		if (v >= 1_000_000L)
		{
			return String.format("$%.1fM", v / 1e6);
		}
		return String.format("$%dK", v / 1000);
	}

	static String formatVolume(long v)
	{
		if (v >= 1_000_000_000L)
		{
			return String.format("$%.1fB Vol.", v / 1e9);
		}
		if (v >= 1_000_000L)
		{
			return String.format("$%.1fM Vol.", v / 1e6);
		}
		return String.format("$%,dK Vol.", v / 1000);
	}

	static List<String> wrap(String text, FontMetrics fm, int maxWidth, int maxLines)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		String[] words = text.split(" ");
		for (int i = 0; i < words.length; i++)
		{
			String candidate = current.length() == 0 ? words[i] : current + " " + words[i];
			if (fm.stringWidth(candidate) <= maxWidth || current.length() == 0)
			{
				current.setLength(0);
				current.append(candidate);
				continue;
			}
			if (lines.size() == maxLines - 1)
			{
				lines.add(ellipsize(candidate, fm, maxWidth));
				return lines;
			}
			lines.add(current.toString());
			current.setLength(0);
			current.append(words[i]);
		}
		if (current.length() > 0)
		{
			lines.add(lines.size() == maxLines - 1 ? ellipsize(current.toString(), fm, maxWidth) : current.toString());
		}
		return lines;
	}

	private static String ellipsize(String s, FontMetrics fm, int maxWidth)
	{
		if (fm.stringWidth(s) <= maxWidth)
		{
			return s;
		}
		String out = s;
		while (out.length() > 1 && fm.stringWidth(out + "...") > maxWidth)
		{
			out = out.substring(0, out.length() - 1);
		}
		return out.trim() + "...";
	}
}

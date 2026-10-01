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
	private static final Color UP = new Color(0x2ECC71);
	private static final Color DOWN = new Color(0xE5484D);
	private static final Color LIVE_RED = new Color(0xFF3B5C);
	private static final Color EYE = new Color(20, 20, 24);
	private static final Color POLY_BLUE = new Color(0x1652F0);
	private static final Color POLY_LINE = new Color(0x3D7BFF);
	private static final Font BRAND_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
	private static final Stroke GRID_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
		10f, new float[]{2f, 3f}, 0f);
	private static final Stroke LINE_STROKE = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final int PAD = 6;
	private static final int FLASH_MS = 400;
	private static final int PULSE_MS = 1400;
	private static final int BRAND_BAR_H = 14;

	private MarketPainter()
	{
	}

	static void paint(Graphics2D graphics, int w, int h, String title, MarketGraph graph, Color lineColor,
		boolean resolved, long now, Font titleFont, Font smallFont, String badge, boolean badgeSkull,
		boolean showKalshiBar, boolean showPolymarketBar, MarketGraph polyGraph, long volumePop,
		boolean showDualLine, String kalshiBarText, String polymarketBarText)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			boolean flashOn = resolved && (now / FLASH_MS) % 2 == 0;
			Color accent = flashOn ? RAID_PURPLE : lineColor;

			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			RoundRectangle2D panel = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 8, 8);
			g.setColor(BACKGROUND);
			g.fill(panel);
			if (flashOn)
			{
				g.setColor(new Color(RAID_PURPLE.getRed(), RAID_PURPLE.getGreen(), RAID_PURPLE.getBlue(), 45));
				g.fill(panel);
			}

			int y = 0;
			if (showPolymarketBar)
			{
				g.setColor(POLY_BLUE);
				g.fillRect(0, y, w, BRAND_BAR_H);
				if (polymarketBarText != null && !polymarketBarText.isEmpty())
				{
					drawCenteredBrandText(g, polymarketBarText, w, y, BRAND_BAR_H, Color.WHITE);
				}
				y += BRAND_BAR_H;
			}
			if (showKalshiBar)
			{
				g.setColor(DropMarketConfig.KALSHI_GREEN);
				g.fillRect(0, y, w, BRAND_BAR_H);
				if (kalshiBarText != null && !kalshiBarText.isEmpty())
				{
					drawCenteredBrandText(g, kalshiBarText, w, y, BRAND_BAR_H, Color.BLACK);
				}
				y += BRAND_BAR_H;
			}

			g.setColor(flashOn ? RAID_PURPLE : BORDER);
			g.setStroke(new BasicStroke(flashOn ? 2f : 1f));
			g.draw(panel);

			Font bigFont = titleFont.deriveFont(titleFont.getSize2D() * 1.25f);
			FontMetrics titleFm = g.getFontMetrics(titleFont);
			FontMetrics smallFm = g.getFontMetrics(smallFont);
			FontMetrics bigFm = g.getFontMetrics(bigFont);

			y += PAD;
			int liveBaseline = y + smallFm.getAscent();
			int lx = PAD;
			boolean dotOn = (now / 600) % 2 == 0;
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
					drawSkull(g, lx, liveBaseline + 1, skullSize, MUTED);
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

			int delta = (int) Math.round(graph.current() - graph.previous());
			if (delta != 0)
			{
				String deltaText = Math.abs(delta) + "%";
				Font deltaFont = smallFont.deriveFont(Font.BOLD, smallFont.getSize2D() * 1.15f);
				FontMetrics deltaFm = g.getFontMetrics(deltaFont);
				int triW = 7;
				int mid = y - deltaFm.getAscent() / 2;
				Polygon tri = delta > 0
					? new Polygon(new int[]{x, x + triW, x + triW / 2}, new int[]{mid + 3, mid + 3, mid - 3}, 3)
					: new Polygon(new int[]{x, x + triW, x + triW / 2}, new int[]{mid - 3, mid - 3, mid + 3}, 3);
				g.setColor(accent);
				g.fill(tri);
				g.setFont(deltaFont);
				text(g, deltaText, x + triW + 4, y, accent);
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
				drawChart(g, graph, showDualLine && polyGraph != null ? polyGraph : null, accent, chartLeft, chartTop, chartRight, chartBottom, smallFm, now);
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

	private static void drawCenteredBrandText(Graphics2D g, String s, int w, int barTop, int barH, Color color)
	{
		Font old = g.getFont();
		g.setFont(BRAND_FONT);
		FontMetrics fm = g.getFontMetrics();
		int tx = (w - fm.stringWidth(s)) / 2;
		int ty = barTop + (barH + fm.getAscent() - fm.getDescent()) / 2;
		g.setColor(color);
		g.drawString(s, tx, ty);
		g.setFont(old);
	}

	private static void drawPolymarketBrand(Graphics2D g, int w, int barTop, int barH)
	{
		Font old = g.getFont();
		g.setFont(BRAND_FONT);
		FontMetrics fm = g.getFontMetrics();
		String s = "Polymarket";
		int logoSize = barH - 6;
		int totalW = logoSize + 4 + fm.stringWidth(s);
		int x = (w - totalW) / 2;
		int ty = barTop + (barH + fm.getAscent() - fm.getDescent()) / 2;
		int logoY = barTop + (barH - logoSize) / 2;
		g.setColor(Color.WHITE);
		drawBowtie(g, x, logoY, logoSize);
		g.drawString(s, x + logoSize + 4, ty);
		g.setFont(old);
	}

	private static void drawBowtie(Graphics2D g, int x, int y, int size)
	{
		int half = size / 2;
		g.fill(new Polygon(new int[]{x, x, x + half}, new int[]{y, y + size, y + half}, 3));
		g.fill(new Polygon(new int[]{x + size, x + size, x + half}, new int[]{y, y + size, y + half}, 3));
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

	private static void drawChart(Graphics2D g, MarketGraph graph, MarketGraph polyGraph, Color color, int left, int top, int right,
		int bottom, FontMetrics smallFm, long now)
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

		if (polyGraph != null)
		{
			drawLine(g, polyGraph, POLY_LINE, left, top, right, bottom, false, now);
		}
		drawLine(g, graph, color, left, top, right, bottom, true, now);
	}

	private static void drawLine(Graphics2D g, MarketGraph graph, Color color, int left, int top, int right,
		int bottom, boolean primary, long now)
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

		if (primary)
		{
			Path2D.Double area = new Path2D.Double(line);
			area.lineTo(right, bottom);
			area.lineTo(left, bottom);
			area.closePath();
			g.setPaint(new GradientPaint(0, top, withAlpha(color, 70), 0, bottom, withAlpha(color, 0)));
			g.fill(area);
		}

		g.setStroke(LINE_STROKE);
		g.setColor(color);
		g.draw(line);

		double pulse = (now % PULSE_MS) / (double) PULSE_MS;
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

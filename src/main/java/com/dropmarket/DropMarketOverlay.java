package com.dropmarket;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

class DropMarketOverlay extends Overlay
{
	static final Dimension DEFAULT_SIZE = new Dimension(200, 192);

	private final Client client;
	private final DropMarketPlugin plugin;
	private final DropMarketConfig config;

	@Inject
	DropMarketOverlay(Client client, DropMarketPlugin plugin, DropMarketConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
		setResizable(true);
		setMinimumSize(120);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		Dimension size = getPreferredSize() != null ? getPreferredSize() : DEFAULT_SIZE;
		MarketPainter.paint(graphics, size.width, size.height, plugin.getTitle(), plugin.getGraph(),
			config.graphColor(), plugin.isResolved(), System.currentTimeMillis(),
			FontManager.getRunescapeBoldFont(), FontManager.getRunescapeSmallFont(), plugin.getBadgeText(),
			plugin.isBadgeSkull(), config.showKalshiBar(), config.showPolymarketBar(),
			config.showKalshiBar() && config.showPolymarketBar() ? plugin.getPolyGraph() : null,
			plugin.getVolumePop(), config.showDualLine(), config.kalshiBarText(), config.polymarketBarText());
		return size;
	}
}

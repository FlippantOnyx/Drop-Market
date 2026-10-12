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
	// A ticker strip needs more width than the card to leave room for the title.
	private static final int TICKER_DEFAULT_WIDTH = 260;

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
		Dimension preferred = getPreferredSize() != null ? getPreferredSize() : DEFAULT_SIZE;
		// Compact and Ticker keep the width the player chose but have a fixed height.
		int layoutHeight = MarketPainter.layoutHeight(config);
		boolean untouched = preferred.equals(DEFAULT_SIZE);
		int width = config.overlayLayout() == OverlayLayout.TICKER && untouched ? TICKER_DEFAULT_WIDTH : preferred.width;
		Dimension size = layoutHeight < 0 ? preferred : new Dimension(width, layoutHeight);
		MarketPainter.paint(graphics, size.width, size.height, plugin.getTitle(), plugin.getGraph(),
			plugin.getPolyGraph(), config, plugin.isResolved(), plugin.getTicks(),
			FontManager.getRunescapeBoldFont(), FontManager.getRunescapeSmallFont(), plugin.getBadgeText(),
			plugin.isBadgeSkull(), plugin.getVolumePop(), plugin.getDisplayDelta(), plugin.getDisplayDelta2());
		return size;
	}
}

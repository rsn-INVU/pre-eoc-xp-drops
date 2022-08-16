package com.preeocxp;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.xptracker.XpTrackerService;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.LinkedList;

import static com.preeocxp.PreEocXpPlugin.*;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
//import static net.runelite.client.plugins.preeocxp.PreEocXpPlugin.tickCounter;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class PreEocXpOverlay extends Overlay
{
	private static final int MINIMUM_STEP = 8;
	private static final int TOOLTIP_RECT_SIZE_X = 80;
	private static final int OVERLAY_RECT_SIZE_X = 110;
	private static final int OVERLAY_RECT_SIZE_Y = 14;
	private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 0); //made transparent

	private final Client client;
	private final PreEocXpPlugin plugin;
	private final PreEocXpConfig config;
	private final XpTrackerService xpTrackerService;
	private final TooltipManager tooltipManager;

	private final Tooltip xpTooltip = new Tooltip(new PanelComponent());

	private Skill skillChosen =  Skill.OVERALL;
	private static int lotsThreshold = 100000000; //100 million
	private static int shrinkValue = 0;
	private static final LinkedList<Integer> xpStored = new LinkedList<Integer> ();
	private static final LinkedList<Long> timeValStored = new LinkedList <Long> ();
	private static int xpDropWidth;

	final BufferedImage xpBarMidImage = ImageUtil.loadImageResource(getClass(), "/xpBarMid.png");
	final BufferedImage xpBarLeftImage = ImageUtil.loadImageResource(getClass(), "/xpBarLeft.png");
	final BufferedImage xpBarRightImage = ImageUtil.loadImageResource(getClass(), "/xpBarRight.png");

	Font runescapeChatFont;
	Font runescapeSmallFont;
	Font rsXpDropFont;
	private static float dropSize = 16f;

	@Inject
	private PreEocXpOverlay(
			Client client,
			PreEocXpPlugin plugin,
			PreEocXpConfig config,
			XpTrackerService xpTrackerService,
			TooltipManager tooltipManager
	)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.xpTrackerService = xpTrackerService;
		this.tooltipManager = tooltipManager;
		this.xpTooltip.getComponent().setPreferredSize(new Dimension(TOOLTIP_RECT_SIZE_X, -30));

		//setPosition(OverlayPosition.DETACHED);
		setPosition(OverlayPosition.DYNAMIC);
		setMovable(true);

		//Above HP Bars and Hitsplats - Below Bank and Quest interfaces
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(WidgetID.HEALTH_OVERLAY_BAR_GROUP_ID);
		setPriority(OverlayPriority.HIGH);

		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "XP Tracker overlay"));

	}

	/**
	 * loads the font file, creates the two fonts, with the smallFont being scalable.
	 * lastly registers the fonts.
	 */
	public void registerFont()
	{
		try
		{
			InputStream runescapeChat = this.getClass().getResourceAsStream("/runescape_chat.ttf");
			InputStream runescapeSmall = this.getClass().getResourceAsStream("/runescape_small.ttf");
			InputStream rsXpDrop = this.getClass().getResourceAsStream("/rsxpdrop.ttf");
			//create the font to use. Specify the size!
			runescapeChatFont = Font.createFont(Font.TRUETYPE_FONT, runescapeChat).deriveFont(16f);
			runescapeSmallFont = Font.createFont(Font.TRUETYPE_FONT, runescapeSmall).deriveFont(dropSize);
			//for some reason the size of this font is x2, subtracting 4 makes sure regular mimics the pre eoc size.
			rsXpDropFont = Font.createFont(Font.TRUETYPE_FONT, rsXpDrop).deriveFont(dropSize - 4);

			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			//register the font
			ge.registerFont(runescapeChatFont);
			ge.registerFont(runescapeSmallFont);
			ge.registerFont(rsXpDropFont);
		}
		catch (IOException | FontFormatException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * set the dropsize according to config
	 */
	public void setDropSize ()
	{
		if (config.dropSize() == PreEocXpConfig.OptionEnum.REGULAR) {
			dropSize = 16f;
		} else if (config.dropSize() == PreEocXpConfig.OptionEnum.LARGE) {
			dropSize = 20f;
		} else {
			dropSize = 24f;
		}
	}

	/**
	 * Starts off by setting the font size, lotsLimit and skillChosen according to the Config -if the config was changed.
	 * on startup, the config is loaded and set to true.
	 * Unless null return, triggers renderRectangle.
	 * @param graphics
	 * @return if LoginXp is 0, no xp has been fetched, so no need to start rendering. (even lvl 3s aren't 0 xp due to hp)
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (PreEocXpPlugin.getConfigUpdateState()) {

			setDropSize();
			registerFont();
			skillChosen = config.displaySkill();
			lotsThreshold = config.lotsLimit();
			//checkTwelve();

			PreEocXpPlugin.setConfigUpdateState(false);
		}

		graphics.setFont(runescapeChatFont);

		long xpFetched = PreEocXpPlugin.getLoginXp();

		//if loginxp is 0, we don't want to run yet. If a listed widget was active on tick - also don't render.
		if ( xpFetched == 0)
		{
			return null;
		}
		int curDrawPosition = 0;

		renderRectangle(graphics, curDrawPosition, 0, getBounds());
		curDrawPosition += MINIMUM_STEP;

		return new Dimension(OVERLAY_RECT_SIZE_X, OVERLAY_RECT_SIZE_Y);
	}

	/**
	 * "master method" - triggers and controls the rendering of the rectangle and it's asset, tooltips, and text values.
	 * Checks if new xp has been sent. If so, adds the starting time and the xp drop to lists.
	 * After 1.8 seconds, these values are removed from the lists (as these are no longer to be rendered)
	 * Initiates the drawing of xp drops, the counter value and the background rectangle.
	 * Lastly checks whether the counter is hovered to display tooltips.
	 * @param graphics
	 * @param x initial x position
	 * @param y initial y position
	 * @param bounds - the bounds of the rectangle - used to check whether to display tooltips or not.
	 */
	private void renderRectangle(Graphics2D graphics, int x, int y, Rectangle bounds)
	{
		graphics.setColor(OVERLAY_COLOR);

		Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX() - bounds.x;
		int mouseY = mouse.getY() - bounds.y;


		if (sentXp)
		{
			sentXp = false;

			long startTime = System.currentTimeMillis();

			//an extra check, as scene-updates would trigger this with stored values for unknown reasons
			if (xpDrop != 0)
			{
				timeValStored.add(startTime);
				xpStored.add(xpDrop);
				xpDrop = 0;
			}

			int n = 0;

			for (int i = 0; i < xpStored.size(); i++)
			{
				//calc elapsedTime
				if ( startTime - timeValStored.get(i) > 1800)
				{
					xpStored.remove(n);
					timeValStored.remove(n);
				}
				else
				{
					n ++;
				}
			}
		}
		else
		{
			long currentTime = System.currentTimeMillis();
			int n = 0;
			for (int i = 0; i < xpStored.size(); i++)
			{
				//calc elapsedTime
				if (currentTime - timeValStored.get(i) > 1800)
				{
					xpStored.remove(n);
					timeValStored.remove(n);
				}
				n++;
			}
		}
		if (!config.onlyCounter()) {
			if (!config.enableTwelve()) {
				drawTenDrop(graphics, 0, 0);
			}
			else drawTwelveDrop(graphics);
		}


		if (!config.onlyDrops()) {
			Rectangle backgroundRectangle = drawRectangle(graphics, x, y);
			drawXpLabel(graphics, 0, 0);
			// If mouse is hovering the box
			if (backgroundRectangle.contains(mouseX, mouseY))
			{
				//prev x,y being mouse position
				if (config.enableTooltips())
				{
					drawTooltip();
				}
			}
			if (config.onlyCounter()) {
				Widget xpDisplay = client.getWidget(WidgetInfo.EXPERIENCE_TRACKER);
				xpDisplay.setHidden(false);
			}
		}
	}

	/**
	 * Draws the text to be rendered on top of the xp counter.
	 * Sets the font to the ChatFont, sets the colors of the text, and calculates its position, depending on the size.
	 * This is done stepwise currently, as it seems to have been that way in 2010 (atleast for large and small values...
	 * Finally draws the total xp, or lots, if the threshold was met.
	 * @param graphics
	 * @param x initial x position
	 * @param y initial y position
	 */
	private void drawXpLabel(Graphics2D graphics, int x, int y)
	{
		graphics.setFont( runescapeChatFont );
		DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

		final FontMetrics metrics = graphics.getFontMetrics();

		// runescape chat, 12p, shadow black.

		int drawX = x ;
		int drawY = y + OVERLAY_RECT_SIZE_Y - 1 ;


		//int skillXp = (client.getSkillExperience(skillChosen));

		Color xpColor = Color.WHITE;
		Color lotsColor = Color.RED;
		String formattedXp;
		int offset = 5;

		//get the length of the skillXp to scale rectangle
		int xpLength = (int) (Math.log10(skillXp) + 1);

		if (xpLength < 7) //less than a million
		{
			shrinkValue = 25;
		}

		else if (xpLength < 8) //less than 10 million
		{
			shrinkValue = 20;
		}

		else if (xpLength < 9) //less than 100 million
		{
			shrinkValue = 10;
		}
		//1b +
		else if (xpLength > 10)
		{
			shrinkValue = - 10;
		}

		else //100m +
		{
			shrinkValue = 0;
		}

		if (skillXp > lotsThreshold)
		{
			shrinkValue = 25;
		}

		if (skillXp < lotsThreshold)
		{
			formattedXp = decimalFormat.format(skillXp);
			int xpWidth = metrics.stringWidth(formattedXp);
			OverlayUtil.renderTextLocation(graphics, new Point(drawX + (OVERLAY_RECT_SIZE_X  - (xpWidth + offset)), drawY), formattedXp, xpColor);
		}

		else
		{
			formattedXp = "Lots!";
			int lotsWidth = metrics.stringWidth(formattedXp);
			OverlayUtil.renderTextLocation(graphics, new Point(drawX + (OVERLAY_RECT_SIZE_X  - ( lotsWidth + offset)), drawY), formattedXp, lotsColor);
		}
		OverlayUtil.renderTextLocation(graphics, new Point(drawX + offset + shrinkValue, drawY), "XP:", xpColor);
	}

	/**
	 * Draws the xp drop.
	 * Sets the font and color of the xp drop.
	 * Cycle through all the currently stored xp drops and their time pairs.
	 * If 1.2 seconds have not passed - draw the position of the xp drop based on the time passed (same animation speed regardless of fps)
	 * After a drop has "existed" for 1.2 seconds or more - hold it for .6 seconds, until it disappears.
	 * @param graphics
	 * @param x initial x position of the xp Drop
	 * @param y initial y position of the xp Drop
	 */
	private void drawTenDrop(Graphics2D graphics, int x, int y)
	{
		graphics.setFont(runescapeSmallFont);
		final FontMetrics metrics = graphics.getFontMetrics();
		// runescape chat, 12p, shadow black.
		int drawXVal = x;
		int drawYVal;

		Color dropColor = new Color(250, 141, 17);

		for (int i = 0; i < xpStored.size(); i++)
		{
			long animationTimer = Math.min((System.currentTimeMillis() - timeValStored.get(i)) , 1200);

			drawYVal = ((int) ((animationTimer) / 17) + 24);

			DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

			String skillXpString = decimalFormat.format(xpStored.get(i));
			xpDropWidth = metrics.stringWidth(skillXpString + "xp");

			OverlayUtil.renderTextLocation(graphics, new Point(drawXVal + (OVERLAY_RECT_SIZE_X - xpDropWidth), drawYVal), skillXpString + "xp", dropColor);
		}
	}

	/**
	 * Draws the xp drop in a 2012 style.
	 * Sets the font and color of the xp drop.
	 * Cycle through all the currently stored xp drops and their time pairs.
	 * If 0.6 seconds have not passed - draw the position of the xp drop based on the time passed (same animation speed regardless of fps)
	 * After a drop has "existed" for 0.6 seconds or more - hold it for 1.2 seconds, and fade it, until it disappears.
	 * @param graphics
	 //* @param x initial x position of the xp Drop
	 //* @param y initial y position of the xp Drop
	 */
	private void drawTwelveDrop(Graphics2D graphics)
	{

		graphics.setFont(rsXpDropFont);

		//Center the xp drop, regardless of label position and client size.
		//getBounds() retrieves the label's position, which is also the (0,0) coordinate, then offset that from the center value
		//to get the actual center (tm).
		int overlayLocationX = client.getCenterX() - (int) getBounds().getX();
		//offset y 4 pixels cause that's just how it was. (It ends up centered over the hover text in-game - probably was jagex' anchor?)
		int overlayLocationY = client.getCenterY() -(int) getBounds().getY() - 4;

		//draw via a helper class, to assign unique opacity values to each xp drop.
		PreEocXp2012Overlay helper = new PreEocXp2012Overlay( plugin);


		for (int i = 0; i < xpStored.size(); i++)
		{
			long timePassed = System.currentTimeMillis() - timeValStored.get(i);
			helper.drawTwelveDrop(graphics, overlayLocationX, overlayLocationY, xpStored.get(i),(int)timePassed);
		}
	}

	/**
	 * Draws the background rectangle, and the xp counter graphic.
	 * Gets the position of the 2 edge images, and scales the center one according to xp value, for cosmetic reasons.
	 * Gets the position and scale dynamically, in case future expansion here would be desired (such as replacing the graphic)
	 * @param graphics
	 * @param x
	 * @param y
	 * @return
	 */
	private Rectangle drawRectangle(Graphics2D graphics, int x , int y)
	{
		Rectangle rectangle = new Rectangle(OVERLAY_RECT_SIZE_X - shrinkValue, OVERLAY_RECT_SIZE_Y);
		rectangle.setLocation(x + shrinkValue, y);

		int imgHeight = xpBarMidImage.getHeight();
		int fixedWidth = xpBarLeftImage.getWidth();

		int yHeight = y - (imgHeight - OVERLAY_RECT_SIZE_Y) / 2;

		graphics.drawImage(xpBarMidImage, rectangle.x + fixedWidth , yHeight, rectangle.width - ( 2 * fixedWidth ), imgHeight, null);
		graphics.drawImage(xpBarLeftImage, rectangle.x, yHeight, null);
		graphics.drawImage(xpBarRightImage, rectangle.x + rectangle.width - fixedWidth , yHeight, null);

		return rectangle;
	}

	/**
	 * draws the tooltip displaying what skill is being...displayed.
	 */
	private void drawTooltip()
	{
		final PanelComponent xpTooltip = (PanelComponent) this.xpTooltip.getComponent();
		xpTooltip.getChildren().clear();

		xpTooltip.getChildren().add(LineComponent.builder()
				.left(" Skill: " + skillChosen.getName())
				.leftColor(Color.WHITE)
				.build());

		tooltipManager.add(this.xpTooltip);
	}
}

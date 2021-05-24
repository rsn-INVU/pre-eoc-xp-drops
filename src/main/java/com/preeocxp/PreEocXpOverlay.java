package com.preeocxp;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.client.plugins.xptracker.XpTrackerService;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;

import static com.preeocxp.PreEocXpPlugin.sentXp;
import static com.preeocxp.PreEocXpPlugin.xpDrop;
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
	private static int xpWidth;
	private static final LinkedList<Integer> xpStored = new LinkedList<Integer> ();
	private static final LinkedList<Long> timeValStored = new LinkedList <Long> ();
	private static int xpDropWidth;

	File xpBarMid = new File("/Users/slushangel/Desktop/xpBarMid.png");
	File xpBarLeft = new File("/Users/slushangel/Desktop/xpBarLeft.png");
	File xpBarRight = new File("/Users/slushangel/Desktop/xpBarRight.png");
	private static File runescapeChat = new File("/Users/slushangel/Desktop/runescape_chat.ttf");
	Font runescapeChatFont = new Font ("runescape_chat.ttf", Font.TRUETYPE_FONT, 16);
	private static File runescapeSmall = new File("/Users/slushangel/Desktop/runescape_small.ttf");
	Font runescapeSmallFont = new Font ("runescape_chat.ttf", Font.TRUETYPE_FONT, 16);
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

		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);

		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "XP Tracker overlay"));
	}

	public void registerFont()
	{
		try
		{
			//create the font to use. Specify the size!
			runescapeChatFont = Font.createFont(Font.TRUETYPE_FONT, runescapeChat).deriveFont(16f);
			runescapeSmallFont = Font.createFont(Font.TRUETYPE_FONT, runescapeSmall).deriveFont(dropSize);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			//register the font
			ge.registerFont(runescapeChatFont);
			ge.registerFont(runescapeSmallFont);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (FontFormatException e)
		{
			e.printStackTrace();
		}
	}
	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.dropSize() == PreEocXpConfig.OptionEnum.REGULAR)
		{
			dropSize = 16f;
		}
		else if (config.dropSize() == PreEocXpConfig.OptionEnum.LARGE)
		{
			dropSize = 20f;
		}
		else
		{
			dropSize = 24f;
		}
		registerFont();
		graphics.setFont(runescapeChatFont);
		skillChosen = config.displaySkill();
		lotsThreshold = config.lotsLimit();

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

	/*
	"master method" - triggers and controls the rendering of the rectangle and it's asset, tooltips, and text values.
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
			drawXpDrop(graphics, 0, 0);
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
			drawXpDrop(graphics, 0, 0);
		}

		Rectangle backgroundRectangle = drawRectangle(graphics, x, y);

		drawXpLabel(graphics, 0, 0);

		// If mouse is hovering the globe
		if (backgroundRectangle.contains(mouseX, mouseY))
		{
			//prev x,y being mouse position
			if (config.enableTooltips())
			{
				drawTooltip();
			}
		}
	}

	private void drawXpLabel(Graphics2D graphics, int x, int y)
	{
		graphics.setFont( runescapeChatFont );
		DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

		final FontMetrics metrics = graphics.getFontMetrics();

		// runescape chat, 12p, shadow black.

		int drawX = x ;
		int drawY = y + OVERLAY_RECT_SIZE_Y - 1 ;

		int skillXp = (client.getSkillExperience(skillChosen));

		Color xpColor = Color.WHITE;
		Color lotsColor = Color.RED;
		String formattedXp = decimalFormat.format(skillXp);
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

	private void drawXpDrop(Graphics2D graphics, int x, int y)
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
		//OverlayUtil.renderTextLocation(graphics, new Point(0, 0), "Gametick" + tickCounter, Color.RED);
	}

	private Rectangle drawRectangle(Graphics2D graphics, int x , int y)
	{
		BufferedImage xpBarMidImage = null;
		BufferedImage xpBarLeftImage = null;
		BufferedImage xpBarRightImage = null;

		try
		{
			xpBarMidImage = ImageIO.read(xpBarMid);
			xpBarLeftImage = ImageIO.read(xpBarLeft);
			xpBarRightImage = ImageIO.read(xpBarRight);
		}
		catch (IOException e)
		{
		}

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

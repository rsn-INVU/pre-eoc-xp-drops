package com.preeocxp;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.util.ColorUtil;
import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;

public class PreEocXp2012Overlay extends Overlay
{

    private static int xpDropWidth;

    @Inject
    public PreEocXp2012Overlay(
            Client client,
            PreEocXpPlugin plugin,
            PreEocXpConfig config
    ) {
        super(plugin);
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
        return new Dimension();
    }

    /**
     * Draws the xp drop in a 2012 style.
     * Sets the font and color of the xp drop.
     * Cycle through all the currently stored xp drops and their time pairs.
     * If 0.6 seconds have not passed - draw the position of the xp drop based on the time passed (same animation speed regardless of fps)
     * After a drop has "existed" for 0.6 seconds or more - hold it for 1.2 seconds, and fade it, until it disappears.
     * @param graphics
     * @param x the x position entered - client centerpoint
     * @param y the y position entered - client centerpoint
     * @param xpDrop the xpDrop value sent
     * @param timePassed time passed since drop creation
     */
    public void drawTwelveDrop(Graphics2D graphics, int x, int y, int xpDrop, int timePassed)
    {
        final FontMetrics metrics = graphics.getFontMetrics();

        int drawXVal = x;
        int drawYVal = y + ( metrics.getHeight() / 2 );

        int opacityValue = 255;
        Color dropColor = new Color(250, 141, 17);
        Color shadowColor = new Color(0,0,0);

        long animationTimer = Math.min((timePassed) , 600);

        //moves 150 pixels on the Y axis
        drawYVal = ((int) ((-1)*((animationTimer) / 4) ) + drawYVal);
        //time based fade
        if (timePassed > 600) {
            opacityValue = 255 - (int)((timePassed - 600)/4.7);
        }

        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

        String skillXpString = decimalFormat.format(xpDrop);
        String xpDropString = "+" + skillXpString + "xp";
        xpDropWidth = metrics.stringWidth(xpDropString);
        //draw shadow
        graphics.setColor(ColorUtil.colorWithAlpha(shadowColor,opacityValue));
        graphics.drawString(xpDropString, drawXVal - (xpDropWidth/2) + 1, drawYVal +1 );
        //draw text
        graphics.setColor(ColorUtil.colorWithAlpha(dropColor,opacityValue));
        graphics.drawString(xpDropString, drawXVal  - (xpDropWidth/2), drawYVal);

        }
    }

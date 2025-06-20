package com.jouriroosjen.hardcoreSMPPlugin.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Utility class for all things regarding images
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class ImageUtils {
    /**
     * Converts the input image to grayscale.
     *
     * @param image The original BufferedImage.
     * @return A new BufferedImage in grayscale.
     */
    public static BufferedImage toGrayscale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        // Loop over every pixel
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);

                // Convert to grayscale using the luminance formula
                int gray = (int) (color.getRed() * 0.3 + color.getGreen() * 0.59 + color.getBlue() * 0.11);

                // Preserve alpha transparency
                Color bw = new Color(gray, gray, gray, color.getAlpha());
                grayImage.setRGB(x, y, bw.getRGB());
            }
        }

        return grayImage;
    }

    /**
     * Draws a red cross across the entire image.
     *
     * @param image The image to draw on.
     */
    public static void drawRedCross(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(10));
        graphics.drawLine(0, 0, image.getWidth(), image.getHeight());
        graphics.drawLine(0, image.getHeight(), image.getWidth(), 0);
        graphics.dispose();
    }

    /**
     * Draws a two-line death message on the provided image.
     *
     * @param image        The BufferedImage on which to draw the text.
     * @param playerName   The player’s name to display at the top of the image.
     * @param deathMessage The death message to display at the bottom of the image.
     * @param fontFile     An InputStream providing the font file.
     */
    public static void drawDeathText(BufferedImage image, String playerName, String deathMessage, InputStream fontFile) {
        try {
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.setColor(Color.WHITE);

            // Create and set custom font
            int fontSize = image.getWidth() / 10;
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.BOLD, fontSize);
            graphics.setFont(font);

            FontMetrics fontMetrics = graphics.getFontMetrics();

            int centerX = image.getWidth() / 2;

            // Draw top line
            int topLineWidth = fontMetrics.stringWidth(playerName);
            int topLineY = fontSize + fontMetrics.getAscent();
            graphics.drawString(playerName, centerX - topLineWidth / 2, topLineY);

            // Draw bottom line
            String bottomLine = deathMessage.toUpperCase();
            int bottomLineWidth = fontMetrics.stringWidth(bottomLine);
            int bottomLineY = image.getHeight() - fontSize;
            graphics.drawString(bottomLine, centerX - bottomLineWidth / 2, bottomLineY);

            graphics.dispose();
        } catch (Exception e) {
            System.out.println("Failed to draw death text!");
            e.printStackTrace();
        }
    }
}

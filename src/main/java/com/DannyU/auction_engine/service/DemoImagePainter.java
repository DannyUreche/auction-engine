package com.DannyU.auction_engine.service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Random;

// Draws "studio photos" for demo listings with Java2D: a soft backdrop, a floor shadow and a stylized
// drawing of the object in the listing's color. Photo 1 is the full view, photo 2 a dark studio shot
// from a slight angle, photo 3 a close-up. No external images are used.
final class DemoImagePainter {

    enum Shape {
        PHONE, LAPTOP, MONITOR, HEADPHONES, GAMEPAD, CAMERA, SPEAKER, DRONE,
        WATCH, RING, NECKLACE, PAINTING, VASE, COIN, BOOK,
        SHIRT, SHOE, BAG, GLASSES, CHAIR, LAMP, PLANT,
        BIKE, BALL, DUMBBELL, RACKET, SCOOTER, GUITAR, VINYL, PIANO,
        CAR, MOTO, BOX, TEDDY, TELESCOPE, TENT, SNOWBOARD
    }

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private DemoImagePainter() {
    }

    static byte[] paint(Shape shape, int rgb, int photoIndex, Random random) {
        Color color = new Color(rgb);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int variant = photoIndex % 3;
        boolean dark = variant == 1;
        backdrop(g, color, dark, random);

        // Fit the drawing: measure it once, then center it and scale it to a common size
        Rectangle2D bounds = measure(shape, color);
        double fit = Math.min(Math.min(330 / bounds.getHeight(), 500 / bounds.getWidth()), 1.7);
        double scale = fit * (variant == 2 ? 1.6 : variant == 1 ? 0.88 : 1.0);
        double angle = variant == 1 ? -0.1 : 0;
        double cx = WIDTH / 2.0 + (variant == 2 ? 50 : 0);
        double cy = HEIGHT / 2.0 - 15 + (variant == 2 ? 60 : 0);

        // Floor shadow under the object (not rotated)
        double shadowWidth = Math.max(bounds.getWidth(), 160) * scale * 0.9;
        double floor = cy + bounds.getHeight() / 2 * scale;
        g.setColor(new Color(0, 0, 0, dark ? 90 : 45));
        g.fill(new Ellipse2D.Double(cx - shadowWidth / 2, floor - 6 * scale, shadowWidth, 24 * scale));

        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.rotate(angle);
        g.translate(-bounds.getCenterX(), -bounds.getCenterY());
        draw(g, shape, color);
        g.setTransform(saved);

        // Small caption, like a watermark
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        g.setColor(dark ? new Color(255, 255, 255, 120) : new Color(15, 23, 42, 110));
        g.drawString("Poza " + (photoIndex + 1) + " · date demo", 24, HEIGHT - 22);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    // Bounding box of a drawing in its own coordinates (draws it once off-screen and scans the pixels)
    private static Rectangle2D measure(Shape shape, Color color) {
        int size = 700;
        BufferedImage probe = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        g.translate(size / 2.0, size / 2.0);
        draw(g, shape, color);
        g.dispose();
        int minX = size, minY = size, maxX = 0, maxY = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if ((probe.getRGB(x, y) >>> 24) > 40) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX <= minX || maxY <= minY) {
            return new Rectangle2D.Double(-150, -150, 300, 300);
        }
        return new Rectangle2D.Double(minX - size / 2.0, minY - size / 2.0, maxX - minX, maxY - minY);
    }

    private static void backdrop(Graphics2D g, Color c, boolean dark, Random random) {
        Color top = dark ? mix(c, Color.BLACK, 0.78) : mix(c, Color.WHITE, 0.9);
        Color bottom = dark ? mix(c, Color.BLACK, 0.55) : mix(c, Color.WHITE, 0.72);
        g.setPaint(new GradientPaint(0, 0, top, 0, HEIGHT, bottom));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // Spotlight behind the object
        g.setPaint(new RadialGradientPaint(new Point2D.Double(WIDTH / 2.0, HEIGHT * 0.42), 360,
                new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, dark ? 40 : 150), new Color(255, 255, 255, 0)}));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // Floor
        g.setPaint(new GradientPaint(0, HEIGHT * 0.74f, new Color(0, 0, 0, 0), 0, HEIGHT, new Color(0, 0, 0, dark ? 70 : 22)));
        g.fillRect(0, (int) (HEIGHT * 0.74), WIDTH, HEIGHT);
        // A few soft bokeh dots
        for (int i = 0; i < 6; i++) {
            int size = 30 + random.nextInt(90);
            g.setColor(new Color(255, 255, 255, dark ? 10 + random.nextInt(14) : 30 + random.nextInt(40)));
            g.fill(new Ellipse2D.Double(random.nextInt(WIDTH), random.nextInt(HEIGHT / 2), size, size));
        }
    }

    // ------------------------------------------------------------------ helpers

    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Paint vertical(double y1, Color c1, double y2, Color c2) {
        return new GradientPaint(0, (float) y1, c1, 0, (float) y2, c2);
    }

    private static Paint diagonal(double x1, double y1, Color c1, double x2, double y2, Color c2) {
        return new GradientPaint((float) x1, (float) y1, c1, (float) x2, (float) y2, c2);
    }

    private static java.awt.Shape rr(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }

    private static java.awt.Shape circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
    }

    private static java.awt.Shape oval(double cx, double cy, double rx, double ry) {
        return new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
    }

    private static java.awt.Shape poly(double... xy) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            path.lineTo(xy[i], xy[i + 1]);
        }
        path.closePath();
        return path;
    }

    private static void line(Graphics2D g, Color c, float width, double x1, double y1, double x2, double y2) {
        g.setColor(c);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void stroke(Graphics2D g, java.awt.Shape s, Color c, float width) {
        g.setColor(c);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    private static void fill(Graphics2D g, java.awt.Shape s, Paint p) {
        g.setPaint(p);
        g.fill(s);
    }

    // Glossy screen: indigo -> violet with a diagonal reflection
    private static void screen(Graphics2D g, java.awt.Shape s, Color c) {
        Rectangle2D b = s.getBounds2D();
        fill(g, s, diagonal(b.getMinX(), b.getMinY(), mix(c, new Color(0x6366f1), 0.6),
                b.getMaxX(), b.getMaxY(), mix(c, new Color(0xc026d3), 0.55)));
        java.awt.Shape oldClip = g.getClip();
        g.clip(s);
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(poly(b.getMinX(), b.getMinY(), b.getMinX() + b.getWidth() * 0.55, b.getMinY(),
                b.getMinX(), b.getMinY() + b.getHeight() * 0.7));
        g.setClip(oldClip);
    }

    private static void wheel(Graphics2D g, double cx, double cy, double r, Color tire) {
        fill(g, circle(cx, cy, r), tire);
        fill(g, circle(cx, cy, r * 0.62), new Color(0xcbd5e1));
        fill(g, circle(cx, cy, r * 0.25), new Color(0x475569));
    }

    // ------------------------------------------------------------------ shapes

    private static void draw(Graphics2D g, Shape shape, Color c) {
        Color dark = mix(c, Color.BLACK, 0.4);
        Color light = mix(c, Color.WHITE, 0.45);
        Color metal = new Color(0xcbd5e1);
        Color ink = new Color(0x0f172a);
        switch (shape) {
            case PHONE -> {
                fill(g, rr(-95, -165, 190, 330, 34), vertical(-165, light, 165, dark));
                screen(g, rr(-83, -152, 166, 304, 26), c);
                fill(g, rr(-28, -144, 56, 14, 7), ink);
            }
            case LAPTOP -> {
                fill(g, rr(-155, -135, 310, 200, 14), vertical(-135, dark, 65, ink));
                screen(g, rr(-142, -122, 284, 172, 6), c);
                fill(g, poly(-185, 66, 185, 66, 200, 92, -200, 92), vertical(66, mix(c, Color.WHITE, 0.7), 92, light));
                fill(g, rr(-40, 66, 80, 8, 4), alpha(dark, 120));
            }
            case MONITOR -> {
                fill(g, rr(-175, -145, 350, 215, 12), vertical(-145, dark, 70, ink));
                screen(g, rr(-163, -133, 326, 182, 4), c);
                fill(g, rr(-14, 70, 28, 62, 6), vertical(70, metal, 132, new Color(0x94a3b8)));
                fill(g, rr(-80, 126, 160, 16, 8), new Color(0x94a3b8));
            }
            case HEADPHONES -> {
                stroke(g, new Arc2D.Double(-125, -150, 250, 250, 0, 180, Arc2D.OPEN), c, 26);
                for (int side : new int[]{-1, 1}) {
                    double x = side < 0 ? -160 : 90;
                    fill(g, rr(x, -15, 70, 130, 32), vertical(-15, light, 115, dark));
                    fill(g, rr(x + (side < 0 ? 46 : -6), 0, 30, 100, 15), ink);
                }
            }
            case GAMEPAD -> {
                fill(g, rr(-115, -75, 230, 150, 12), ink);
                screen(g, rr(-102, -62, 204, 124, 4), c);
                fill(g, rr(-175, -75, 64, 150, 30), vertical(-75, light, 75, c));
                fill(g, rr(111, -75, 64, 150, 30), vertical(-75, new Color(0x60a5fa), 75, new Color(0x1d4ed8)));
                fill(g, circle(-143, -30, 13), ink);
                fill(g, circle(143, 30, 13), ink);
                for (double[] p : new double[][]{{143, -45}, {128, -30}, {158, -30}, {143, -15}}) {
                    fill(g, circle(p[0], p[1], 6), ink);
                }
            }
            case CAMERA -> {
                fill(g, rr(-60, -125, 120, 60, 12), vertical(-125, light, -65, c));
                fill(g, rr(-165, -85, 330, 195, 24), vertical(-85, light, 110, dark));
                fill(g, rr(-165, -85, 60, 195, 24), alpha(ink, 90));
                fill(g, circle(0, 15, 88), ink);
                fill(g, circle(0, 15, 66), vertical(-51, new Color(0x64748b), 81, new Color(0x1e293b)));
                fill(g, circle(0, 15, 42), new Color(0x020617));
                fill(g, oval(-16, -2, 14, 10), new Color(255, 255, 255, 120));
                fill(g, circle(120, -100, 14), metal);
            }
            case SPEAKER -> {
                fill(g, rr(-175, -78, 350, 156, 78), vertical(-78, light, 78, dark));
                g.setColor(alpha(ink, 60));
                for (int x = -130; x <= 130; x += 16) {
                    for (int y = -50; y <= 50; y += 16) {
                        g.fill(circle(x, y, 3));
                    }
                }
                fill(g, rr(-180, -60, 26, 120, 13), ink);
                fill(g, rr(154, -60, 26, 120, 13), ink);
                fill(g, rr(-30, -10, 60, 20, 6), new Color(255, 255, 255, 200));
            }
            case DRONE -> {
                for (double[] p : new double[][]{{-140, -90}, {140, -90}, {-140, 90}, {140, 90}}) {
                    line(g, dark, 16, 0, 0, p[0], p[1]);
                    fill(g, circle(p[0], p[1], 18), ink);
                    fill(g, oval(p[0], p[1] - 14, 78, 12), new Color(255, 255, 255, 120));
                }
                fill(g, rr(-65, -40, 130, 80, 28), vertical(-40, light, 40, c));
                fill(g, circle(0, 46, 18), ink);
                fill(g, circle(0, 46, 8), new Color(0x334155));
            }
            case WATCH -> {
                Color strap = mix(c, Color.BLACK, 0.35);
                fill(g, rr(-58, -200, 116, 400, 22), vertical(-200, strap, 200, mix(strap, Color.BLACK, 0.3)));
                fill(g, rr(100, -14, 24, 28, 5), metal);
                fill(g, circle(0, 0, 110), diagonal(-110, -110, Color.WHITE, 110, 110, new Color(0x64748b)));
                fill(g, circle(0, 0, 92), vertical(-92, light, 92, c));
                for (int h = 0; h < 12; h++) {
                    double a = Math.PI * 2 * h / 12;
                    line(g, Color.WHITE, h % 3 == 0 ? 7 : 4, Math.sin(a) * 72, -Math.cos(a) * 72, Math.sin(a) * 84, -Math.cos(a) * 84);
                }
                line(g, Color.WHITE, 8, 0, 0, 30, -42);
                line(g, Color.WHITE, 5, 0, 0, -10, -68);
                fill(g, circle(0, 0, 8), Color.WHITE);
                fill(g, oval(-40, -55, 36, 18), new Color(255, 255, 255, 45));
            }
            case RING -> {
                Area band = new Area(circle(0, 50, 120));
                band.subtract(new Area(circle(0, 50, 96)));
                fill(g, band, diagonal(-120, -70, Color.WHITE, 120, 170, mix(c, new Color(0x64748b), 0.3)));
                for (int x : new int[]{-30, 30}) {
                    line(g, metal, 8, x * 0.8, -70, x, -100);
                }
                java.awt.Shape gem = poly(-48, -112, 48, -112, 66, -88, 0, -20, -66, -88);
                fill(g, gem, diagonal(-60, -112, Color.WHITE, 60, -20, light));
                stroke(g, gem, alpha(dark, 120), 2);
                line(g, alpha(Color.WHITE, 200), 2, -66, -88, 66, -88);
                line(g, alpha(dark, 90), 2, -20, -112, 0, -20);
                line(g, alpha(dark, 90), 2, 20, -112, 0, -20);
            }
            case NECKLACE -> {
                for (double a = 0.04 * Math.PI; a <= 0.96 * Math.PI; a += 0.045 * Math.PI) {
                    double x = 175 * Math.cos(a);
                    double y = -90 + 175 * Math.sin(a);
                    fill(g, circle(x, y, 15), diagonal(x - 15, y - 15, Color.WHITE, x + 15, y + 15, mix(c, new Color(0x94a3b8), 0.4)));
                }
                fill(g, oval(0, 125, 26, 34), diagonal(-26, 90, Color.WHITE, 26, 160, mix(c, new Color(0x7c3aed), 0.5)));
                fill(g, oval(-8, 112, 7, 10), new Color(255, 255, 255, 170));
            }
            case PAINTING -> {
                fill(g, rr(-175, -135, 350, 270, 6), diagonal(-175, -135, new Color(0xfde68a), 175, 135, new Color(0x854d0e)));
                java.awt.Shape canvas = new Rectangle2D.Double(-148, -108, 296, 216);
                fill(g, canvas, vertical(-108, mix(c, Color.WHITE, 0.55), 40, mix(c, Color.WHITE, 0.9)));
                java.awt.Shape oldClip = g.getClip();
                g.clip(canvas);
                fill(g, circle(70, -45, 28), new Color(0xfbbf24));
                fill(g, poly(-160, 40, -90, -30, -20, 20, 40, -40, 160, 40, 160, 120, -160, 120), c);
                fill(g, poly(-160, 70, -60, 20, 60, 60, 160, 30, 160, 120, -160, 120), dark);
                g.setClip(oldClip);
            }
            case VASE -> {
                Path2D.Double vase = new Path2D.Double();
                vase.moveTo(-38, -165);
                vase.lineTo(38, -165);
                vase.curveTo(38, -110, 125, -85, 125, 15);
                vase.curveTo(125, 110, 75, 150, 52, 162);
                vase.lineTo(-52, 162);
                vase.curveTo(-75, 150, -125, 110, -125, 15);
                vase.curveTo(-125, -85, -38, -110, -38, -165);
                vase.closePath();
                fill(g, vase, new GradientPaint(-125, 0, light, 125, 0, dark));
                java.awt.Shape oldClip = g.getClip();
                g.clip(vase);
                fill(g, new Rectangle2D.Double(-130, -10, 260, 26), new Color(0xfbbf24));
                fill(g, new Rectangle2D.Double(-130, 40, 260, 8), new Color(0xfde68a));
                fill(g, oval(-60, -20, 18, 90), new Color(255, 255, 255, 55));
                g.setClip(oldClip);
                fill(g, oval(0, -165, 38, 8), dark);
            }
            case COIN -> {
                fill(g, circle(0, 0, 150), diagonal(-150, -150, mix(c, Color.WHITE, 0.6), 150, 150, dark));
                stroke(g, circle(0, 0, 128), alpha(mix(c, Color.WHITE, 0.7), 200), 4);
                for (int i = 0; i < 40; i++) {
                    double a = Math.PI * 2 * i / 40;
                    fill(g, circle(Math.cos(a) * 140, Math.sin(a) * 140, 3), alpha(dark, 160));
                }
                g.setFont(new Font(Font.SERIF, Font.BOLD, 92));
                g.setColor(alpha(mix(c, Color.BLACK, 0.55), 170));
                drawCentered(g, "LEI", 0, 30);
                g.setFont(new Font(Font.SERIF, Font.BOLD, 26));
                drawCentered(g, "ROMANIA", 0, -60);
                fill(g, oval(-60, -80, 50, 22), new Color(255, 255, 255, 50));
            }
            case BOOK -> {
                Color[] covers = {c, mix(c, new Color(0x1e3a8a), 0.5), dark};
                double[][] books = {{-155, 50, 310}, {-130, -18, 280}, {-145, -86, 265}};
                for (int i = 0; i < 3; i++) {
                    double x = books[i][0], y = books[i][1], w = books[i][2];
                    fill(g, rr(x, y, w, 62, 6), vertical(y, mix(covers[i], Color.WHITE, 0.2), y + 62, covers[i]));
                    fill(g, new Rectangle2D.Double(x + w - 14, y + 6, 10, 50), new Color(0xf8fafc));
                    line(g, new Color(0xfbbf24), 3, x + 30, y + 14, x + 30, y + 48);
                    line(g, new Color(0xfbbf24), 3, x + 44, y + 14, x + 44, y + 48);
                }
            }
            case SHIRT -> {
                java.awt.Shape body = poly(-62, -155, -155, -112, -200, 25, -145, 45, -118, -35, -118, 165, 118, 165,
                        118, -35, 145, 45, 200, 25, 155, -112, 62, -155, 0, -95);
                fill(g, body, vertical(-155, light, 165, dark));
                line(g, alpha(ink, 90), 5, -62, -155, 0, -40);
                line(g, alpha(ink, 90), 5, 62, -155, 0, -40);
                for (int y = -10; y <= 130; y += 45) {
                    fill(g, circle(0, y, 7), alpha(ink, 140));
                }
                fill(g, rr(-95, 40, 50, 8, 3), alpha(ink, 70));
                fill(g, rr(45, 40, 50, 8, 3), alpha(ink, 70));
            }
            case SHOE -> {
                Path2D.Double shoe = new Path2D.Double();
                shoe.moveTo(-175, 62);
                shoe.lineTo(-175, -15);
                shoe.quadTo(-165, -68, -112, -74);
                shoe.lineTo(-45, -125);
                shoe.lineTo(5, -125);
                shoe.lineTo(25, -62);
                shoe.quadTo(125, -45, 175, 8);
                shoe.quadTo(192, 40, 180, 62);
                shoe.closePath();
                fill(g, shoe, vertical(-125, light, 62, c));
                fill(g, rr(-185, 52, 375, 34, 15), vertical(52, Color.WHITE, 86, new Color(0xe2e8f0)));
                for (int i = 0; i < 4; i++) {
                    line(g, Color.WHITE, 5, -40 + i * 16, -105 + i * 14, 8 + i * 16, -96 + i * 14);
                }
                fill(g, poly(110, -20, 180, 8, 186, 52, 110, 52), alpha(dark, 120));
            }
            case BAG -> {
                stroke(g, new Arc2D.Double(-85, -140, 170, 210, 0, 180, Arc2D.OPEN), dark, 18);
                fill(g, poly(-155, -35, 155, -35, 178, 150, -178, 150), vertical(-35, light, 150, dark));
                fill(g, poly(-155, -35, 155, -35, 140, 40, -140, 40), alpha(dark, 110));
                fill(g, rr(-22, 22, 44, 34, 6), new Color(0xfbbf24));
                Stroke dashed = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 8}, 0);
                g.setStroke(dashed);
                g.setColor(alpha(Color.WHITE, 120));
                g.draw(poly(-145, -25, 145, -25, 165, 138, -165, 138));
            }
            case GLASSES -> {
                line(g, c, 8, -165, -40, -205, -20);
                line(g, c, 8, 165, -40, 205, -20);
                for (int side : new int[]{-1, 1}) {
                    java.awt.Shape lens = oval(side * 88, 10, 78, 66);
                    fill(g, lens, vertical(-56, new Color(0x14532d), 76, new Color(0x052e16)));
                    stroke(g, lens, c, 8);
                    fill(g, oval(side * 88 - 25, -18, 26, 12), new Color(255, 255, 255, 70));
                }
                stroke(g, new QuadCurve2D.Double(-12, -20, 0, -40, 12, -20), c, 8);
                line(g, c, 6, -40, -45, 40, -45);
            }
            case CHAIR -> {
                Color wood = new Color(0x78350f);
                line(g, wood, 13, -120, 70, -138, 158);
                line(g, wood, 13, 120, 70, 138, 158);
                line(g, wood, 13, -60, 75, -66, 150);
                line(g, wood, 13, 60, 75, 66, 150);
                fill(g, rr(-125, -160, 250, 185, 42), vertical(-160, light, 25, c));
                fill(g, rr(-150, 0, 300, 76, 26), vertical(0, light, 76, dark));
                fill(g, rr(-180, -45, 54, 125, 24), vertical(-45, c, 80, dark));
                fill(g, rr(126, -45, 54, 125, 24), vertical(-45, c, 80, dark));
            }
            case LAMP -> {
                fill(g, oval(0, 0, 150, 70), new RadialGradientPaint(new Point2D.Double(0, -20), 160, new float[]{0f, 1f},
                        new Color[]{new Color(253, 224, 71, 120), new Color(253, 224, 71, 0)}));
                fill(g, rr(-8, -45, 16, 180, 6), vertical(-45, new Color(0x475569), 135, ink));
                fill(g, oval(0, 140, 85, 17), ink);
                fill(g, poly(-90, -160, 90, -160, 135, -42, -135, -42), vertical(-160, light, -42, c));
                fill(g, oval(0, -42, 135, 12), mix(c, new Color(0xfde68a), 0.6));
            }
            case PLANT -> {
                Color leafDark = mix(c, Color.BLACK, 0.25);
                double[][] leaves = {{-80, -90, -0.7}, {75, -100, 0.7}, {-20, -150, -0.15}, {30, -60, 0.4}, {-55, -30, -1.0}, {95, -30, 1.1}};
                for (double[] leaf : leaves) {
                    line(g, new Color(0x3f6212), 6, 0, 30, leaf[0] * 0.6, leaf[1] * 0.6);
                    AffineTransform t = g.getTransform();
                    g.translate(leaf[0], leaf[1]);
                    g.rotate(leaf[2]);
                    fill(g, oval(0, 0, 38, 72), vertical(-72, light, 72, leafDark));
                    line(g, alpha(Color.WHITE, 90), 3, 0, -60, 0, 60);
                    g.setTransform(t);
                }
                fill(g, poly(-82, 45, 82, 45, 62, 165, -62, 165), vertical(45, new Color(0xf8fafc), 165, new Color(0xcbd5e1)));
                fill(g, rr(-92, 30, 184, 26, 8), new Color(0xe2e8f0));
            }
            case BIKE -> {
                for (double x : new double[]{-115, 115}) {
                    stroke(g, circle(x, 65, 78), ink, 12);
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * i / 4;
                        line(g, new Color(0x94a3b8), 2, x, 65, x + Math.cos(a) * 72, 65 + Math.sin(a) * 72);
                    }
                }
                float w = 12;
                line(g, c, w, -115, 65, -20, 65);
                line(g, c, w, -20, 65, 45, -40);
                line(g, c, w, 45, -40, -65, -40);
                line(g, c, w, -65, -40, -115, 65);
                line(g, c, w, -65, -40, -20, 65);
                line(g, c, w, 45, -40, 115, 65);
                line(g, ink, 9, 45, -40, 58, -85);
                line(g, ink, 9, 40, -88, 85, -88);
                line(g, ink, 7, -65, -40, -72, -62);
                fill(g, rr(-102, -72, 62, 14, 7), ink);
            }
            case BALL -> {
                fill(g, circle(0, 0, 145), diagonal(-145, -145, Color.WHITE, 145, 145, mix(c, Color.WHITE, 0.55)));
                java.awt.Shape oldClip = g.getClip();
                g.clip(circle(0, 0, 145));
                stroke(g, new Arc2D.Double(-260, -150, 300, 300, -60, 120, Arc2D.OPEN), c, 9);
                stroke(g, new Arc2D.Double(-40, -150, 300, 300, 120, 120, Arc2D.OPEN), c, 9);
                line(g, c, 9, -150, 0, 150, 0);
                g.setClip(oldClip);
                fill(g, poly(0, -38, 36, -12, 22, 30, -22, 30, -36, -12), c);
                fill(g, oval(-55, -70, 36, 20), new Color(255, 255, 255, 140));
            }
            case DUMBBELL -> {
                fill(g, rr(-165, -12, 330, 24, 10), vertical(-12, Color.WHITE, 12, new Color(0x64748b)));
                for (int side : new int[]{-1, 1}) {
                    fill(g, rr(side > 0 ? 95 : -135, -85, 40, 170, 12), vertical(-85, light, 85, dark));
                    fill(g, rr(side > 0 ? 138 : -178, -62, 40, 124, 12), vertical(-62, light, 62, dark));
                }
                for (int x = -60; x <= 60; x += 12) {
                    line(g, alpha(ink, 80), 2, x, -10, x, 10);
                }
            }
            case RACKET -> {
                java.awt.Shape head = oval(0, -60, 108, 140);
                java.awt.Shape oldClip = g.getClip();
                g.clip(head);
                for (int x = -110; x <= 110; x += 16) {
                    line(g, alpha(Color.WHITE, 200), 2, x, -200, x, 80);
                }
                for (int y = -200; y <= 80; y += 16) {
                    line(g, alpha(Color.WHITE, 200), 2, -110, y, 110, y);
                }
                g.setClip(oldClip);
                stroke(g, head, c, 18);
                line(g, c, 14, -30, 72, 0, 110);
                line(g, c, 14, 30, 72, 0, 110);
                fill(g, rr(-15, 100, 30, 110, 8), vertical(100, dark, 210, ink));
            }
            case SCOOTER -> {
                wheel(g, -145, 110, 42, ink);
                wheel(g, 125, 110, 42, ink);
                fill(g, rr(-150, 72, 250, 24, 10), vertical(72, light, 96, c));
                line(g, c, 16, 112, 88, 70, -135);
                line(g, ink, 12, 40, -138, 105, -138);
                fill(g, oval(-60, 40, 70, 40), vertical(0, light, 80, c));
                fill(g, rr(-95, 10, 70, 16, 8), ink);
                fill(g, circle(80, -95, 12), new Color(0xfde68a));
            }
            case GUITAR -> {
                fill(g, rr(-15, -215, 30, 190, 4), vertical(-215, new Color(0x78350f), -25, new Color(0x451a03)));
                fill(g, rr(-24, -258, 48, 52, 10), new Color(0x451a03));
                Area body = new Area(oval(0, 85, 112, 98));
                body.add(new Area(oval(0, -20, 82, 74)));
                fill(g, body, new GradientPaint(-112, 0, light, 112, 0, dark));
                fill(g, circle(0, 15, 30), ink);
                fill(g, rr(-42, 100, 84, 12, 4), ink);
                for (int i = -3; i <= 2; i++) {
                    line(g, alpha(Color.WHITE, 170), 1.5f, i * 4 + 2, -250, i * 6 + 3, 104);
                }
            }
            case VINYL -> {
                fill(g, new Rectangle2D.Double(-215, -150, 300, 300), vertical(-150, light, 150, c));
                fill(g, new Rectangle2D.Double(-215, -150, 300, 300), alpha(Color.WHITE, 20));
                fill(g, circle(45, 0, 152), new Color(0x0b0b0f));
                for (int r = 60; r < 148; r += 8) {
                    stroke(g, circle(45, 0, r), new Color(255, 255, 255, 18), 1);
                }
                fill(g, circle(45, 0, 52), vertical(-52, mix(c, Color.WHITE, 0.3), 52, dark));
                fill(g, circle(45, 0, 6), Color.WHITE);
                fill(g, oval(0, -70, 60, 18), new Color(255, 255, 255, 26));
            }
            case PIANO -> {
                fill(g, rr(-210, -70, 420, 140, 14), vertical(-70, mix(c, Color.WHITE, 0.15), 70, ink));
                fill(g, rr(-198, -58, 396, 32, 6), new Color(0x1e293b));
                for (int i = 0; i < 6; i++) {
                    fill(g, circle(-170 + i * 22, -42, 4), i == 2 ? new Color(0x22c55e) : new Color(0x64748b));
                }
                double keyW = 396 / 22.0;
                for (int i = 0; i < 22; i++) {
                    fill(g, rr(-198 + i * keyW + 1, -18, keyW - 2, 78, 2), new Color(0xf8fafc));
                }
                int[] pattern = {1, 1, 0, 1, 1, 1, 0};
                for (int i = 0; i < 21; i++) {
                    if (pattern[i % 7] == 1) {
                        fill(g, rr(-198 + (i + 1) * keyW - keyW * 0.3, -18, keyW * 0.6, 46, 2), ink);
                    }
                }
            }
            case CAR -> {
                Path2D.Double car = new Path2D.Double();
                car.moveTo(-195, 42);
                car.lineTo(-195, 2);
                car.quadTo(-190, -20, -150, -26);
                car.lineTo(-92, -32);
                car.quadTo(-60, -88, -10, -92);
                car.lineTo(70, -92);
                car.quadTo(112, -88, 142, -36);
                car.lineTo(178, -26);
                car.quadTo(200, -16, 200, 16);
                car.lineTo(200, 42);
                car.closePath();
                fill(g, car, vertical(-92, light, 42, dark));
                fill(g, poly(-76, -34, -22, -80, 62, -80, 112, -36), vertical(-80, new Color(0xbae6fd), -34, new Color(0x334155)));
                line(g, alpha(dark, 200), 8, 18, -82, 18, -34);
                line(g, alpha(ink, 70), 2, 18, -30, 22, 38);
                fill(g, oval(182, -6, 12, 8), new Color(0xfde68a));
                fill(g, oval(-186, -4, 8, 8), new Color(0xef4444));
                wheel(g, -112, 46, 40, ink);
                wheel(g, 118, 46, 40, ink);
            }
            case MOTO -> {
                wheel(g, -120, 72, 58, ink);
                wheel(g, 120, 72, 58, ink);
                line(g, new Color(0x94a3b8), 10, 120, 72, 82, -52);
                line(g, ink, 10, 60, -64, 112, -74);
                line(g, dark, 12, -120, 72, -20, 40);
                fill(g, rr(-45, 8, 90, 60, 12), vertical(8, new Color(0x94a3b8), 68, new Color(0x334155)));
                fill(g, oval(-5, -22, 82, 40), vertical(-62, light, 18, c));
                fill(g, rr(-125, -38, 105, 24, 11), ink);
                fill(g, circle(95, -45, 14), new Color(0xfde68a));
            }
            case BOX -> {
                fill(g, poly(-135, -45, 70, -45, 145, -105, -60, -105), vertical(-105, mix(c, Color.WHITE, 0.55), -45, light));
                fill(g, poly(70, -45, 145, -105, 145, 85, 70, 145), vertical(-105, c, 145, dark));
                fill(g, new Rectangle2D.Double(-135, -45, 205, 190), vertical(-45, light, 145, c));
                fill(g, new Rectangle2D.Double(-50, -45, 34, 190), alpha(Color.WHITE, 60));
                fill(g, rr(-115, 60, 100, 60, 6), new Color(255, 255, 255, 210));
                line(g, alpha(ink, 120), 4, -100, 78, -35, 78);
                line(g, alpha(ink, 90), 4, -100, 96, -55, 96);
            }
            case TEDDY -> {
                fill(g, oval(0, 82, 108, 96), vertical(-14, light, 178, c));
                fill(g, oval(-120, 50, 36, 58), c);
                fill(g, oval(120, 50, 36, 58), c);
                fill(g, oval(-60, 165, 42, 30), dark);
                fill(g, oval(60, 165, 42, 30), dark);
                for (int side : new int[]{-1, 1}) {
                    fill(g, circle(side * 72, -140, 34), c);
                    fill(g, circle(side * 72, -140, 18), light);
                }
                fill(g, circle(0, -72, 88), vertical(-160, light, 16, c));
                fill(g, oval(0, -45, 42, 32), mix(c, Color.WHITE, 0.6));
                fill(g, oval(0, -58, 14, 10), ink);
                fill(g, circle(-32, -92, 9), ink);
                fill(g, circle(32, -92, 9), ink);
                fill(g, oval(0, 90, 46, 52), mix(c, Color.WHITE, 0.4));
            }
            case TELESCOPE -> {
                line(g, ink, 10, 0, 30, -115, 165);
                line(g, ink, 10, 0, 30, 0, 175);
                line(g, ink, 10, 0, 30, 115, 165);
                fill(g, rr(-26, 0, 52, 44, 8), new Color(0x334155));
                AffineTransform t = g.getTransform();
                g.translate(0, -10);
                g.rotate(-0.4);
                fill(g, rr(-170, -42, 320, 84, 20), vertical(-42, light, 42, dark));
                fill(g, rr(150, -50, 34, 100, 10), ink);
                fill(g, rr(-190, -16, 30, 32, 6), ink);
                fill(g, rr(-60, -60, 90, 18, 6), new Color(0x94a3b8));
                g.setTransform(t);
            }
            case SNOWBOARD -> {
                AffineTransform t = g.getTransform();
                g.rotate(-0.3);
                fill(g, rr(-230, -42, 460, 84, 42), new GradientPaint(-230, 0, light, 230, 0, dark));
                fill(g, rr(-200, -30, 400, 60, 30), alpha(Color.WHITE, 30));
                fill(g, new Rectangle2D.Double(-70, -18, 140, 6), new Color(0xfbbf24));
                for (int x : new int[]{-110, 70}) {
                    fill(g, rr(x, -36, 40, 72, 10), ink);
                    line(g, new Color(0x64748b), 6, x + 6, -30, x + 34, -30);
                    line(g, new Color(0x64748b), 6, x + 6, 28, x + 34, 28);
                }
                g.setTransform(t);
            }
            case TENT -> {
                fill(g, poly(-195, 145, 0, -145, 195, 145), vertical(-145, light, 145, dark));
                fill(g, poly(0, -145, 195, 145, 120, 145), alpha(ink, 50));
                fill(g, poly(-58, 145, 0, 12, 58, 145), vertical(12, mix(c, Color.BLACK, 0.55), 145, ink));
                line(g, new Color(0x94a3b8), 3, 0, -145, -230, 150);
                line(g, new Color(0x94a3b8), 3, 0, -145, 230, 150);
                line(g, new Color(0x78350f), 6, -240, 150, 240, 150);
            }
        }
    }

    private static void drawCentered(Graphics2D g, String text, double cx, double baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (float) (cx - metrics.stringWidth(text) / 2.0), (float) baseline);
    }
}

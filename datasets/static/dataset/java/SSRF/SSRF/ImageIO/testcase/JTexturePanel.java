<filename>strange-eons-main/src/main/java/ca/cgjennings/ui/JTexturePanel.java<fim_prefix>

        package ca.cgjennings.ui;

import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * A panel with a textured background.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JTexturePanel extends JPanel {

    public static final int LEFT_EDGE = 1;
    public static final int TOP_EDGE = 1;
    public static final int RIGHT_EDGE = -1;
    public static final int BOTTOM_EDGE = -1;

    private int verticalEdge = TOP_EDGE;
    private int horizontalEdge = LEFT_EDGE;
    private int verticalRepeats = Integer.MAX_VALUE;
    private int horizontalRepeats = Integer.MAX_VALUE;
    private BufferedImage texture;

    public JTexturePanel() {
    }

    public JTexturePanel(BufferedImage texture) {
        this();
        this.texture = texture;
    }

    public JTexturePanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public JTexturePanel(LayoutManager layout) {
        super(layout);
    }

    public JTexturePanel(LayoutManager layout, BufferedImage texture) {
        this(layout);
        this.texture = texture;
    }

    public JTexturePanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    private void edgeCheck(int edge) {
        if (edge != -1 && edge != 1) {
            throw new IllegalArgumentException("invalid edge value: " + edge);
        }
    }

    private void repeatsCheck(int reps) {
        if (reps < 0) {
            throw new IllegalArgumentException("repeats: " + reps);
        }
    }

    public BufferedImage getTexture() {
        return texture;
    }

    public void setTexture(BufferedImage texture) {
        if (this.texture != texture) {
            this.texture = texture;
            repaint();
        }
    }

    public void setTexture(URL url) throws IOException {
        <fim_suffix>
        setTexture(ImageIO.read(url));
    }

    public int getHorizontalEdge() {
        return horizontalEdge;
    }

    public void setHorizontalEdge(int horizontalEdge) {
        edgeCheck(horizontalEdge);
        if (this.horizontalEdge != horizontalEdge) {
            this.horizontalEdge = horizontalEdge;
            repaint();
        }
    }

    public int getHorizontalRepeats() {
        return horizontalRepeats;
    }

    public void setHorizontalRepeats(int horizontalRepeats) {
        repeatsCheck(horizontalRepeats);
        if (this.horizontalRepeats != horizontalRepeats) {
            this.horizontalRepeats = horizontalRepeats;
            repaint();
        }
    }

    public int getVerticalEdge() {
        return verticalEdge;
    }

    public void setVerticalEdge(int verticalEdge) {
        edgeCheck(verticalEdge);
        if (this.verticalEdge != verticalEdge) {
            this.verticalEdge = verticalEdge;
            repaint();
        }
    }

    public int getVerticalRepeats() {
        return verticalRepeats;
    }

    public void setVerticalRepeats(int verticalRepeats) {
        repeatsCheck(verticalRepeats);
        if (this.verticalRepeats != verticalRepeats) {
            this.verticalRepeats = verticalRepeats;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isOpaque()) {
            return;
        }
        if (texture == null) {
            super.paintComponent(g);
            return;
        }

        int tw = texture.getWidth(), th = texture.getHeight(), w = getWidth(), h = getHeight();

        if (texture.getTransparency() != BufferedImage.OPAQUE || tw * horizontalRepeats < w || th * verticalRepeats < h) {
            super.paintComponent(g);
        }

        int yRep = 0, xRep = 0;
        if (verticalEdge == BOTTOM_EDGE) {
            for (int y = h - th; y >= -th && yRep < verticalRepeats; y -= th, ++yRep) {
                if (horizontalEdge == RIGHT_EDGE) {
                    for (int x = w - tw; x >= -tw && xRep < horizontalRepeats; x -= tw, ++xRep) {
                        g.drawImage(texture, x, y, null);
                    }
                } else {
                    for (int x = 0; x < w && xRep < horizontalRepeats; x += tw, ++xRep) {
                        g.drawImage(texture, x, y, null);
                    }
                }
            }
        } else {
            for (int y = 0; y < h && yRep < verticalRepeats; y += th, ++yRep) {
                if (horizontalEdge == RIGHT_EDGE) {
                    for (int x = w - tw; x >= -tw && xRep < horizontalRepeats; x -= tw, ++xRep) {
                        g.drawImage(texture, x, y, null);
                    }
                } else {
                    for (int x = 0; x < w && xRep < horizontalRepeats; x += tw, ++xRep) {
                        g.drawImage(texture, x, y, null);
                    }
                }
            }
        }
    }
}
<fim_middle>
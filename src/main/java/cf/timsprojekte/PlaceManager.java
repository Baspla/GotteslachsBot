package cf.timsprojekte;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;

class PlaceManager {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;
    private static final File file = new File("place.bmp");
    private static final int MULT = 5;
    private BufferedImage img;

    PlaceManager() {
        img = null;
        try {
            img = ImageIO.read(file);
        } catch (IOException e) {
            img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            savePlace();
        }
    }

    void setPixel(int x, int y, Color color) {
        img.setRGB(x, y, color.getRGB());
    }

    int getWidth() {
        return img.getWidth();
    }

    int getHeight() {
        return img.getHeight();
    }

    void savePlace() {
        try {
            ImageIO.write(img, "BMP", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ByteArrayInputStream getImageStream() {
        BufferedImage scaled = new BufferedImage(WIDTH * MULT, HEIGHT * MULT, BufferedImage.TYPE_INT_RGB);
        Graphics gra = scaled.getGraphics();
        gra.drawImage(img, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(scaled, "bmp", os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(os.toByteArray());
    }
}

import java.awt.*;
import java.awt.image.BufferedImage;

public class Node {
  private Rectangle area;
  private Color avgColor;
  private Node[] children;

  public Node(Rectangle area, Color avgColor) {
    this.area = area;
    this.avgColor = avgColor;
    this.children = null;
  }

  public Rectangle getArea() {
    return area;
  }

  public Color getColor() {
    return avgColor;
  }

  public Node[] getChildren() {
    return children;
  }

  public boolean isLeaf() {
    return children == null;
  }

  public void subDivide(BufferedImage image, Quadtree quadtree) {
    int halfWidth = area.width / 2;
    int halfHeight = area.height / 2;
    int remainingWidth = area.width - halfWidth;
    int remainingHeight = area.height - halfHeight;

    if (halfWidth < quadtree.minBlockSize || halfHeight < quadtree.minBlockSize) {
      return;
    }

    children = new Node[4];

    Rectangle[] subAreas = {
        new Rectangle(area.x, area.y, halfWidth, halfHeight),
        new Rectangle(area.x + halfWidth, area.y, remainingWidth, halfHeight),
        new Rectangle(area.x, area.y + halfHeight, halfWidth, remainingHeight),
        new Rectangle(area.x + halfWidth, area.y + halfHeight, remainingWidth, remainingHeight)
    };

    for (int i = 0; i < 4; i++) {
      children[i] = quadtree.buildTree(image, subAreas[i]);
    }
  }

  public static Color avgColor(BufferedImage image, Rectangle area) {
    int red = 0;
    int green = 0;
    int blue = 0;
    int totalPixel = area.width * area.height;

    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x >= image.getWidth() || y >= image.getHeight()) {
          continue;
        }
        int rgb = image.getRGB(x, y);
        Color color = new Color(rgb);

        red += color.getRed();
        green += color.getGreen();
        blue += color.getBlue();
      }
    }

    if (totalPixel == 0) {
      return new Color(0, 0, 0);
    }

    int avgRed = red / totalPixel;
    int avgGreen = green / totalPixel;
    int avgBlue = blue / totalPixel;

    return new Color(avgRed, avgGreen, avgBlue);
  }
}

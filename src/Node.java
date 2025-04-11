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
    int validPixelCount = 0;

    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
          continue;
        }
        Color color = new Color(image.getRGB(x, y));
        red += color.getRed();
        green += color.getGreen();
        blue += color.getBlue();
        validPixelCount++;
      }
    }

    if (validPixelCount == 0) {
      return new Color(0, 0, 0);
    }

    int avgRed = red / validPixelCount;
    int avgGreen = green / validPixelCount;
    int avgBlue = blue / validPixelCount;

    // Clamp nilai ke rentang 0-255 untuk jaga-jaga
    avgRed = Math.min(255, Math.max(0, avgRed));
    avgGreen = Math.min(255, Math.max(0, avgGreen));
    avgBlue = Math.min(255, Math.max(0, avgBlue));

    return new Color(avgRed, avgGreen, avgBlue);
  }
}

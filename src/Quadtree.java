import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;

public class Quadtree {
  private double threshold;
  public int minBlockSize;
  private ErrorMeasurement.Metode metode;

  public Quadtree(double threshold, int minBlockSize, ErrorMeasurement.Metode metode) {
    this.threshold = threshold;
    this.minBlockSize = minBlockSize;
    this.metode = metode;
  }

  public int getDepth(Node node) {
    if (node == null || node.isLeaf()) {
      return 1;
    }
    int maxDepth = 0;
    for (Node child : node.getChildren()) {
      maxDepth = Math.max(maxDepth, getDepth(child));
    }
    return maxDepth + 1;
  }

  public int getTotalNodes(Node node) {
    if (node == null) {
      return 0;
    }
    int count = 1;
    if (!node.isLeaf()) {
      for (Node child : node.getChildren()) {
        count += getTotalNodes(child);
      }
    }
    return count;
  }

  public boolean divide(Node node, BufferedImage image) {
    if (node.getArea().width <= minBlockSize || node.getArea().height <= minBlockSize) {
      return false;
    }

    double err = hitungErr(image, node.getArea());

    return err > threshold;
  }

  private double hitungErr(BufferedImage image, Rectangle area) {
    ErrorMeasurement em = new ErrorMeasurement();

    switch (metode) {
      case VARIANCE:
        return em.variance(image, area);
      case MAD:
        return em.mad(image, area);
      case MPD:
        return em.maxPixelDifference(image, area);
      case ENTROPY:
        return em.entropy(image, area);
      case SSIM:
        double error = 1 - em.ssim(image,area);
        //System.out.println("Error = " + error);
        return error;
      default:
        throw new IllegalArgumentException("Metode tidak ada!");
    }
  }

  public Node buildTree(BufferedImage image, Rectangle area) {
    Color avgColor = Node.avgColor(image, area);
    Node node = new Node(area, avgColor);

    if (divide(node, image)) {
      node.subDivide(image, this);
    }

    return node;
  }

  public BufferedImage reconstructImage(Node root, int width, int height) {
    BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = newImage.createGraphics();

    fillImage(graphics, root);

    graphics.dispose();
    return newImage;
  }

  private void fillImage(Graphics2D graphics, Node node) {
    if (node == null) {
      return;
    }

    if (node.isLeaf()) {
      graphics.setColor(node.getColor());
      graphics.fillRect(node.getArea().x, node.getArea().y, node.getArea().width, node.getArea().height);
    } else {
      for (Node child : node.getChildren()) {
        fillImage(graphics, child);
      }
    }
  }

  public BufferedImage reconstructImageAtLevel(Node root, int targetLevel, int width, int height) {
    BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = newImage.createGraphics();
    fillImageAtLevel(g, root, 1, targetLevel);
    g.dispose();
    return newImage;
  }

  private void fillImageAtLevel(Graphics2D g, Node node, int currentLevel, int targetLevel) {
    if (node == null) return;

    // Jika node adalah leaf atau sudah mencapai target level, gambar blok dengan warna rata-rata node.
    if (node.isLeaf() || currentLevel == targetLevel) {
        g.setColor(node.getColor());
        Rectangle r = node.getArea();
        g.fillRect(r.x, r.y, r.width, r.height);
    } else { // Jika belum mencapai target level, lanjutkan ke anak-anaknya.
        for (Node child : node.getChildren()) {
            fillImageAtLevel(g, child, currentLevel + 1, targetLevel);
        }
    }
  }
}

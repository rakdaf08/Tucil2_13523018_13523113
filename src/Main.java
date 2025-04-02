import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Scanner;
import java.awt.Rectangle;

public class Main {
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    // Input PATH gambar (ditentukan untuk mempermudah debugging)
    String inputPath = "c:/Users/rakad/OneDrive/Dokumen/ITB/Semester 4/Stima/Tucil2_13523018_13523113/test/input/porsche718.jpg";
    BufferedImage image = loadImage(inputPath);
    if (image == null) {
      scanner.close();
      return;
    }

    // Input metode
    ErrorMeasurement.Metode metode = chooseErrorMethod(scanner);
    if (metode == null) {
      scanner.close();
      return;
    }

    // Input threshold dan minblock
    double threshold = getThreshold(scanner, metode);
    int minBlockSize = getMinBlockSize(scanner);

    // PATH output
    String outputFolder = "c:/Users/rakad/OneDrive/Dokumen/ITB/Semester 4/Stima/Tucil2_13523018_13523113/test/output";
    File outputFile = getOutputFile(inputPath, outputFolder);
    if (outputFile == null || !prepareOutputFolder(outputFolder)) {
      scanner.close();
      return;
    }

    // Proses kompresi
    long startTime = System.nanoTime();
    Quadtree quadtree = new Quadtree(threshold, minBlockSize, metode);
    Node root = quadtree.buildTree(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
    BufferedImage compressedImage = quadtree.reconstructImage(root, image.getWidth(), image.getHeight());
    long endTime = System.nanoTime();

    // Menyimpan gambar
    saveImage(compressedImage, outputFile);

    // Output ststistik
    printStatistics(inputPath, outputFile, quadtree, root, startTime, endTime);

    scanner.close();
  }

  // Fungsi untuk membaca gambar
  private static BufferedImage loadImage(String inputPath) {
    BufferedImage image = null;
    try {
      System.out.println("Path yang dibaca: " + inputPath);
      image = ImageIO.read(new File(inputPath));
      if (image == null) {
        System.out.println("Error: Format gambar tidak didukung atau file rusak.");
        return null;
      }
    } catch (IOException e) {
      System.out.println("Gagal membaca gambar! Periksa kembali path file.");
      return null;
    }
    return image;
  }

  // Fungsi untuk memilih metode
  private static ErrorMeasurement.Metode chooseErrorMethod(Scanner scanner) {
    System.out
        .println("1. Variance (100 - 1000)\n2. MAD (5 - 50)\n3. MaxPixelDifference (10 - 200)\n4. Entropy(0.5 - 5)");
    System.out.print("Pilih metode perhitungan error: ");

    int metodePilihan = scanner.nextInt();
    if (metodePilihan < 1 || metodePilihan > 4) {
      System.out.println("Pilihan metode tidak valid!");
      return null;
    }
    return ErrorMeasurement.Metode.values()[metodePilihan - 1];
  }

  // Fungsi untuk mendapatkan threshold
  private static double getThreshold(Scanner scanner, ErrorMeasurement.Metode metode) {
    double threshold;
    while (true) {
      System.out.print("Masukkan threshold: ");
      String input = scanner.next().replace(",", ".");

      try {
        threshold = Double.parseDouble(input);
      } catch (NumberFormatException e) {
        System.out.println("Input tidak valid. Masukkan angka desimal dengan titik atau koma.");
        continue;
      }

      if (metode == ErrorMeasurement.Metode.VARIANCE && (threshold < 100 || threshold > 1000)) {
        System.out.println("Threshold tidak sesuai range untuk metode Variansi.");
      } else if (metode == ErrorMeasurement.Metode.MAD && (threshold < 5 || threshold > 50)) {
        System.out.println("Threshold tidak sesuai range untuk metode MAD.");
      } else if (metode == ErrorMeasurement.Metode.MPD && (threshold < 10 || threshold > 200)) {
        System.out.println("Threshold tidak sesuai range untuk metode MPD.");
      } else if (metode == ErrorMeasurement.Metode.ENTROPY && (threshold < 0.5 || threshold > 5)) {
        System.out.println("Threshold tidak sesuai range untuk metode Entropi.");
      } else {
        break;
      }
    }
    return threshold;
  }

  // Fungsi untuk mendapatkan ukuran blok minimum
  private static int getMinBlockSize(Scanner scanner) {
    System.out.print("Masukkan ukuran blok minimum: ");
    return scanner.nextInt();
  }

  // Fungsi untuk mendapatkan file output dengan nama sesuai format
  private static File getOutputFile(String inputPath, String outputFolder) {
    File inputFile = new File(inputPath);
    String fileName = inputFile.getName();

    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex == -1) {
      System.out.println("Error: File tidak memiliki ekstensi.");
      return null;
    }

    String realName = fileName.substring(0, dotIndex);
    String extension = fileName.substring(dotIndex + 1);
    String outputFileName = realName + "-compressed." + extension;
    return new File(outputFolder, outputFileName);
  }

  // Fungsi untuk memastikan folder output tersedia
  private static boolean prepareOutputFolder(String outputFolder) {
    File outputDir = new File(outputFolder);
    if (!outputDir.exists() && !outputDir.mkdirs()) {
      System.out.println("Gagal membuat direktori output.");
      return false;
    }
    return true;
  }

  // Fungsi untuk mendapatkan ekstensi file
  private static String getFileExtension(File file) {
    String name = file.getName();
    int dotIndex = name.lastIndexOf('.');
    return (dotIndex == -1) ? "png" : name.substring(dotIndex + 1);
  }

  // Fungsi untuk menyimpan gambar
  private static void saveImage(BufferedImage image, File outputFile) {
    try {
      String extension = getFileExtension(outputFile);
      ImageIO.write(image, extension, outputFile);
      System.out.println("Gambar berhasil disimpan di: " + outputFile.getAbsolutePath());
    } catch (IOException e) {
      System.out.println("Gagal menyimpan gambar! Periksa kembali path file.");
      e.printStackTrace();
    }
  }

  // Fungsi untuk mencetak statistik hasil kompresi
  private static void printStatistics(String inputPath, File outputFile, Quadtree quadtree, Node root, long startTime,
      long endTime) {
    double executionTime = (endTime - startTime) / 1e9;
    long originalSize = new File(inputPath).length();
    long compressedSize = outputFile.length();
    double compressionRatio = ((double) (originalSize - compressedSize) / originalSize) * 100;
    int depth = quadtree.getDepth(root);
    int nodeCount = quadtree.getTotalNodes(root);

    System.out.println("Waktu eksekusi: " + executionTime + " detik");
    System.out.println("Ukuran gambar sebelum: " + originalSize + " bytes");
    System.out.println("Ukuran gambar setelah: " + compressedSize + " bytes");
    System.out.println("Persentase kompresi: " + compressionRatio + "%");
    System.out.println("Kedalaman pohon: " + depth);
    System.out.println("Banyak simpul pada pohon: " + nodeCount);
  }
}

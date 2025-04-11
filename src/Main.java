import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Scanner;
import java.awt.Rectangle;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    System.out.println(" ___ __  __  ____    ____                                                   \r\n" + //
        " |_ _|  \\/  |/ ___|  / ___|___  _ __ ___  _ __  _ __ ___  ___ ___  ___  _ __ \r\n" + //
        "  | || |\\/| | |  _  | |   / _ \\| '_ ` _ \\| '_ \\| '__/ _ \\/ __/ __|/ _ \\| '__|\r\n" + //
        "  | || |  | | |_| | | |__| (_) | | | | | | |_) | | |  __/\\__ \\__ \\ (_) | |   \r\n" + //
        " |___|_|  |_|\\____|  \\____\\___/|_| |_| |_| .__/|_|  \\___||___/___/\\___/|_|   \r\n" + //
        "                                         |_|     \n");

    // Input PATH gambar
    String inputPath = getInputPath(scanner);
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

    // Input BONUS 1 - Target kompresi
    double targetKompresi = getValidatedTarget(scanner);
    long originalSize = new File(inputPath).length();
    String format = getFileExtension(new File(inputPath));
    if (targetKompresi != 0) { // jika 0, maka dinonaktifkan
      double[] range = getCompressionRange(image, originalSize, metode, format);
      double minTarget = range[0], maxTarget = range[1];
      System.out.printf("Rentang target kompresi untuk metode %s adalah: %.2f%% - %.2f%%\n", metode, minTarget * 100,
          maxTarget * 100); // lihat line 397
      threshold = autoAdjustThreshold(image, originalSize, metode, format, targetKompresi);
      minBlockSize = 1;
      if (targetKompresi < minTarget || targetKompresi > maxTarget) {
        System.out.println(
            "Input target kompresi tidak berada dalam rentang yang bisa dilakukan oleh program ini, sistem akan tetap mencoba.");
      } else {
        System.out
            .println("Target kompresi diaktifkan! Berusaha menargetkan kompresi ke: " + (targetKompresi * 100) + "%");
      }
    } else {
      System.out.println("Mode target kompresi dinonaktifkan.");
    }

    // PATH output
    String outputFilePath = getOutputFilePath(scanner);
    File outputFile = new File(outputFilePath);
    if (!prepareOutputFolder(outputFile.getParent())) {
      System.out.println("Folder output tidak tersedia atau tidak dapat dibuat.");
      scanner.close();
      return;
    }

    // Proses kompresi
    long startTime = System.nanoTime();
    Quadtree quadtree = new Quadtree(threshold, minBlockSize, metode);
    Node root = quadtree.buildTree(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
    BufferedImage compressedImage = quadtree.reconstructImage(root, image.getWidth(), image.getHeight());
    long endTime = System.nanoTime();

    // Menyimpan gambar hasil kompresi
    saveImage(compressedImage, outputFile);

    /*
     * Membuat GIF, bisa skip karena heap (jika file terlalu besar, Java tidak
     * cukup, harus disetting terlebih dahulu)
     */
    boolean skipGIF = false;
    if (image.getWidth() * image.getHeight() > 1920 * 1080) {
      System.out.println(
          "\nMaaf! Ukuran gambar terlalu besar untuk membuat GIF proses kompresi karena memori Java tidak cukup. Kompresi akan tetap dijalankan tanpa pembuatan GIF.");
      skipGIF = true;
    }

    if (!skipGIF) {
      int maxDepth = quadtree.getDepth(root);
      List<BufferedImage> evolutionFrames = new ArrayList<>();
      for (int level = 1; level <= maxDepth; level++) {
        BufferedImage frame = quadtree.reconstructImageAtLevel(root, level, image.getWidth(), image.getHeight());
        evolutionFrames.add(frame);
      }

      // Dapatkan lokasi file output hasil GIF secara absolut
      String outputGifPath = getOutputGifPath(scanner);
      try {
        ImageOutputStream evoOutput = new FileImageOutputStream(new File(outputGifPath));
        GifWriter evoGifWriter = new GifWriter(evoOutput, BufferedImage.TYPE_INT_RGB, 50, true);
        for (BufferedImage frame : evolutionFrames) {
          evoGifWriter.writeToSequence(frame, 50);
        }
        evoGifWriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      System.out.println("GIF berhasil disimpan pada path yang dituju.");
    }

    // Output stastistik
    printStatistics(inputPath, outputFile, quadtree, root, startTime, endTime);

    scanner.close();
  }

  // Fungsi untuk membaca gambar
  private static BufferedImage loadImage(String inputPath) {
    BufferedImage image = null;
    try {
      System.out.println("File berhasil dibaca.");
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
    ErrorMeasurement.Metode metode = null;
    while (metode == null) {
      System.out.println("\nList Metode Perhitungan Error");
      System.out.println("1. Variance (0 - 20000)");
      System.out.println("2. MAD (0 - 255)");
      System.out.println("3. MaxPixelDifference (0 - 255)");
      System.out.println("4. Entropy (0.5 - 8)");
      System.out.println("5. SSIM (0 - 1)");
      System.out.print("Pilih metode perhitungan error: ");
      if (scanner.hasNextInt()) {
        int metodePilihan = scanner.nextInt();
        scanner.nextLine();
        if (metodePilihan >= 1 && metodePilihan <= 5) {
          metode = ErrorMeasurement.Metode.values()[metodePilihan - 1];
        } else {
          System.out.println("Pilihan metode tidak valid! Silakan coba lagi antara 1-5.");
        }
      } else {
        System.out.println("Input tidak valid. Silakan masukkan angka bulat antara 1-5.");
        scanner.nextLine();
      }
    }
    return metode;
  }

  // Fungsi untuk mendapatkan threshold
  private static double getThreshold(Scanner scanner, ErrorMeasurement.Metode metode) {
    double threshold;
    while (true) {
      System.out.print("Masukkan threshold: ");
      String input = scanner.next().replace(",", ".");
      scanner.nextLine();

      try {
        threshold = Double.parseDouble(input);
      } catch (NumberFormatException e) {
        System.out.println("Input tidak valid. Masukkan angka desimal dengan titik atau koma.");
        continue;
      }

      if (metode == ErrorMeasurement.Metode.VARIANCE && (threshold < 0 || threshold > 20000)) {
        System.out.println("Threshold tidak sesuai range untuk metode Variansi.");
      } else if (metode == ErrorMeasurement.Metode.MAD && (threshold < 0 || threshold > 255)) {
        System.out.println("Threshold tidak sesuai range untuk metode MAD.");
      } else if (metode == ErrorMeasurement.Metode.MPD && (threshold < 0 || threshold > 255)) {
        System.out.println("Threshold tidak sesuai range untuk metode MPD.");
      } else if (metode == ErrorMeasurement.Metode.ENTROPY && (threshold < 0.5 || threshold > 8)) {
        System.out.println("Threshold tidak sesuai range untuk metode Entropi.");
      } else if (metode == ErrorMeasurement.Metode.SSIM && (threshold < 0 || threshold > 1)) {
        System.out.println("Threshold tidak sesuai range untuk metode SSIM.");
      } else {
        break;
      }
    }
    return threshold;
  }

  // Fungsi untuk mendapatkan ukuran blok minimum
  private static int getMinBlockSize(Scanner scanner) {
    int minBlockSize = 0;
    do {
      System.out.print("Masukkan ukuran blok minimum (> 0): ");
      while (!scanner.hasNextInt()) {
        System.out.println("Input tidak valid. Silakan masukkan angka bulat.");
        scanner.nextLine();
        System.out.print("Masukkan ukuran blok minimum (> 0): ");
      }
      minBlockSize = scanner.nextInt();
      scanner.nextLine(); // membersihkan newline
      if (minBlockSize <= 0) {
        System.out.println("Ukuran blok minimum harus lebih besar dari 0.");
      }
    } while (minBlockSize <= 0);
    return minBlockSize;
  }

  // Fungsi untuk memastikan folder output tersedia
  private static boolean prepareOutputFolder(String outputFolder) {
    File outputDir = new File(outputFolder);
    if (!outputDir.exists() && !outputDir.mkdirs()) {
      System.out.println("Maaf! Gagal membuat direktori output.");
      return false;
    }
    return true;
  }

  // Fungsi untuk mendapatkan ekstensi file
  private static String getFileExtension(File file) {
    String name = file.getName();
    int dotIndex = name.lastIndexOf('.');
    return (dotIndex == -1) ? "" : name.substring(dotIndex + 1).toLowerCase();
  }

  // Fungsi untuk menyimpan gambar
  private static void saveImage(BufferedImage image, File outputFile) {
    try {
      String extension = getFileExtension(outputFile);
      ImageIO.write(image, extension, outputFile);
      System.out.println("Gambar berhasil disimpan pada path yang dituju.");
    } catch (IOException e) {
      System.out.println("Maaf! Gagal menyimpan gambar! Periksa kembali path file.");
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

    System.out.println("\nStatistik Kompresi Gambar");
    System.out.println("Waktu eksekusi: " + executionTime + " detik");
    System.out.println("Ukuran gambar sebelum: " + originalSize + " bytes");
    System.out.println("Ukuran gambar setelah: " + compressedSize + " bytes");
    System.out.println("Persentase kompresi: " + compressionRatio + "%");
    System.out.println("Kedalaman pohon: " + depth);
    System.out.println("Banyak simpul pada pohon: " + nodeCount);
  }

  // Fungsi untuk mendapatkan input path dan validasi
  private static String getInputPath(Scanner scanner) {
    String inputPath;
    BufferedImage testImage = null;
    while (true) {
      System.out.print("Masukkan path absolut gambar (contoh C:\\folder\\gambar.jpg): ");
      inputPath = scanner.nextLine().trim();
      File file = new File(inputPath);
      if (!file.exists() || !file.isFile()) {
        System.out.println("Maaf! Path tidak valid. Pastikan menyertakan direktori, bukan hanya nama file.");
        continue;
      }
      try {
        testImage = ImageIO.read(file);
      } catch (IOException e) {
        System.out.println("Maaf! Terjadi kesalahan saat membaca gambar: " + e.getMessage());
        continue;
      }
      if (testImage == null) {
        System.out.println("Maaf! Format gambar tidak didukung atau file rusak. Silakan coba lagi.");
        continue;
      }
      break;
    }
    return inputPath;
  }

  // Fungsi untuk mendapatkan output path dan validasi
  private static String getOutputFilePath(Scanner scanner) {
    String filePath;
    String[] validExtensions = { "jpg", "jpeg", "png" };
    while (true) {
      System.out.print("\nMasukkan path absolut untuk file hasil kompresi (contoh C:\\folder\\hasil.jpg): ");
      filePath = scanner.nextLine().trim();
      File file = new File(filePath);
      String parent = file.getParent();
      String ext = getFileExtension(file);
      boolean validExtension = false;
      for (String valid : validExtensions) {
        if (ext.equals(valid)) {
          validExtension = true;
          break;
        }
      }
      if (!validExtension) {
        System.out.println("Maaf! Ekstensi file tidak valid. Pastikan ekstensi file adalah .jpg, .jpeg, atau .png.");
        continue;
      }
      if (parent == null || parent.isEmpty()) {
        System.out.println("Maaf! Path tidak valid. Pastikan menyertakan direktori, bukan hanya nama file.");
        continue;
      }
      break;
    }
    return filePath;
  }

  // Fungsi untuk mendapatkan output gif dan validasi
  private static String getOutputGifPath(Scanner scanner) {
    String filePath;
    while (true) {
      System.out.print("\nMasukkan path absolut untuk file GIF hasil (contoh C:\\folder\\hasil.gif): ");
      filePath = scanner.nextLine().trim();
      if (!filePath.toLowerCase().endsWith(".gif")) {
        System.out.println("Maaf! Path harus diakhiri dengan ekstensi .gif. Silakan coba lagi.");
        continue;
      }
      File file = new File(filePath);
      String parent = file.getParent();
      if (parent == null || parent.isEmpty()) {
        System.out.println("Maaf! Path tidak valid. Pastikan menyertakan direktori, bukan hanya nama file.");
      } else {
        break;
      }
    }
    return filePath;
  }

  // Fungsi untuk mendapatkan BONUS 1 - Target persentase dan validasi
  private static double getValidatedTarget(Scanner scanner) {
    double target;
    while (true) {
      System.out.print("\nMasukkan target persentase kompresi (0.0-1.0 | 0 untuk menonaktifkan mode ini): ");
      String input = scanner.nextLine().replace(",", ".").trim();
      try {
        target = Double.parseDouble(input);
      } catch (NumberFormatException e) {
        System.out.println("Maaf! Input tidak valid. Silakan masukkan angka desimal.");
        continue;
      }
      if (target < 0.0 || target > 1.0) {
        System.out.println("Maaf! Input harus berada di antara 0.0 dan 1.0. Silakan coba lagi.");
      } else {
        break;
      }
    }
    return target;
  }

  // Fungsi untuk BONUS 1, mencari threshold yang sesuai dengan keinginan user
  private static double autoAdjustThreshold(BufferedImage image, long originalSize, ErrorMeasurement.Metode metode,
      String format, double target) {
    double batasBawah, batasAtas;
    switch (metode) {
      case VARIANCE:
        batasBawah = 0;
        batasAtas = 20000;
        break;
      case MAD:
        batasBawah = 0;
        batasAtas = 100;
        break;
      case MPD:
        batasBawah = 0;
        batasAtas = 255;
        break;
      case ENTROPY:
        batasBawah = 0.5;
        batasAtas = 8;
        break;
      case SSIM:
        batasBawah = 0;
        batasAtas = 1;
        break;
      default:
        batasBawah = 0;
        batasAtas = 0;
    }

    double mid = batasBawah;

    for (int i = 0; i < 10; i++) {
      mid = (batasBawah + batasAtas) / 2;
      Quadtree qt = new Quadtree(mid, 1, metode);
      Node root = qt.buildTree(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
      BufferedImage compressed = qt.reconstructImage(root, image.getWidth(), image.getHeight());

      byte[] imageBytes = getImageBytes(compressed, format);
      long compressedSize = imageBytes.length;
      double currentCompression = (1 - ((double) compressedSize / (double) originalSize));

      if (currentCompression < target) {
        batasBawah = mid;
      } else {
        batasAtas = mid;
      }
    }
    return mid;
  }

  private static byte[] getImageBytes(BufferedImage image, String format) {
    ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, format, byteArrayOS);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return byteArrayOS.toByteArray();
  }

  /*
   * Fungsi untuk BONUS 1, mencari limit bawah dan atas persentase, karena
   * meskipun threshold 0 atau max, ada batasan sejauh mana dia bisa dikompresi
   */
  private static double[] getCompressionRange(BufferedImage image, long originalSize, ErrorMeasurement.Metode metode,
      String format) {
    double batasBawah, batasAtas;
    switch (metode) {
      case VARIANCE:
        batasBawah = 0;
        batasAtas = 20000;
        break;
      case MAD:
        batasBawah = 0;
        batasAtas = 100;
        break;
      case MPD:
        batasBawah = 0;
        batasAtas = 255;
        break;
      case ENTROPY:
        batasBawah = 0.5;
        batasAtas = 8;
        break;
      case SSIM:
        batasBawah = 0;
        batasAtas = 1;
        break;
      default:
        batasBawah = 0;
        batasAtas = 0;
    }
    double minCompression = getCompressionForThreshold(image, originalSize, metode, format, batasBawah);
    double maxCompression = getCompressionForThreshold(image, originalSize, metode, format, batasAtas);
    return new double[] { minCompression, maxCompression };
  }

  /*
   * Fungsi untuk BONUS 1, mencari limit bawah dan atas persentase, karena
   * meskipun threshold 0 atau max, ada batasan sejauh mana dia bisa dikompresi
   */
  private static double getCompressionForThreshold(BufferedImage image, long originalSize,
      ErrorMeasurement.Metode metode, String format, double threshold) {
    Quadtree qt = new Quadtree(threshold, 1, metode);
    Node root = qt.buildTree(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
    BufferedImage compressed = qt.reconstructImage(root, image.getWidth(), image.getHeight());
    byte[] imageBytes = getImageBytes(compressed, format);
    long compressedSize = imageBytes.length;
    return 1 - ((double) compressedSize / (double) originalSize);
  }
}

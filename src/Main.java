import java.awt.image.BufferedImage;
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

    // Input PATH gambar (ditentukan untuk mempermudah debugging)
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

    boolean skipGIF = false;
    if(image.getWidth() * image.getHeight() > 1920*1080){
      System.out.println("\nMaaf! Ukuran gambar terlalu besar untuk membuat GIF proses kompresi. Kompresi akan tetap dijalankan tanpa pembuatan GIF.");
      skipGIF = true;
    }

    // *** Membuat GIF animasi proses pembentukan Quadtree ***
    // GIF ini akan menunjukkan gambar dari 1 blok (level 1) hingga quadtree penuh (level max)
    if(!skipGIF){
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
          evoGifWriter.writeToSequence(frame,50);
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
      //System.out.println("Path yang dibaca: " + inputPath);
      System.out.println("File berhasil dibaca.\n");
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
    System.out.println("List Metode Perhitungan Error\n1. Variance (100 - 1000)\n2. MAD (5 - 50)\n3. MaxPixelDifference (10 - 200)\n4. Entropy (0.5 - 5)\n5. SSIM (0.1 - 0.3)");
    System.out.print("Pilih metode perhitungan error: ");

    int metodePilihan = scanner.nextInt();
    scanner.nextLine();
    if (metodePilihan < 1 || metodePilihan > 5) {
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
      scanner.nextLine();

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
      } else if(metode == ErrorMeasurement.Metode.SSIM && (threshold < 0.1 || threshold > 1)){
        System.out.println("Threshold tidak sesuai range untuk metode SSIM.");
      } else {
        break;
      }
    }
    return threshold;
  }

  // Fungsi untuk mendapatkan ukuran blok minimum
  private static int getMinBlockSize(Scanner scanner) {
    System.out.print("Masukkan ukuran blok minimum: ");
    int minBlockSize = scanner.nextInt();
    scanner.nextLine();
    return minBlockSize;
  }

  // Fungsi untuk mendapatkan file output dengan nama sesuai format
  /*private static File getOutputFile(String inputPath, String outputFolder) {
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
  } */

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
    return (dotIndex == -1) ? "png" : name.substring(dotIndex + 1);
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

  private static String getOutputFilePath(Scanner scanner) {
    String filePath;
    while (true) {
      System.out.print("\nMasukkan path absolut untuk file hasil kompresi (contoh C:\\folder\\hasil.jpg): ");
      filePath = scanner.nextLine().trim();
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

  private static String getOutputGifPath(Scanner scanner) {
    String filePath;
    while (true) {
      System.out.print("\nMasukkan path absolut untuk file GIF hasil (contoh C:\\folder\\hasil.gif): ");
      filePath = scanner.nextLine().trim();
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
}

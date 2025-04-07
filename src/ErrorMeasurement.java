import java.awt.*;
import java.awt.image.BufferedImage;

public class ErrorMeasurement {
  public enum Metode {
    VARIANCE, MAD, MPD, ENTROPY, SSIM;
  }

  public double variance(BufferedImage image, Rectangle area) {
    int jumlahPiksel = area.width * area.height;
    if (jumlahPiksel == 0) {
      return 0;
    }

    // Rata-rata tiap warna
    Color rataWarna = Node.avgColor(image, area);
    double meanRed = rataWarna.getRed();
    double meanGreen = rataWarna.getGreen();
    double meanBlue = rataWarna.getBlue();

    // Menghitung variansi tiap warna
    double variansiRed = 0;
    double variansiGreen = 0;
    double variansiBlue = 0;

    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x >= image.getWidth() || y >= image.getHeight()) {
          continue;
        }
        Color rgb = new Color(image.getRGB(x, y));
        variansiRed += Math.pow(rgb.getRed() - meanRed, 2);
        variansiGreen += Math.pow(rgb.getGreen() - meanGreen, 2);
        variansiBlue += Math.pow(rgb.getBlue() - meanBlue, 2);
      }
    }

    variansiRed /= jumlahPiksel;
    variansiGreen /= jumlahPiksel;
    variansiBlue /= jumlahPiksel;

    return ((variansiRed + variansiGreen + variansiBlue) / 3);
  }

  public double mad(BufferedImage image, Rectangle area) {
    int jumlahPiksel = area.width * area.height;
    if (jumlahPiksel == 0) {
      return 0;
    }

    // Rata-rata tiap warna
    Color rataWarna = Node.avgColor(image, area);
    double meanRed = rataWarna.getRed();
    double meanGreen = rataWarna.getGreen();
    double meanBlue = rataWarna.getBlue();

    // Menghitung variansi tiap warna
    double madRed = 0;
    double madGreen = 0;
    double madBlue = 0;

    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x >= image.getWidth() || y >= image.getHeight()) {
          continue;
        }
        Color rgb = new Color(image.getRGB(x, y));
        madRed += Math.abs(rgb.getRed() - meanRed);
        madGreen += Math.abs(rgb.getGreen() - meanGreen);
        madBlue += Math.abs(rgb.getBlue() - meanBlue);
      }
    }

    madRed /= jumlahPiksel;
    madGreen /= jumlahPiksel;
    madBlue /= jumlahPiksel;

    return ((madRed + madGreen + madBlue) / 3);
  }

  public double maxPixelDifference(BufferedImage image, Rectangle area) {
    int minRed = 255, maxRed = 0;
    int minGreen = 255, maxGreen = 0;
    int minBlue = 255, maxBlue = 0;

    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x >= image.getWidth() || y >= image.getHeight()) {
          continue;
        }

        Color rgb = new Color(image.getRGB(x, y));

        // Update nilai minimum
        minRed = Math.min(minRed, rgb.getRed());
        minGreen = Math.min(minGreen, rgb.getGreen());
        minBlue = Math.min(minBlue, rgb.getBlue());

        // Update nilai maksimumm
        maxRed = Math.max(maxRed, rgb.getRed());
        maxGreen = Math.max(maxGreen, rgb.getGreen());
        maxBlue = Math.max(maxBlue, rgb.getBlue());
      }
    }

    int deltaRed = maxRed - minRed;
    int deltaGreen = maxGreen - minGreen;
    int deltaBlue = maxBlue - minBlue;

    return ((deltaRed + deltaGreen + deltaBlue) / 3);
  }

  public double entropy(BufferedImage image, Rectangle area) {
    int jumlahPiksel = area.width * area.height;
    if (jumlahPiksel == 0) {
      return 0;
    }

    // Menghitung kemunculan setiap komponen warna
    int[] histogramRed = new int[256];
    int[] histogramGreen = new int[256];
    int[] histogramBlue = new int[256];

    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x >= image.getWidth() || y >= image.getHeight()) {
          continue;
        }

        Color pixel = new Color(image.getRGB(x, y));
        histogramRed[pixel.getRed()]++;
        histogramGreen[pixel.getGreen()]++;
        histogramBlue[pixel.getBlue()]++;
      }
    }

    // Menghitung probabilitasnya
    double[] probRed = new double[256];
    double[] probGreen = new double[256];
    double[] probBlue = new double[256];

    for (int i = 0; i < 256; i++) {
      probRed[i] = (double) histogramRed[i] / jumlahPiksel;
      probGreen[i] = (double) histogramGreen[i] / jumlahPiksel;
      probBlue[i] = (double) histogramBlue[i] / jumlahPiksel;
    }

    // Menghitung entropi
    double entropyRed = 0;
    double entropyGreen = 0;
    double entropyBlue = 0;

    for (int i = 0; i < 256; i++) {
      if (probRed[i] > 0) {
        entropyRed -= probRed[i] * (Math.log(probRed[i]) / Math.log(2));
      }
      if (probGreen[i] > 0) {
        entropyGreen -= probGreen[i] * (Math.log(probGreen[i]) / Math.log(2));
      }
      if (probBlue[i] > 0) {
        entropyBlue -= probBlue[i] * (Math.log(probBlue[i]) / Math.log(2));
      }
    }

    return ((entropyRed + entropyGreen + entropyBlue) / 3);
  }

  public double ssim(BufferedImage image, Rectangle area) {
    int jumlahPiksel = area.width * area.height;
    if (jumlahPiksel == 0) {
        return 0;
    }
    
    double sumR = 0, sumG = 0, sumB = 0;
    for (int y = area.y; y < area.y + area.height; y++) {
        for (int x = area.x; x < area.x + area.width; x++) {
            if (x >= image.getWidth() || y >= image.getHeight()) {
                continue;
            }
            Color c = new Color(image.getRGB(x, y));
            sumR += c.getRed();
            sumG += c.getGreen();
            sumB += c.getBlue();
        }
    }
    
    double muR = sumR / jumlahPiksel;
    double muG = sumG / jumlahPiksel;
    double muB = sumB / jumlahPiksel;
    
    double varR = 0, varG = 0, varB = 0;
    for (int y = area.y; y < area.y + area.height; y++) {
        for (int x = area.x; x < area.x + area.width; x++) {
            if (x >= image.getWidth() || y >= image.getHeight()) {
                continue;
            }
            Color c = new Color(image.getRGB(x, y));
            varR += Math.pow(c.getRed() - muR, 2);
            varG += Math.pow(c.getGreen() - muG, 2);
            varB += Math.pow(c.getBlue() - muB, 2);
        }
    }
    varR /= jumlahPiksel;
    varG /= jumlahPiksel;
    varB /= jumlahPiksel;
    
    // Konstanta untuk 8-bit per kanal
    //double C1 = Math.pow(0.01 * 255, 2); // tidak berperan karena luminansi menyederhana
    double C2 = Math.pow(0.03 * 255, 2);
    
    // Karena blok referensi (hasil kompresi) adalah blok homogen dengan nilai rata-rata,
    // maka perhitungan SSIM per kanal hanya didasarkan pada kontras:
    double ssimR = C2 / (varR + C2);
    double ssimG = C2 / (varG + C2);
    double ssimB = C2 / (varB + C2);
    
    // Rata-rata tertimbang misal: R:0.3, G:0.3, B:0.4
    double ssimRGB = 0.3 * ssimR + 0.3 * ssimG + 0.4 * ssimB;
    return ssimRGB;
  }

  // Metode ssim yang lama (berbasis blok homogen) masih ada, tetapi sekarang kita tambahkan ssimFull.
  // ssimFull membandingkan blok before dan after kompresi secara lengkap sesuai rumus.
  public double ssimFull(BufferedImage original, BufferedImage compressed, Rectangle area) {
    int N = area.width * area.height;
    if (N == 0) return 0;

    // Konstanta untuk stabilitas (untuk 8-bit per kanal)
    double L = 255;
    double K1 = 0.01, K2 = 0.03;
    double C1 = Math.pow(K1 * L, 2);
    double C2 = Math.pow(K2 * L, 2);

    // Hitung rata-rata (mu) untuk tiap kanal pada original dan compressed
    double sumOrR = 0, sumOrG = 0, sumOrB = 0;
    double sumCpR = 0, sumCpG = 0, sumCpB = 0;
    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x >= original.getWidth() || y >= original.getHeight() ||
            x >= compressed.getWidth() || y >= compressed.getHeight()) continue;
        Color orig = new Color(original.getRGB(x, y));
        Color comp = new Color(compressed.getRGB(x, y));
        sumOrR += orig.getRed(); sumOrG += orig.getGreen(); sumOrB += orig.getBlue();
        sumCpR += comp.getRed(); sumCpG += comp.getGreen(); sumCpB += comp.getBlue();
      }
    }
    double muOrR = sumOrR / N, muOrG = sumOrG / N, muOrB = sumOrB / N;
    double muCpR = sumCpR / N, muCpG = sumCpG / N, muCpB = sumCpB / N;

    // Hitung variansi dan kovarians untuk tiap kanal
    double varOrR = 0, varOrG = 0, varOrB = 0;
    double varCpR = 0, varCpG = 0, varCpB = 0;
    double covR = 0, covG = 0, covB = 0;
    for (int y = area.y; y < area.y + area.height; y++) {
      for (int x = area.x; x < area.x + area.width; x++) {
        if (x >= original.getWidth() || y >= original.getHeight() ||
            x >= compressed.getWidth() || y >= compressed.getHeight()) continue;
        Color orig = new Color(original.getRGB(x, y));
        Color comp = new Color(compressed.getRGB(x, y));
        double diffOrR = orig.getRed() - muOrR;
        double diffOrG = orig.getGreen() - muOrG;
        double diffOrB = orig.getBlue() - muOrB;
        double diffCpR = comp.getRed() - muCpR;
        double diffCpG = comp.getGreen() - muCpG;
        double diffCpB = comp.getBlue() - muCpB;
        varOrR += diffOrR * diffOrR;
        varOrG += diffOrG * diffOrG;
        varOrB += diffOrB * diffOrB;
        varCpR += diffCpR * diffCpR;
        varCpG += diffCpG * diffCpG;
        varCpB += diffCpB * diffCpB;
        covR += diffOrR * diffCpR;
        covG += diffOrG * diffCpG;
        covB += diffOrB * diffCpB;
      }
    }
    varOrR /= N; varOrG /= N; varOrB /= N;
    varCpR /= N; varCpG /= N; varCpB /= N;
    covR /= N; covG /= N; covB /= N;

    // Hitung SSIM per kanal menggunakan rumus penuh:
    double ssimR = ((2 * muOrR * muCpR + C1) * (2 * covR + C2)) / ((muOrR*muOrR + muCpR*muCpR + C1) * (varOrR + varCpR + C2));
    double ssimG = ((2 * muOrG * muCpG + C1) * (2 * covG + C2)) / ((muOrG*muOrG + muCpG*muCpG + C1) * (varOrG + varCpG + C2));
    double ssimB = ((2 * muOrB * muCpB + C1) * (2 * covB + C2)) / ((muOrB*muOrB + muCpB*muCpB + C1) * (varOrB + varCpB + C2));

    // Kombinasikan dengan bobot per kanal (misalnya: R:0.3, G:0.3, B:0.4)
    double ssimRGB = 0.3 * ssimR + 0.3 * ssimG + 0.4 * ssimB;
    return ssimRGB;
  }
}

package gamod;

import static java.lang.Math.*;
import java.util.Arrays;

public final class Fourier {
  public static void DFT(double[] ar, double[] ai, double[] br, double[] bi, int n) {
    for (int i = 0; i < n; i++) {
      br[i] = DFTReal(ar, ai, n, -2 * PI * i / n);
      bi[i] = DFTImag(ar, ai, n, -2 * PI * i / n);
    }
  }
  
  public static void DFTi(double[] ar, double[] ai, double[] br, double[] bi, int n) {
    for (int i = 0; i < n; i++) {
      br[i] = DFTReal(ar, ai, n, 2 * PI * i / n);
      bi[i] = DFTImag(ar, ai, n, 2 * PI * i / n);
    }
    normalize(n, br, bi);
  }

  private static double DFTReal(double[] ar, double[] ai, int n, double theta) {
    double x = 0;
    for (int k = 0; k < n; k++)
      x += ar[k] * cos(k * theta) - ai[k] * sin(k * theta);
    return x;
  }

  private static double DFTImag(double[] ar, double[] ai, int n, double theta) {
    double x = 0;
    for (int k = 0; k < n; k++)
      x += ar[k] * sin(k * theta) + ai[k] * cos(k * theta);
    return x;
  }

  public static void FFT(double[] ar, double[] ai, double[] br, double[] bi, int n) {
    shuffle(ar, ai, br, bi, n);
    for (int i = 1; i < n; i *= 2)
      FFTLoop(br, bi, i, n, cos(PI / i), sin(PI / i));
  }

  public static void FFTi(double[] ar, double[] ai, double[] br, double[] bi, int n) {
    shuffle(ar, ai, br, bi, n);
    for (int i = 1; i < n; i *= 2)
      FFTLoop(br, bi, i, n, cos(-PI / i), sin(-PI / i));
    normalize(n, br, bi);
  }

  private static void FFTLoop(double[] real, double[] imag, int i, int n, double cos, double sin) {
    double xr = 1.0;
    double xi = 0.0;
    for (int k = 0; k < i; k++) {
      for (int i1 = k; i1 < n; i1 += i * 2) {
        int i2 = i1 + i;
        double tr = xr * real[i2] - xi * imag[i2];
        double ti = xr * imag[i2] + xi * real[i2];
        real[i2] = real[i1] - tr;
        imag[i2] = imag[i1] - ti;
        real[i1] += tr;
        imag[i1] += ti;
      }
      double txr = xr;
      xr = xr * cos - xi * sin;
      xi = xi * cos + txr * sin;
    }
  }

  private static void shuffle(double[] ar, double[] ai, double[] br, double[] bi, int n) {
    int n2 = n >> 1;
    for (int i = 0, j = 0; i < n; i++) {
      if (i <= j) {
        br[i] = ar[j];
        bi[i] = ai[j];
        br[j] = ar[i];
        bi[j] = ai[i];
      }
      int k = n2;
      while (k > 0 && k <= j) {
        j -= k;
        k >>= 1;
      }
      j += k;
    }
  }

  private static void normalize(int n, double[] real, double[] imag) {
    for (int i = 0; i < n; i++) {
      real[i] /= n;
      imag[i] /= n;
    }
  }

  private final static int log2sintabLength = 9;
  private final static short[] sintab = new short[1 << log2sintabLength];
  private static final int costabIndex = sintab.length / 4;
  static {
    for (int i = 0; i < sintab.length; i++)
      sintab[i] = (short)(Math.round(32767 * sin(2 * PI * i / sintab.length)));
  }
  
  public static void fixFFT(short real[], short imag[], int m) {
    int n = 1 << m;
    if (m > log2sintabLength)
      throw new IllegalArgumentException("Input too long: " + n + " > " + sintab.length);
    shuffle(real, n);
    for (int i = 1, step = sintab.length / 2; i < n; i *= 2, step /= 2) {
      for (int j = 0; j < i; j++) {
        int cosj = sintab[costabIndex + j * step];
        int sinj = -sintab[j * step];
        for (int i1 = j; i1 < n; i1 += i * 2) {
          int i2 = i1 + i;
          int real1 = real[i1];
          int imag1 = imag[i1];
          int real2 = real[i2];
          int imag2 = imag[i2];
          int real2a = cosj * real2 - sinj * imag2 + (1 << 14) >> 15;
          int imag2a = cosj * imag2 + sinj * real2 + (1 << 14) >> 15;
          real[i1] = (short)((real1 + real2a) / 2);
          imag[i1] = (short)((imag1 + imag2a) / 2);
          real[i2] = (short)((real1 - real2a) / 2);
          imag[i2] = (short)((imag1 - imag2a) / 2);
        }
      }
    }
  }
  
  public static void fixFFTi(short real[], short imag[], int m) {
    int n = 1 << m;
    if (m > log2sintabLength)
      throw new IllegalArgumentException("Input too long: " + n + " > " + sintab.length);
    shuffle(real, imag, n);
    for (int i = 1, step = sintab.length / 2; i < n; i *= 2, step /= 2) {
      for (int j = 0; j < i; j++) {
        int cosj = sintab[costabIndex + j * step];
        int sinj = sintab[j * step];
        for (int i1 = j; i1 < n; i1 += i * 2) {
          int i2 = i1 + i;
          int real1 = real[i1];
          int imag1 = imag[i1];
          int real2 = real[i2];
          int imag2 = imag[i2];
          int real2a = cosj * real2 - sinj * imag2 + (1 << 14) >> 15;
          int imag2a = cosj * imag2 + sinj * real2 + (1 << 14) >> 15;
          real[i1] = (short)(real1 + real2a);
          imag[i1] = (short)(imag1 + imag2a);
          real[i2] = (short)(real1 - real2a);
          imag[i2] = (short)(imag1 - imag2a);
        }
      }
    }
    Arrays.fill(imag, (short)0);
  }

  private static void shuffle(short[] real, short[] imag, int n) {
    int i2 = 0;
    for (int i1 = 1; i1 < n; i1++) {
      int k = n >> 1;
      while (i2 + k >= n)
        k >>= 1;
      i2 = (i2 & (k - 1)) + k;
      if (i1 < i2) {
        short real1 = real[i1];
        real[i1] = real[i2];
        real[i2] = real1;
        short imag1 = imag[i1];
        imag[i1] = imag[i2];
        imag[i2] = imag1;
      }
    }
  }

  private static void shuffle(short[] real, int n) {
    int i2 = 0;
    for (int i1 = 1; i1 < n; i1++) {
      int k = n >> 1;
      while (i2 + k >= n)
        k >>= 1;
      i2 = (i2 & (k - 1)) + k;
      if (i1 < i2) {
        short real1 = real[i1];
        real[i1] = real[i2];
        real[i2] = real1;
      }
    }
  }
}

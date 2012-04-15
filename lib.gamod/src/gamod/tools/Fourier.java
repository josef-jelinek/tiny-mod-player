package gamod.tools;

import static java.lang.Math.*;

public final class Fourier {
  // DFT
  public static void DFT(int dir, int n, double[] ar, double[] ai, double[] br, double[] bi) {
    double f = -dir * 2.0 * PI / n;
    for (int i = 0; i < n; i++) {
      br[i] = 0;
      bi[i] = 0;
      double arg = f * i;
      for (int k = 0; k < n; k++) {
        double cosarg = cos(k * arg);
        double sinarg = sin(k * arg);
        br[i] += ar[k] * cosarg - ai[k] * sinarg;
        bi[i] += ar[k] * sinarg + ai[k] * cosarg;
      }
    }
    normalize(dir, n, br, bi);
  }

  // FFT using sin function
  public static void sinFFT(int dir, int n, double[] ar, double[] ai, double[] br, double[] bi) {
    shuffle(ar, ai, br, bi, n);
    for (int mmax = 1; mmax < n; mmax <<= 1) {
      double theta = dir * PI / mmax;
      double sinhalftheta = sin(0.5 * theta);
      FFTLoop(n, mmax, br, bi, -2.0 * sinhalftheta * sinhalftheta, sin(theta));
    }
    normalize(dir, n, br, bi);
  }

  private static void FFTLoop(int n, int mmax, double[] ar, double[] ai, double wpr, double wpi) {
    double wr = 1.0;
    double wi = 0.0;
    for (int m = 0; m < mmax; m++) {
      for (int i = m; i < n; i += mmax * 2) {
        int j = i + mmax;
        double tr = wr * ar[j] - wi * ai[j];
        double ti = wr * ai[j] + wi * ar[j];
        ar[j] = ar[i] - tr;
        ai[j] = ai[i] - ti;
        ar[i] += tr;
        ai[i] += ti;
      }
      double wtr = wr;
      wr += wr * wpr - wi * wpi;
      wi += wi * wpr + wtr * wpi;
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

  private static void normalize(int dir, int n, double[] ar, double[] ai) {
    if (dir == -1) {
      for (int i = 0; i < n; i++) {
        ar[i] /= n;
        ai[i] /= n;
      }
    }
  }

  private final static int log2sintabLength = 9;
  private final static short[] sintab = new short[1 << log2sintabLength];
  private static final int costabIndex = sintab.length / 4;
  static {
    for (int i = 0; i < sintab.length; i++)
      sintab[i] = (short)(Math.round(32767 * sin(2 * PI * i / sintab.length)));
  }
  
  public static void fixFFT(short real[], short imag[], int m, boolean inverse) {
    int n = 1 << m;
    if (m > log2sintabLength)
      throw new IllegalArgumentException("Input too long: " + n + " > " + sintab.length);
    shuffle(real, imag, n);
    for (int i = 1, k = sintab.length / 2; i < n; i *= 2, k /= 2) {
      for (int j = 0; j < i; j++) {
        int cosj = sintab[costabIndex + j * k];
        int sinj = inverse ? sintab[j * k] : -sintab[j * k];
        for (int i1 = j; i1 < n; i1 += i * 2) {
          int i2 = i1 + i;
          int real1 = real[i1];
          int imag1 = imag[i1];
          int real2 = cosj * real[i2] - sinj * imag[i2] + (1 << 14) >> 15;
          int imag2 = cosj * imag[i2] + sinj * real[i2] + (1 << 14) >> 15;
          if (inverse) {
            real[i1] = (short)(real1 + real2);
            imag[i1] = (short)(imag1 + imag2);
            real[i2] = (short)(real1 - real2);
            imag[i2] = (short)(imag1 - imag2);
          } else {
            real[i1] = (short)((real1 + real2) / 2);
            imag[i1] = (short)((imag1 + imag2) / 2);
            real[i2] = (short)((real1 - real2) / 2);
            imag[i2] = (short)((imag1 - imag2) / 2);
          }
        }
      }
    }
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
}

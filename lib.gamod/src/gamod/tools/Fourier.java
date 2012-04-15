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
    int istep = mmax << 1;
    for (int m = 0; m < mmax; m++) {
      for (int i = m; i < n; i += istep) {
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

  private final static int log2sintabLen = 9;
  private final static short[] sintab = new short[1 << log2sintabLen];
  static {
    for (int i = 0; i < sintab.length; i++)
      sintab[i] = (short)(32767 * sin(2 * PI * i / sintab.length));
  }
  
  public static int fixFFT(short fr[], short fi[], int m, boolean inverse) {
    int n = 1 << m;
    if (n > sintab.length)
      return -1;
    shuffle(fr, fi, n);
    int scale = 0;
    int k = log2sintabLen - 1;
    for (int l = 1; l < n; l *= 2) {
      boolean shift = true;
      if (inverse) {
        shift = false;
        for (int i = 0; i < n; ++i) {
          if (fr[i] > 16383 || fr[i] < -16383 || fi[i] > 16383 || fi[i] < -16383) {
            shift = true;
            break;
          }
        }
        if (shift)
          scale += 1;
      }
      int istep = l << 1;
      for (m = 0; m < l; ++m) {
        int j = m << k;
        int wr = sintab[j + sintab.length / 4];
        int wi = inverse ? sintab[j] : -sintab[j];
        if (shift) {
          wr >>= 1;
          wi >>= 1;
        }
        for (int i = m; i < n; i += istep) {
          j = i + l;
          int tr = fixMul(wr, fr[j]) - fixMul(wi, fi[j]);
          int ti = fixMul(wr, fi[j]) + fixMul(wi, fr[j]);
          int qr = fr[i];
          int qi = fi[i];
          if (shift) {
            qr >>= 1;
            qi >>= 1;
          }
          fr[j] = (short)(qr - tr);
          fi[j] = (short)(qi - ti);
          fr[i] = (short)(qr + tr);
          fi[i] = (short)(qi + ti);
        }
      }
      k -= 1;
    }
    return scale;
  }

  private static void shuffle(short[] ar, short[] ai, int n) {
    int ii = 0;
    for (int i = 1; i < n; ++i) {
      int k = n >> 1;
      while (ii + k >= n)
        k >>= 1;
      ii = (ii & (k - 1)) + k;
      if (i < ii) {
        int tr = ar[i];
        ar[i] = ar[ii];
        ar[ii] = (short)tr;
        int ti = ai[i];
        ai[i] = ai[ii];
        ai[ii] = (short)ti;
      }
    }
  }

  private static int fixMul(int a, int b) {
    int x = a * b >> 14;
    return (x & 1) + (x >> 1);
  }
}

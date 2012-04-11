package gamod.tools;

public final class Fourier {

  // DFT
  public static void DFT(int dir, int n, double[] ar, double[] ai, double[] br, double[] bi) {
    double f = -dir * 2.0 * Math.PI / n;
    for (int i = 0; i < n; i++) {
      br[i] = 0;
      bi[i] = 0;
      double arg = f * i;
      for (int k = 0; k < n; k++) {
        double cosarg = Math.cos(k * arg);
        double sinarg = Math.sin(k * arg);
        br[i] += ar[k] * cosarg - ai[k] * sinarg;
        bi[i] += ar[k] * sinarg + ai[k] * cosarg;
      }
    }
    normalize(dir, n, br, bi);
  }

  // FFT using sin function
  public static void sinFFT(int dir, int n, double[] ar, double[] ai, double[] br, double[] bi) {
    shuffle(n, ar, ai, br, bi);
    for (int mmax = 1; mmax < n; mmax <<= 1) {
      double theta = dir * Math.PI / mmax;
      double sinhalftheta = Math.sin(0.5 * theta);
      FFTLoop(n, mmax, br, bi, -2.0 * sinhalftheta * sinhalftheta, Math.sin(theta));
    }
    normalize(dir, n, br, bi);
  }

  // FFT using sqrt function
  public static void sqrtFFT(int dir, int n, double[] ar, double[] ai, double[] br, double[] bi) {
    shuffle(n, ar, ai, br, bi);
    double wpr = -1.0; 
    double wpi = 0.0;
    for (int mmax = 1; mmax < n; mmax <<= 1) {
      FFTLoop(n, mmax, br, bi, wpr, wpi);
      wpi = -dir * Math.sqrt((1.0 - wpr) / 2.0);
      wpr = Math.sqrt((1.0 + wpr) / 2.0);
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

  private static void shuffle(int n, double[] ar, double[] ai, double[] br, double[] bi) {
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
}

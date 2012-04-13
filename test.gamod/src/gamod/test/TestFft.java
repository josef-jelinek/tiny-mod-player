package gamod.test;

import static org.junit.Assert.*;
import gamod.tools.Fourier;
import org.junit.Test;

public class TestFft {
  @Test
  public final void testDFT() {
    int n = 8;
    double[] ar = new double[n];
    double[] ai = new double[n];
    double[] br = new double[n];
    double[] bi = new double[n];
    double[] cr = new double[n];
    double[] ci = new double[n];
    for (int i = 0; i < n; i++)
      ar[i] = 2.0 * i / n - 1.0;
    Fourier.DFT(1, n, ar, ai, br, bi);
    for (int i = 0; i < n; i++)
      assertEquals(br[i], -1.0, 0.001);
    Fourier.DFT(-1, n, br, bi, cr, ci);
    for (int i = 0; i < n; i++) {
      assertEquals(ar[i], cr[i], 0.001);
      assertEquals(ai[i], ci[i], 0.001);
    }
  }

  @Test
  public final void testSinFFT() {
    int n = 8;
    double[] ar = new double[n];
    double[] ai = new double[n];
    double[] br = new double[n];
    double[] bi = new double[n];
    double[] cr = new double[n];
    double[] ci = new double[n];
    for (int i = 0; i < n; i++)
      ar[i] = 2.0 * i / n - 1.0;
    Fourier.sinFFT(1, n, ar, ai, br, bi);
    for (int i = 0; i < n; i++)
      assertEquals(br[i], -1.0, 0.001);
    Fourier.sinFFT(-1, n, br, bi, cr, ci);
    for (int i = 0; i < n; i++) {
      assertEquals(ar[i], cr[i], 0.001);
      assertEquals(ai[i], ci[i], 0.001);
    }
  }

  @Test
  public final void test_fixFFT() {
    int M = 4;
    int N = 1 << M;
    short[] real = new short[N], imag = new short[N], a = new short[N];
    for (int i = 0; i < N; i++)
      real[i] = a[i] = (short)(16000 * i / N - 8000);
    assertEquals(0, Fourier.fixFFT(real, imag, M, false));
    for (int i = 0; i < N; i++)
      assertEquals(real[i], -500);
    assertEquals(0, Fourier.fixFFT(real, imag, M, true));
    for (int i = 0; i < N; i++)
      assertEquals(real[i], a[i], 5);
  }
}

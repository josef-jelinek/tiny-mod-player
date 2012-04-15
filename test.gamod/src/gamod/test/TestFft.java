package gamod.test;

import static org.junit.Assert.assertEquals;
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
    int log2size = 4;
    int size = 1 << log2size;
    short[] real = new short[size], imag = new short[size], orig = new short[size];
    for (int i = 0; i < size; i++)
      real[i] = orig[i] = (short)(16000 * i / size - 8000);
    Fourier.fixFFT(real, imag, log2size, false);
    for (int i = 0; i < size; i++)
      assertEquals(-500, real[i]);
    Fourier.fixFFT(real, imag, log2size, true);
    for (int i = 0; i < size; i++)
      assertEquals(orig[i], real[i], 2);
  }

  @Test
  public final void test_fixFFT_extreme_square() {
    int log2size = 4;
    int size = 1 << log2size;
    short[] real = new short[size], imag = new short[size], orig = new short[size];
    for (int i = 0; i < size; i++)
      real[i] = orig[i] = (short)(i < size / 2 ? 32767 : -32768);
    Fourier.fixFFT(real, imag, log2size, false);
    Fourier.fixFFT(real, imag, log2size, true);
    for (int i = 0; i < size; i++)
      assertEquals(orig[i], real[i], 10);
  }

  @Test
  public final void test_fixFFT_extreme_pulse() {
    int log2size = 4;
    int size = 1 << log2size;
    short[] real = new short[size], imag = new short[size], orig = new short[size];
    real[0] = orig[0] = 32767;
    Fourier.fixFFT(real, imag, log2size, false);
    Fourier.fixFFT(real, imag, log2size, true);
    for (int i = 0; i < size; i++)
      assertEquals(orig[i], real[i], 15);
  }
}

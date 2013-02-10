package gamod.test;

import static org.junit.Assert.assertEquals;
import gamod.Fourier;
import org.junit.Test;

public class TestFft {
  @Test
  public void both_direction_DFT() {
    int n = 8;
    double[] ar = new double[n], ai = new double[n];
    double[] br = new double[n], bi = new double[n];
    double[] cr = new double[n], ci = new double[n];
    for (int i = 0; i < n; i++)
      ar[i] = 2.0 * i / n - 1.0;
    Fourier.DFT(ar, ai, br, bi, n);
    for (int i = 0; i < n; i++)
      assertEquals(br[i], -1.0, 0.001);
    Fourier.DFTi(br, bi, cr, ci, n);
    for (int i = 0; i < n; i++) {
      assertEquals(ar[i], cr[i], 0.001);
      assertEquals(ai[i], ci[i], 0.001);
    }
  }

  @Test
  public void both_direction_FFT() {
    int n = 8;
    double[] ar = new double[n], ai = new double[n];
    double[] br = new double[n], bi = new double[n];
    double[] cr = new double[n], ci = new double[n];
    for (int i = 0; i < n; i++)
      ar[i] = 2.0 * i / n - 1.0;
    Fourier.FFT(ar, ai, br, bi, n);
    for (int i = 0; i < n; i++)
      assertEquals(br[i], -1.0, 0.001);
    Fourier.FFTi(br, bi, cr, ci, n);
    for (int i = 0; i < n; i++) {
      assertEquals(ar[i], cr[i], 0.001);
      assertEquals(ai[i], ci[i], 0.001);
    }
  }

  @Test
  public void both_direction_fixFFT() {
    int log2size = 4;
    int size = 1 << log2size;
    short[] real = new short[size], imag = new short[size], orig = new short[size];
    setRampUp(real, orig, size);
    Fourier.fixFFT(real, imag, log2size);
    for (int i = 0; i < size; i++)
      assertEquals(-500, real[i]);
    isSpectrumSymetric(real, imag, size);
    Fourier.fixFFTi(real, imag, log2size);
    for (int i = 0; i < size; i++)
      assertEquals(orig[i], real[i], 2);
  }

  @Test
  public void fixFFT_of_extreme_square() {
    int log2size = 4;
    int size = 1 << log2size;
    short[] real = new short[size], imag = new short[size], orig = new short[size];
    setSquare(real, orig, size);
    Fourier.fixFFT(real, imag, log2size);
    isSpectrumSymetric(real, imag, size);
    Fourier.fixFFTi(real, imag, log2size);
    for (int i = 0; i < size; i++)
      assertEquals(orig[i], real[i], 9);
  }

  @Test
  public void fixFFT_of_extreme_pulse() {
    int log2size = 4;
    int size = 1 << log2size;
    short[] real = new short[size], imag = new short[size], orig = new short[size];
    setPulse(real, orig);
    Fourier.fixFFT(real, imag, log2size);
    isSpectrumSymetric(real, imag, size);
    Fourier.fixFFTi(real, imag, log2size);
    for (int i = 0; i < size; i++)
      assertEquals(orig[i], real[i], 15);
  }

  private void setRampUp(short[] real, short[] orig, int size) {
    for (int i = 0; i < size; i++)
      real[i] = orig[i] = (short)(16000 * i / size - 8000);
  }

  private void setSquare(short[] real, short[] orig, int size) {
    for (int i = 0; i < size; i++)
      real[i] = orig[i] = (short)(i < size / 2 ? 32767 : -32768);
  }

  private void setPulse(short[] real, short[] orig) {
    real[0] = orig[0] = 32767;
  }

  private void isSpectrumSymetric(short[] real, short[] imag, int size) {
    for (int i1 = 1, i2 = size - 1; i1 < i2; i1++, i2--) {
      assertEquals(real[i1], real[i2]);
      assertEquals(imag[i1], -imag[i2]);
    }
  }
}

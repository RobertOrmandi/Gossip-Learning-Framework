package tests.gossipLearning.utils;

import gossipLearning.utils.Utils;

import java.util.Arrays;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {
  
  public void testRegression() {
    double[] array = new double[]{1, 2, 3, 4, 5};
    double[] exp = new double[]{5.0, 0.0};
    double[] res = Utils.regression(array);
    assertEquals(exp[0], res[0]);
    assertEquals(exp[1], res[1]);
    array = new double[]{0.1, 0.2, 0.3, 0.4, 0.5};
    exp = new double[]{0.5, 0.0};
    res = Utils.regression(array);
    assertEquals(exp[0], res[0]);
    assertEquals(exp[1], res[1]);
    array = new double[]{1, 1, 1, 1, 1};
    exp = new double[]{0.0, 1.0};
    res = Utils.regression(array);
    assertEquals(exp[0], res[0]);
    assertEquals(exp[1], res[1]);
    array = new double[]{0, 0, 0, 0, 0};
    exp = new double[]{0.0, 0.0};
    res = Utils.regression(array);
    assertEquals(exp[0], res[0]);
    assertEquals(exp[1], res[1]);
  }
  
  public void testIsPower2() {
    assertTrue(Utils.isPower2(1.0));
    assertTrue(Utils.isPower2(2.0));
    assertTrue(Utils.isPower2(1024.0));
    assertTrue(Utils.isPower2(0.5));
    assertFalse(Utils.isPower2(3.0));
  }
  
  public void testCDF() {
    assertEquals(0.5, Utils.cdf(0, 0, 1.0), 1E-5);
    assertEquals(0.64531, Utils.cdf(0.42019, 0.23694, 0.49170), 1E-5);
    assertEquals(Double.NaN, Utils.cdf(0, 0, 0));
  }
  
  public void testERF() {
    assertEquals(0.0, Utils.erf(0), 1E-5);
    assertEquals(0.84270, Utils.erf(1), 1E-5);
    assertEquals(-0.84270, Utils.erf(-1), 1E-5);
    assertEquals(1, Utils.erf(10), 1E-5);
  }
  
  public void testNormalize() {
    double[] vector = new double[]{0, 0, 0, 0, 0};
    double[] exp = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    assertTrue(Arrays.equals(exp, Utils.normalize(vector)));
    vector = new double[]{0, 0, 0, 0, 1};
    assertTrue(Arrays.equals(vector, Utils.normalize(vector)));
  }
}

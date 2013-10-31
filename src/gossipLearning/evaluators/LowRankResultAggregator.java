package gossipLearning.evaluators;

import gossipLearning.interfaces.ModelHolder;
import gossipLearning.interfaces.models.FeatureExtractor;
import gossipLearning.interfaces.models.MatrixBasedModel;
import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.Matrix;
import gossipLearning.utils.SparseVector;
import gossipLearning.utils.VectorEntry;
import gossipLearning.utils.jama.SingularValueDecomposition;

import java.util.Map;
import java.util.TreeMap;

public class LowRankResultAggregator extends FactorizationResultAggregator {
  private static final long serialVersionUID = -8883339773613587699L;
  protected static Matrix UST;
  protected static Matrix VT;
  protected static Matrix S;
  protected static Map<Integer, Matrix[]> pid2US;
  protected static Map<Integer, Matrix[]> pid2USTUSp;
  
  public LowRankResultAggregator(String[] modelNames, String[] evalNames) {
    super(modelNames, evalNames);
    pid2US = new TreeMap<Integer, Matrix[]>();
    pid2USTUSp = new TreeMap<Integer, Matrix[]>();
  }
  
  @Override
  public void push(int pid, int index, int userIdx, SparseVector userModel, ModelHolder modelHolder, FeatureExtractor extractor) {
    if (modelHolder.size() == 0) {
      return;
    }
    MatrixBasedModel model = (MatrixBasedModel)modelHolder.getModel(modelHolder.size() - 1);
    modelAges[index] = model.getAge();
    Matrix v = model.getV();
    // the cosine similarity of the eigenvalues should be 1
    double expected = 1.0;
    if (v.getRowDimension() == VT.getColumnDimension()) {
      // The trace of the US^TSUp matrix should contain the square of the eigenvalues.
      // US^T is the expected left eigenvectors multiplied by the corresponding eigenvalues.
      // USp is the computed left eigenvectors multiplied by the corresponding eigenvalues.
      lock.lock();
      if (!pid2US.containsKey(pid)) {
        Matrix[] tmpM = new Matrix[modelNames.length];
        for (int i = 0; i < tmpM.length; i++) {
          tmpM[i] = new Matrix(UST.getColumnDimension(), v.getColumnDimension());
        }
        pid2US.put(pid, tmpM);
        tmpM = new Matrix[modelNames.length];
        for (int i = 0; i < tmpM.length; i++) {
          tmpM[i] = new Matrix(UST.getRowDimension(), UST.getRowDimension());
        }
        pid2USTUSp.put(pid, tmpM);
      }
      Matrix USp = pid2US.get(pid)[index];
      Matrix USTUSp = pid2USTUSp.get(pid)[index];
      Matrix USi = model.getUSi(userModel);
      for (int i = 0; i < USi.getNumberOfColumns(); i++) {
        USTUSp.set(i, i, USTUSp.get(i, i) - (UST.get(i, userIdx) * USp.get(userIdx, i)));
        USp.set(userIdx, i, USi.get(0, i));
        USTUSp.set(i, i, USTUSp.get(i, i) + (UST.get(i, userIdx) * USp.get(userIdx, i)));
      }
      //Matrix USTUSp = UST.mul(USp);
      lock.unlock();
      
      
      // The trace of the V^Tv matrix should contain only 1s.
      // V^T is the expected right eigenvectors
      // v is the computed right eigenvectors
      Matrix VTv = VT.mul(v);
      for (int i = 0; i < Math.min(v.getColumnDimension(), v.getRowDimension()) && S.get(i, i) != 0.0; i++) {
      //for (int i = 0; i < v.getColumnDimension(); i++) {
        double predicted = Math.abs(VTv.get(i, i));
        for (int j = 0; j < evaluators[index].length; j++) {
          evaluators[index][j].evaluate(expected, predicted);
        }
        predicted = Math.abs(USTUSp.get(i, i));
        for (int j = 0; j < evaluators[index].length; j++) {
          evaluators[index][j].evaluate(expected, predicted / (S.get(i, i)*S.get(i, i)));
        }
      }
    } else {
      for (int j = 0; j < evaluators[index].length; j++) {
        evaluators[index][j].evaluate(expected, 0.0);
      }
    }
    push(pid, index);
  }
  
  public void setEvalSet(InstanceHolder evalSet) {
    lock.lock();
    if (ResultAggregator.evalSet == evalSet) {
      lock.unlock();
      return;
    }
    ResultAggregator.evalSet = evalSet;
    Matrix M = new Matrix(evalSet.size(), evalSet.getNumberOfFeatures());
    for (int i = 0; i < evalSet.size(); i++) {
      for (VectorEntry e : evalSet.getInstance(i)) {
        M.set(i, e.index, e.value);
      }
    }
    SingularValueDecomposition svd = new SingularValueDecomposition(M);
    UST = svd.getU().mul(svd.getS()).transpose();
    VT = svd.getV().transpose();
    S = svd.getS();
    lock.unlock();
  }
  
}

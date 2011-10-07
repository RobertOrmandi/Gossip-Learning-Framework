package gossipLearning.controls.observers.errorComputation;

import gossipLearning.InstanceHolder;
import gossipLearning.interfaces.ModelHolder;

/**
 * This abstract class describes the skeleton of error computators that 
 * can be used in our learning framework.
 * @author István Hegedűs
 *
 */
public abstract class AbstractErrorComputator {
  /**
   * Protocol identifier
   */
  protected final int pid;
  /**
   * Evaluation set
   */
  protected final InstanceHolder eval;
  
  /**
   * Constructor for error computator that stores the specified parameters.
   * @param pid process ID
   * @param eval evaluation set
   */
  public AbstractErrorComputator(int pid, InstanceHolder eval) {
    this.pid = pid;
    this.eval = eval;
  }
  
  /**
   * Computes the voted error on the specified node as nodeID based on the specified modelHolder 
   * @param modelHolder
   * @param nodeID
   * @return the array where the index represents the number of votes
   */
  public abstract double[] computeError(ModelHolder modelHolder, int nodeID);

}

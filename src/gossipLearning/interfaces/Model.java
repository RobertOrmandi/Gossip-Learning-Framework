package gossipLearning.interfaces;

import java.io.Serializable;
import java.util.Map;

/**
 * This interface describes the models that can be used in our system. 
 * This models should be online updatable.
 * @author Istvan Hegedus
 *
 */
public interface Model extends Serializable, Cloneable {
  
  /**
   * Returns a clone of this object.
   * @return the clone of this object
   */
  public Object clone();
  
  /**
   * This method is for initializing the member variables of the Model.
   */
  public void init();
  
  /**
   * This method updates the actual model with a training instance.
   * @param instance - the features that represents the instance
   * @param label - the class label of the training instance or Double.NaN in case of clustering
   */
  public void update(final Map<Integer, Double> instance, final double label);
  
  /**
   * This method can predict the label or the category of a given evaluation instance.
   * @param instance - the features that represents the instance 
   * @return returns the class label or the category that was predicted by the model 
   */
  public double predict(final Map<Integer, Double> instance);
}

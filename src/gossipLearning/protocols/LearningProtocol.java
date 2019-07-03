package gossipLearning.protocols;

import gossipLearning.evaluators.ResultAggregator;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.interfaces.models.LearningModel;
import gossipLearning.interfaces.models.Mergeable;
import gossipLearning.interfaces.models.Model;
import gossipLearning.interfaces.models.Partializable;
import gossipLearning.interfaces.protocols.AbstractProtocol;
import gossipLearning.messages.ModelMessage;
import gossipLearning.utils.BQModelHolder;
import gossipLearning.utils.InstanceHolder;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.transport.ChurnTransportM;

/**
 * This protocol uses multiple model holders, the number of model holder will be 
 * specified via the configuration file. Actually this protocol has the same number 
 * of model holders as the number of model names was set in the configuration.
 * 
 * @author István Hegedűs
 */
public class LearningProtocol extends AbstractProtocol {
  private static final String PAR_EXTRACTORPID = "extractorProtocol";
  protected static final String PAR_ARRGNAME = "aggrName";
  private static final String PAR_LEARNER = "learner";
  private static final String PAR_INCLUDE = "include.model";
  private static final String PAR_EVALNAMES = "evalNames";
  private static final String PAR_EVALPROB = "evalProbability";
  
  private static final String PAR_EPOCH = "epoch";
  private static final String PAR_BATCH = "batch";
  
  /**
   * Parameter name in the config file.
   * @hidden
   */
  public static final String PAR_WAIT = "numOfWaitingPeriods";
  protected final int extractorProtocolID;
  
  protected ResultAggregator resultAggregator;
  protected final double evaluationProbability;
  protected final int epoch;
  protected final int batch;
  
  /** @hidden */
  protected final String[] modelNames;
  /** @hidden */
  protected Model[] models;
  /** @hidden */
  protected BQModelHolder modelHolder;
  /** @hidden */
  protected final String[] evalNames;
  
  /**
   * Constructor which parses the content of a standard Peersim configuration file.
   *  
   * @param prefix
   */
  public LearningProtocol(String prefix) {
    super(prefix);
    // loading configuration parameters
    extractorProtocolID = Configuration.getPid(prefix + "." + PAR_EXTRACTORPID);
    
    String include = Configuration.getString(PAR_INCLUDE, null);
    String[] includes = include == null ? null : include.split("\\s");
    if (includes != null) {
      modelNames = new String[includes.length];
      for (int i = 0; i < includes.length; i++) {
        modelNames[i] = prefix + "." + PAR_LEARNER + "." + includes[i];
      }
    } else {
      modelNames = Configuration.getNames(prefix + "." + PAR_LEARNER);
    }
    
    evalNames = Configuration.getString(prefix + "." + PAR_EVALNAMES).split(",");
    evaluationProbability = Configuration.getDouble(prefix + "." + PAR_EVALPROB, 1.0);
    epoch = Configuration.getInt(prefix + "." + PAR_EPOCH, 1);
    batch = Configuration.getInt(prefix + "." + PAR_BATCH, 1);
    
    if (epoch < 0) {
      throw new IllegalArgumentException("Parameter epoch can not be negative: " + epoch);
    }
    if (batch < 0) {
      throw new IllegalArgumentException("Parameter batch can not be negative: " + batch);
    }
    
    // setting up learning related variables
    try {
      String aggrClassName = Configuration.getString(prefix + "." + PAR_ARRGNAME);
      resultAggregator = (ResultAggregator)Class.forName(aggrClassName).getConstructor(String[].class, String[].class).newInstance(modelNames, evalNames);
      modelHolder = new BQModelHolder(modelNames.length);
      models = new Model[modelNames.length];
      for (int i = 0; i < modelNames.length; i++){
        models[i] = (Model)Class.forName(Configuration.getString(modelNames[i])).getConstructor(String.class).newInstance(modelNames[i]);
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": ", e);
    }
  }
  
  /**
   * Copy constructor.
   */
  protected LearningProtocol(LearningProtocol a) {
    super(a);
    extractorProtocolID = a.extractorProtocolID;
    modelNames = a.modelNames;
    evalNames = a.evalNames;
    evaluationProbability = a.evaluationProbability;
    epoch = a.epoch;
    batch = a.batch;
    
    // setting up learning related variables
    resultAggregator = a.resultAggregator.clone();
    modelHolder = a.modelHolder.clone();
    models = new Model[a.models.length];
    for (int i = 0; i < models.length; i++) {
      models[i] = a.models[i].clone();
    }
  }
  
  /**
   * It produces a deep copy of the protocol.
   * 
   * @return Clone of the protocol instance.
   */
  @Override
  public LearningProtocol clone() {
    return new LearningProtocol(this);
  }
  
  protected void evaluate() {
    if (CommonState.r.nextDouble() < evaluationProbability) {
      for (int i = 0; i < models.length; i++) {
        resultAggregator.push(currentProtocolID, i, (LearningModel)models[i], ((ExtractionProtocol)currentNode.getProtocol(extractorProtocolID)).getModel());
      }
    }
  }
  
  /**
   * It sends the latest models to a uniformly selected random neighbor.
   */
  @Override
  public void activeThread() {
    boolean isChurnTransport = getTransport() instanceof ChurnTransportM;
    // evaluate
    if (isChurnTransport && !((ChurnTransportM)getTransport()).isOnline()) {
      return;
    }
    evaluate();
    
    // send
    for (int i = 0; i < models.length; i++) {
      Model model = models[i];
      if (model instanceof Partializable) {
        model = ((Partializable)models[i]).getModelPart();
      }
      // store the latest models in a modelHolder
      modelHolder.add(model);
    }
    if (!isChurnTransport) {
      // send the latest models to a random neighbor
      sendToRandomNeighbor(new ModelMessage(currentNode, modelHolder, currentProtocolID, true));
    } else {
      // send the latest models to a random online neighbor
      sendToOnlineNeighbor(new ModelMessage(currentNode, modelHolder, currentProtocolID, true));
    }
    
    modelHolder.clear();
  }

  /**
   * It processes an incoming modelHolder by updating and storing them with all of the
   * stored instances.
   */
  @Override
  public void passiveThread(ModelMessage message) {
    if (message.getTargetPid() == currentProtocolID) {
      updateModels(message);
    }
  }
  
  /**
   * Updates the models of the specified model holder and merges them if it is possible.
   * @param modelHolder container of models to update
   */
  protected void updateModels(ModelHolder modelHolder){
    // get instances from the extraction protocol
    InstanceHolder instances = ((ExtractionProtocol)currentNode.getProtocol(extractorProtocolID)).getInstances();
    for (int i = 0; i < modelHolder.size(); i++){
      // get the ith model from the modelHolder
      LearningModel recvModel = (LearningModel)modelHolder.getModel(i);
      // if it is a mergeable model, then merge them
      if (recvModel instanceof Mergeable){
        models[i] = (LearningModel)((Mergeable) models[i]).merge(recvModel);
      } else {
        models[i] = recvModel;
      }
      // updating the model with the local training samples
      ((LearningModel)models[i]).update(instances, epoch, batch);
    }
  }
  
  /**
   * Returns the aggregated result aggregate of this node.
   * @return the aggregated result.
   */
  public ResultAggregator getResults() {
    return resultAggregator;
  }
  
  /**
   * Sets the specified number of classes and number of features for the models.
   * @param numberOfClasses the number of classes to be set
   * @param numberOfFeatures the number of features to be set
   */
  public void setParameters(int numberOfClasses, int numberOfFeatures) {
    for (int i = 0; i < models.length; i++) {
      ((LearningModel)models[i]).setParameters(numberOfClasses, numberOfFeatures);
    }
  }
  
}
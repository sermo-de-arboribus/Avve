package avve.meka;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import meka.classifiers.multilabel.BR;
import meka.classifiers.multilabel.Evaluation;
import meka.core.MLUtils;
import meka.core.Result;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;

import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.StringToWordVector;


public class ThemaTrainer
{
	private static final Logger logger = LogManager.getLogger();
	private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("ErrorMessagesBundle", Locale.getDefault());
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	private static final int TRAINING_CLASS_THRESHOLD = 10;
	private static final int TEST_CLASS_THRESHOLD = 2;
	private static final String TRAINING_FLAG_NAME = "IsTrainingSet";
	
	public static void main(String[] args)
	{		
		try
		{
			if (args.length < 2 || args.length > 4)
			{
				throw new IllegalArgumentException(errorMessagesBundle.getString("avve.meka.argumentException"));
			}
			
			logger.info(String.format(infoMessagesBundle.getString("avve.meka.loadingTrainingData"), args[0]));
			Instances trainingInstances = DataSource.read(args[0]);
			MLUtils.prepareData(trainingInstances);
			
			logger.info(String.format(infoMessagesBundle.getString("avve.meka.loadingTestingData"), args[1]));
			Instances testInstances = DataSource.read(args[1]);
			MLUtils.prepareData(testInstances);
		    
		    // remove class attributes that are only present either in the training or in the set
			
			// get all attributes in training set
		    int numberOfTrainingAttributes = trainingInstances.numAttributes();
		    int numberOfClasses = trainingInstances.classIndex();
		    
		    SortedSet<String> classesInTrainingData = getSortedClassNames(trainingInstances, numberOfClasses);
		    SortedSet<String> nonClassAttributesInTrainingData = getSortedNonClassAttributeNames(trainingInstances, numberOfTrainingAttributes, numberOfClasses);

		    // reorder training set attributes 
		    trainingInstances = orderClassesAndAttributes(trainingInstances, numberOfTrainingAttributes, numberOfClasses, classesInTrainingData, nonClassAttributesInTrainingData);
		    
		    // combine classes and attribute names of training set
		    SortedSet<String> attributesInTrainingData = new TreeSet<String>();
		    attributesInTrainingData.addAll(classesInTrainingData);
		    attributesInTrainingData.addAll(nonClassAttributesInTrainingData);
		    
		    // get all attributes in testing set
		    int numberOfTestingAttributes = testInstances.numAttributes();
		    numberOfClasses = testInstances.classIndex();
		    int testingClassOffset = numberOfTestingAttributes - testInstances.classIndex();
		    
		    SortedSet<String> classesInTestingData = getSortedClassNames(testInstances, numberOfClasses);
		    SortedSet<String> nonClassAttributesInTestingData = getSortedNonClassAttributeNames(testInstances, numberOfTestingAttributes, numberOfClasses);
		    
		    // reorder testing set attributes
		    testInstances = orderClassesAndAttributes(testInstances, numberOfTestingAttributes, numberOfClasses, classesInTestingData, nonClassAttributesInTestingData);
		    
		    // combine classes and attribute names of testing set
		    SortedSet<String> attributesInTestingData = new TreeSet<String>();
		    attributesInTestingData.addAll(classesInTestingData);
		    attributesInTestingData.addAll(nonClassAttributesInTestingData);
		    
		    // intersect class names in both sets
		    SortedSet<String> classIntersection = new TreeSet<String>(attributesInTrainingData);
		    classIntersection.retainAll(attributesInTestingData);
		    
		    // get indices of intersected class in training set
		    int[] indicesOfRetainedClassesInTrainingSet = new int[classIntersection.size()];
		    int i = 0;
		    for(String attributeName : classIntersection)
		    {
		    	indicesOfRetainedClassesInTrainingSet[i] = trainingInstances.attribute(attributeName).index();
		    	i++;
		    }
		    
		    // get indices of intersected class in testing set
		    int[] indicesOfRetainedClassesInTestingSet = new int[classIntersection.size()];
		    int j = 0;
		    for(String attributeName : classIntersection)
		    {
		    	indicesOfRetainedClassesInTestingSet[j] = testInstances.attribute(attributeName).index();
		    	j++;
		    }
		    
		    Instances filteredTrainingSet = filterClassAttributes("Avve multiclass training dataset", trainingInstances, indicesOfRetainedClassesInTrainingSet, true);
		    
		    Instances filteredTestingSet = filterClassAttributes("Avve multiclass test dataset", testInstances, indicesOfRetainedClassesInTestingSet, true);
		    
			// Are both sets compatible with each other, with respect to the attribute sets?
		    String msg = filteredTrainingSet.equalHeadersMsg(filteredTestingSet);
		    if (msg != null)
		    {
		    	throw new IllegalStateException(msg);
		    }
		    
		    numberOfClasses = filteredTestingSet.numAttributes() - testingClassOffset;
		    
		    logger.info(String.format(infoMessagesBundle.getString("avve.meka.classLabels"), String.join(", ", classIntersection)));
		    
		    // Remove classes with class frequencies below a given threshold
		    ArrayList<Integer> classesToBeRemoved = new ArrayList<Integer>(numberOfClasses); // stores indices of classes that are to be removed
		    int[] trainingClassFrequencies = new int[numberOfClasses];
		    int[] testClassFrequencies = new int[numberOfClasses];
		    
		    for(i = 0; i < filteredTrainingSet.size(); i++)
		    {
		    	Instance instance = filteredTrainingSet.get(i);
		    	
		    	for(j = 0; j < numberOfClasses; j++)
		    	{
		    		if (instance.value(j) > 0)
		    		{
		    			trainingClassFrequencies[j]++;
		    		}
		    	}
		    }
		    
		    logger.info(String.format(infoMessagesBundle.getString("avve.meka.trainingClassFrequencies"), "[" + StringUtils.join(trainingClassFrequencies, ',') + "]"));
		    
		    for(i = 0; i < filteredTestingSet.size(); i++)
		    {
		    	Instance instance = filteredTestingSet.get(i);
		    	
		    	for(j = 0; j < numberOfClasses; j++)
		    	{
		    		if (instance.value(j) > 0)
		    		{
		    			testClassFrequencies[j]++;
		    		}
		    	}
		    }
		    
		    logger.info(String.format(infoMessagesBundle.getString("avve.meka.testClassFrequencies"), "[" + StringUtils.join(testClassFrequencies, ',') + "]"));

	    	int trainingClassThreshold = (args.length > 2 && args[2] != null) ? Integer.parseInt(args[2]) : TRAINING_CLASS_THRESHOLD;
	    	int testingClassThreshold = (args.length > 3 && args[3] != null) ? Integer.parseInt(args[3]) : TEST_CLASS_THRESHOLD;
	    	int numberOfRemovedClasses = 0;
		    for(i = 0; i < numberOfClasses; i++)
		    {
		    	if(trainingClassFrequencies[i] < trainingClassThreshold || testClassFrequencies[i] < testingClassThreshold)
		    	{
		    		classesToBeRemoved.add(i);
		    		numberOfRemovedClasses++;
		    	}
		    }
		    
		    numberOfClasses -= numberOfRemovedClasses;
		    
		    filteredTrainingSet = filterClassAttributes("Avve multiclass training dataset", filteredTrainingSet, classesToBeRemoved, false);
		    filteredTestingSet = filterClassAttributes("Avve multiclass test dataset", filteredTestingSet, classesToBeRemoved, false);
		    
		    // Open question: Do we want to remove instances without any class labels left?

		    // we need to calculate the word vector on the union of train and test instances
		    // so we set a flag first to be able to separate those two sets again later
		    addTrainingSetFlag(filteredTrainingSet, true);
		    addTrainingSetFlag(filteredTestingSet, false);
		    
		    // combine the training and testing sets
		    Instances combinedTrainAndTestSet = new Instances(filteredTrainingSet);
		    combinedTrainAndTestSet.addAll(filteredTestingSet);
		    
		    // Transform String attributes to word vectors for hyperonyms
		    Instances combinedHyperonymSet = stringAttributeToWordVector("hyperonyms", "hy_", combinedTrainAndTestSet);
		    
		    // Transform String attributes to word vectors for normal word vector
		    Instances combinedWordVectorizedSet = stringAttributeToWordVector("top-idf", "wv_", combinedHyperonymSet);
		    
		    // separate the training and testing set again
		    Instances wordVectorizedTrainingSet = new Instances(combinedWordVectorizedSet, 0);
		    Instances wordVectorizedTestingSet = new Instances(combinedWordVectorizedSet, 0);
		    int trainingFlagIndex = combinedWordVectorizedSet.attribute(TRAINING_FLAG_NAME).index();
		    		
		    for(Instance instance : combinedWordVectorizedSet)
		    {
		    	if(instance.value(trainingFlagIndex) != 0.0)
		    	{
		    		wordVectorizedTrainingSet.add(instance);
		    	}
		    	else
		    	{
		    		wordVectorizedTestingSet.add(instance);
		    	}
		    }
		    combinedWordVectorizedSet.clear();
		                                                                                                         
    		// instantiate an attribute filter         
    		removeTrainingSetFlag("Avve multiclass training set", wordVectorizedTrainingSet);
    		removeTrainingSetFlag("Avve multiclass testing set", wordVectorizedTestingSet);                                                                 
    		                    
		    // Dump cleaned training and test set files
		    ArffSaver saver = new ArffSaver();
		    saver.setFile(new File("tmp_train.arff"));
		    DataSink.write(saver, wordVectorizedTrainingSet);
		    
		    saver.setFile(new File("tmp_test.arff"));
			DataSink.write(saver, wordVectorizedTestingSet);

		    // Train
			BR brClassifier = new BR();
			//RAkELd rakeldClassifier = new RAkELd();
			SMO smoClassifier = new SMO();
			brClassifier.setClassifier(smoClassifier);
			//rakeldClassifier.setClassifier(smoClassifier);
			brClassifier.buildClassifier(wordVectorizedTrainingSet);
			//rakeldClassifier.buildClassifier(wordVectorizedTrainingSet);
		    
		    // Evaluate
			Result result = Evaluation.evaluateModel(brClassifier, wordVectorizedTrainingSet, wordVectorizedTestingSet, "PCut1", "5");
			//Result result = Evaluation.evaluateModel(rakeldClassifier, wordVectorizedTrainingSet, wordVectorizedTestingSet, "PCut1", "5");
			logger.info(brClassifier.getModel());
			logger.info(result);

		}
		catch (Exception exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
	}

	private static void addTrainingSetFlag(Instances instances, boolean isTrainingSet) throws Exception
	{
		instances.insertAttributeAt(new Attribute(TRAINING_FLAG_NAME), instances.numAttributes());
		
		int newAttributeIndex = instances.numAttributes() - 1;
		
		for(Instance instance : instances)
		{
			instance.setValue(newAttributeIndex, isTrainingSet ? 1.0 : 0.0);
		}
	}

	private static Instances stringAttributeToWordVector(String attributeName, String prefix, Instances instances) throws Exception
	{
		Instances filteredInstances = instances;
		
		StringToWordVector stringToWordVector = new StringToWordVector();
		if(null != instances.attribute(attributeName))
		{
			int attributeIndex = instances.attribute(attributeName).index();
			stringToWordVector.setAttributeIndicesArray(new int[] { attributeIndex });
			stringToWordVector.setAttributeNamePrefix(prefix);
			stringToWordVector.setInputFormat(instances);
			filteredInstances = Filter.useFilter(instances, stringToWordVector);
		}
		// if attributeName cannot be found, this will return the unmodified Instances object
		return filteredInstances;
	}

	private static Instances filterClassAttributes(String relationName, Instances instances, ArrayList<Integer> classesToBeRemoved, boolean invertSelection) throws Exception
	{
		int[] classThresholdFilterIndices = ArrayUtils.toPrimitive(classesToBeRemoved.toArray(new Integer[classesToBeRemoved.size()]));
		return filterClassAttributes(relationName, instances, classThresholdFilterIndices, invertSelection);
	}
	
	private static Instances filterClassAttributes(String relationName, Instances instances, int[] classesToBeRemoved, boolean invertSelection) throws Exception
	{
		if(classesToBeRemoved != null && classesToBeRemoved.length > 0)
		{
			// calculations for setting the new class index
			int originalClassIndex;
			int removedClasses;
			int newClassIndex;
			if(invertSelection)
			{
				originalClassIndex = instances.classIndex();
				removedClasses = instances.numAttributes() - classesToBeRemoved.length;
				newClassIndex = originalClassIndex - removedClasses;
			}
			else
			{
				originalClassIndex = instances.classIndex();
				removedClasses = classesToBeRemoved.length;
				newClassIndex = originalClassIndex - removedClasses;
			}
			
			// instantiate an attribute filter
			Remove classThresholdFilter = new Remove();
			classThresholdFilter.setAttributeIndicesArray(classesToBeRemoved);
			classThresholdFilter.setInvertSelection(invertSelection);
			classThresholdFilter.setInputFormat(instances);
			
			// do the filtering
			Instances filteredInstances = Filter.useFilter(instances, classThresholdFilter);
			
			// postprocess: update class index and relation name
			filteredInstances.setRelationName(relationName);
			filteredInstances.setClassIndex(newClassIndex);
			MLUtils.fixRelationName(filteredInstances, newClassIndex);
			return filteredInstances;
		}
		// if classesToBeRemoved argument is null or empty, just return the original instances
		else
		{
			return instances;
		}
	}


	private static SortedSet<String> getSortedClassNames(Instances instances, int numberOfClasses)
	{
		SortedSet<String> classesInTrainingData = new TreeSet<String>();
		// get attribute names and order class attributes and other attributes alphabetically
		for(int i = 0; i < numberOfClasses; i++)
		{
			classesInTrainingData.add(instances.attribute(i).name());
		}
		return classesInTrainingData;
	}
	
	private static SortedSet<String> getSortedNonClassAttributeNames(Instances instances, int numberOfAttributes, int numberOfClasses)
	{
		SortedSet<String> nonClassAttributesInTrainingData = new TreeSet<String>();
		for(int i = numberOfClasses; i < numberOfAttributes; i++)
		{
			nonClassAttributesInTrainingData.add(instances.attribute(i).name());
		}
		return nonClassAttributesInTrainingData;
	}
	
	// orders classes alphabetically in ascending order, then other attributes alphabetically in ascending order
	// this function assumes that the number of attributes does not change! (the Weka Reorder() function can also be used to delete or duplicate attributes, but 
	// this would mess up the class index for Meka!
	private static Instances orderClassesAndAttributes(Instances instances, int numberOfAttributes, int numberOfClasses, 
			SortedSet<String> classAttributeNames, SortedSet<String> nonClassAttributeNames) throws Exception
	{
		int classIndex = instances.classIndex();
		
		int[] classOrder = new int[numberOfAttributes]; // attribute indices of class attributes in sorted order
		String[] orderedClassNames = classAttributeNames.toArray(new String[0]);
		for(int i = 0; i < numberOfClasses; i++)
		{
			classOrder[i] = instances.attribute(orderedClassNames[i]).index();
		}
		String [] orderedAttributeNames = nonClassAttributeNames.toArray(new String[0]);
		for(int i = numberOfClasses; i < numberOfAttributes; i++)
		{
			classOrder[i] = instances.attribute(orderedAttributeNames[i - numberOfClasses]).index();
		}
		Reorder reorderFilter = new Reorder();
		reorderFilter.setAttributeIndicesArray(classOrder);
		reorderFilter.setInputFormat(instances);
		instances = Filter.useFilter(instances, reorderFilter);
		instances.setClassIndex(classIndex);
		return instances;
	}

	private static void removeTrainingSetFlag(String relationName, Instances instances) throws Exception
	{
	    		Remove removeTrainingSetFlag = new Remove();                                               
        		removeTrainingSetFlag.setAttributeIndicesArray(new int[] { instances.attribute(TRAINING_FLAG_NAME).index() });                                                    
        		removeTrainingSetFlag.setInputFormat(instances);
        		                                                                                          
        		// do the filtering                                                                       
        		instances = Filter.useFilter(instances, removeTrainingSetFlag);          
        		                                                                                          
        		// postprocess: update class index and relation name                                      
        		instances.setRelationName(relationName);                            
        		MLUtils.fixRelationName(instances, instances.classIndex());
    } 
}                                                           
        		                                                                                          
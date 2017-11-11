package avve.classify;

import java.io.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

public class ClassPredictor
{
	private static final Logger logger = LogManager.getLogger();
	
	private Classifier classifier;
	
	public ClassPredictor(String pathToModelFile)
	{
		try
		{
			classifier = loadModel(new File(pathToModelFile));
		}
		catch (ClassNotFoundException | IOException exc)
		{
			logger.error(exc.getLocalizedMessage(), exc);
		}
	}
	
	public String classify(Instances instances)
	{
		try
		{
			Instance instance = instances.firstInstance();
			double classValue = classifier.classifyInstance(instance);
			return instances.classAttribute().value((int) classValue);
		}
		catch (Exception exc)
		{
			exc.printStackTrace();
		}
		
		return "???";
	}
	
	private static Classifier loadModel(File path) throws IOException, ClassNotFoundException
	{
	    Classifier classifier;

	    FileInputStream fis = new FileInputStream(path);
	    ObjectInputStream ois = new ObjectInputStream(fis);

	    classifier = (Classifier) ois.readObject();
	    ois.close();

	    return classifier;
	}	
	
}
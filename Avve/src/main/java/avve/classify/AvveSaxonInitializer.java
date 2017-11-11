package avve.classify;

import javax.xml.transform.TransformerException;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Initializer;

public class AvveSaxonInitializer implements Initializer
{

	@Override
	public void initialize(Configuration configuration) throws TransformerException
	{
		System.out.println("Initialisiere AvveSaxon");
		configuration.registerExtensionFunction(new SaxonClassPredictorExtension());
		configuration.setValidation(false);
	}
}
package avve.classify;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import avve.extractor.XrffFileWriter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import weka.core.Instances;
import weka.core.converters.XRFFLoader;

public class SaxonClassPredictorExtension extends ExtensionFunctionDefinition
{
	@Override public StructuredQName getFunctionQName()
	{
		return new StructuredQName("weka", "http://weka.sourceforge.net", "classify");
	}
	
	@Override public SequenceType[] getArgumentTypes()
	{
		return new SequenceType[]
		{
				// first argument should be the root element of an xrff document; second argument the full path to a weka model file
				SequenceType.SINGLE_NODE, SequenceType.SINGLE_STRING
		};
	}
	
	@Override public SequenceType getResultType(SequenceType[] suppliedArgumentTypes)
	{
		return SequenceType.SINGLE_STRING;
	}
	
	@Override public ExtensionFunctionCall makeCallExpression()
	{
		return new ExtensionFunctionCall()
		{
			@Override public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException 
			{
				String classString = "";
				
				Processor saxonProcessor = new Processor(false);
				String xrffDocumentAsString = "";
		    	InputStream xrffDocumentAsInputStream = null;
				XRFFLoader xrffLoader = new XRFFLoader();

				net.sf.saxon.om.NodeInfo rootNodeInfo = ((net.sf.saxon.om.NodeInfo)arguments[0].head());
				XdmNode xdmNode = null;
				DocumentBuilder builder = saxonProcessor.newDocumentBuilder();
			
				try
				{
					xdmNode = builder.build(rootNodeInfo);
				}
				catch (SaxonApiException exc)
				{
					exc.printStackTrace();
				}

				Serializer serializer = saxonProcessor.newSerializer();
			    serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
			    serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
			    serializer.setOutputProperty(Serializer.Property.ENCODING, "UTF-8");
			    
			    try
				{
				    String xdmNodeAsString = serializer.serializeNodeToString(xdmNode);
					
				    // weka expects an internal dtd, so we need to add one to the document before we pass the xml doc
				    xrffDocumentAsString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator() + XrffFileWriter.dtd + System.lineSeparator() + xdmNodeAsString;
					xrffDocumentAsInputStream = IOUtils.toInputStream(xrffDocumentAsString, "UTF-8");
					xrffLoader.setSource(xrffDocumentAsInputStream);
					
				    Instances instances = xrffLoader.getDataSet();
				    
				    String modelFilePath = arguments[1].head().getStringValue();
				    
				    ClassPredictor classPredictor = new ClassPredictor(modelFilePath);
				    classString = classPredictor.classify(instances);
				}
			    catch (SaxonApiException | IOException exc)
				{
					exc.printStackTrace();
				}
			    
				return StringValue.makeStringValue(classString); 
			}
		}; 
	}
}
package avve.extractor;

/**
 * A typical call:
 * -folder "/home/kai/Dokumente/E-Books/Trainingsmenge" -lc -pc -wvs 100 -cv "/home/kai/git/Avve/Avve/src/main/resources/controlledvocabulary/vlb_wg_cv.txt"
 */
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.cli.Options;

public enum CommandLineArguments
{
	CONTROLLEDVOCABULARY("cv"),
	DONOTINDEXFOREIGNWORDS("dnifw"),
	FOLDER("folder"), 
	INPUT("i"), 
	LEMMACORRECTION("lc"), 
	MULTILABEL("ml"), 
	NORMALIZEURLS("urlnorm"),
	POSCORRECTION("pc"),
	USETHESAURUS("usethesaurus"),
	WARENGRUPPE("wg"), 
	WORDVECTORSIZE("wvs");
	
	private String commandLineArgument;
	private static final ResourceBundle infoMessagesBundle = ResourceBundle.getBundle("InfoMessagesBundle", Locale.getDefault());
	
	private CommandLineArguments(String arg)
	{
		    this.commandLineArgument = arg;
	}
	
	@Override
	public String toString()
	{
		return commandLineArgument;
	}
	
	public static Options getCommandLineOptions()
	{
		Options options = new Options();
		options.addOption(CommandLineArguments.CONTROLLEDVOCABULARY.toString(), "controlledvocabulary", true, infoMessagesBundle.getString("explainControlledVocabularyOption"));
		options.addOption(CommandLineArguments.DONOTINDEXFOREIGNWORDS.toString(), "donotindexforeignwords", false, infoMessagesBundle.getString("explainDoNotIndexForeignWordsOption"));
		options.addOption(CommandLineArguments.INPUT.toString(), "input", true, infoMessagesBundle.getString("explainInputOption"));
		options.addOption(CommandLineArguments.FOLDER.toString(), "inputfolder", true, infoMessagesBundle.getString("explainInputFolderOption"));
		options.addOption(CommandLineArguments.LEMMACORRECTION.toString(), "lemmacorrection", false, infoMessagesBundle.getString("explainLemmaCorrectionOption"));
		options.addOption(CommandLineArguments.MULTILABEL.toString(), "multilabel", false, infoMessagesBundle.getString("explainMultiLabelOption"));
		options.addOption(CommandLineArguments.NORMALIZEURLS.toString(), "normalizeurls", false, infoMessagesBundle.getString("explainUrlNormOption"));
		options.addOption(CommandLineArguments.POSCORRECTION.toString(), "poscorrection", false, infoMessagesBundle.getString("explainPosCorrectionOption"));
		options.addOption(CommandLineArguments.USETHESAURUS.toString(), "usethesaurus", false, infoMessagesBundle.getString("explainThesaurusOption"));
		options.addOption(CommandLineArguments.WARENGRUPPE.toString(), "warengruppe", true, infoMessagesBundle.getString("explainWarengruppeOption"));
		options.addOption(CommandLineArguments.WORDVECTORSIZE.toString(), "wordvectorsize", true, infoMessagesBundle.getString("explainWordVectorSizeOption"));
		return options;
	}
}
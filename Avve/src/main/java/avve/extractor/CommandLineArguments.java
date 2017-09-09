package avve.extractor;

public enum CommandLineArguments
{
	FOLDER("folder"), INPUT("i"), LEMMACORRECTION("lc"), POSCORRECTION("pc"), WARENGRUPPE("wg");
	
	private String commandLineArgument;
	
	private CommandLineArguments(String arg)
	{
		    this.commandLineArgument = arg;
	}
	
	@Override
	public String toString()
	{
		return commandLineArgument;
	}
}
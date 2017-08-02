package avve.extractor;

public enum CommandLineArguments
{
	INPUT("i"), FOLDER("folder"), WARENGRUPPE("wg");
	
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
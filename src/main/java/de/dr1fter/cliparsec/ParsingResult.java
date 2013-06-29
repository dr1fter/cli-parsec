package de.dr1fter.cliparsec;


public interface ParsingResult<T>
{
	<X> T options();
	String[] operands();
	Status status();
	
	public enum Status { SUCCESS, ERROR, HELP }
	//TODO: add sub-commands (Stack subcommands or similar)
	
	//Optional<SelectedSubCommand> subCommands();
//	public interface SelectedSubCommand
//	{
//		<S> S getSubCommand();
//		SelectedSubCommand pop();
//	}
}

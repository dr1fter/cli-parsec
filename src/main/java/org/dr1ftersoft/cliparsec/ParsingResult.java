package org.dr1ftersoft.cliparsec;


public interface ParsingResult<T>
{
	<X> T options();
	String[] operands();
	//TODO: add sub-commands (Stack subcommands or similar)
	
	/**
	 * 
	 * @return
	 */
	//Optional<SelectedSubCommand> subCommands();
	public interface SelectedSubCommand
	{
		<S> S getSubCommand();
		SelectedSubCommand pop();
	}
}

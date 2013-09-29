package de.dr1fter.cliparsec;

import com.google.common.base.Optional;

public interface ParsingResult<T>
{
	<X> T options();

	String[] operands();

	Status status();

	public enum Status
	{
		SUCCESS, ERROR, HELP
	}

	Optional<SelectedCommand> selectedCommand();

	// TODO: use a more sophisticated data structure for returning selected commands
	public interface SelectedCommand
	{
		String commandName();

		Optional<SelectedCommand> nestedCommand();
	}
}

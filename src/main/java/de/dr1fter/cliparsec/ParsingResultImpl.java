package de.dr1fter.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Deque;

import com.google.common.base.Optional;

class ParsingResultImpl<T> implements ParsingResult<T>
{
	private final T							options;
	private final String[]					operands;
	private final Status					status;
	private final Optional<SelectedCommand>	selectedCommand;

	public ParsingResultImpl(T options, Status status, String[] operands,
			Optional<SelectedCommand> selectedCommand)
	{
		this.options = checkNotNull(options);
		this.operands = checkNotNull(operands);
		this.status = checkNotNull(status);
		this.selectedCommand = checkNotNull(selectedCommand);
	}

	@Override
	public <X> T options()
	{
		return options;
	}

	@Override
	public de.dr1fter.cliparsec.ParsingResult.Status status()
	{
		return status;
	}

	@Override
	public String[] operands()
	{
		return operands;
	}

	@Override
	public Optional<de.dr1fter.cliparsec.ParsingResult.SelectedCommand> selectedCommand()
	{
		return selectedCommand;
	}

	static class SelectedCommandImpl implements SelectedCommand
	{
		private final String		commandName;
		Optional<SelectedCommand>	nestedCommand	= Optional.absent();

		SelectedCommandImpl(String commandName)
		{
			this.commandName = checkNotNull(commandName);
		}

		@Override
		public String commandName()
		{
			return this.commandName;
		}

		@Override
		public Optional<de.dr1fter.cliparsec.ParsingResult.SelectedCommand> nestedCommand()
		{
			return nestedCommand;
		}

	}

	static Optional<SelectedCommand> fromCommandStrStack(
			Deque<String> commandStack)
	{
		if (commandStack.isEmpty())
			return Optional.absent();

		SelectedCommandImpl selCmd = new SelectedCommandImpl(
				commandStack.removeLast());
		selCmd.nestedCommand = fromCommandStrStack(commandStack);
		return Optional.of((SelectedCommand) selCmd);
	}
}

package de.dr1fter.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;

class ParsingResultImpl<T> implements ParsingResult<T>
{
	private final T options;
	private final String[] operands;
	private final Status status;
	

	public ParsingResultImpl(T options, Status status, String[] operands)
	{
		this.options = checkNotNull(options);
		this.operands = checkNotNull(operands);
		this.status = checkNotNull(status);
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
	

}

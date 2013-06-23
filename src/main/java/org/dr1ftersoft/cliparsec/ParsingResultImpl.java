package org.dr1ftersoft.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;

class ParsingResultImpl<T> implements ParsingResult<T>
{
	private final T options;
	private final String[] operands;
	

	public ParsingResultImpl(T options, String[] operands)
	{
		this.options = checkNotNull(options);
		this.operands = checkNotNull(operands);
	}

	@Override
	public <X> T options()
	{
		return options;
	}

	@Override
	public String[] operands()
	{
		return operands;
	}
	

}

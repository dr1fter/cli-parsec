package de.dr1fter.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.OutputStream;

/**
 * The CLI parser. It accepts both an annotated options object and the actual command line arguments.
 * 
 * @author Christian Cwienk (dr1fter)
 * 
 */
public abstract class CliParser
{
	/**
	 * creates and returns a new command line parser. The parser will write output (if any) to the System.out output
	 * stream.
	 * 
	 * @return the newly created command line parser
	 */
	public static CliParser createCliParser()
	{
		return new CliParserImpl();
	}
	
	/**
	 * creates and returns a new command line parser. The parser will write output (if any) to the specified output
	 * stream.
	 * 
	 * @param out not <code>null</code>
	 * @return
	 */
	public static CliParser createCliParser(OutputStream out)
	{
		checkNotNull(out);
		return new CliParserImpl(out);
	}
	
	/**
	 * parses the given command line arguments into the given annotated options object according to the rules declared
	 * on said object.
	 * 
	 * @param options
	 *            not <code>null</code>
	 * @param rawArgs
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 * @throws Exception
	 *             on invalid arguments
	 */
	public abstract <T> ParsingResult<T> parse(T options, String... rawArgs)
			throws Exception;

	protected final OutputStream out;
	
	protected CliParser(OutputStream out)
	{
		this.out = checkNotNull(out);
	}
}

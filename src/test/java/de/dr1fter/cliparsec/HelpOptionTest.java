package de.dr1fter.cliparsec;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import de.dr1fter.cliparsec.annotations.Command;
import de.dr1fter.cliparsec.annotations.HelpOption;
import de.dr1fter.cliparsec.annotations.Option;

public class HelpOptionTest
{
	ByteArrayOutputStream	out			= new ByteArrayOutputStream();
	CliParser				examinee	= CliParser.createCliParser(out);

	String outputStr()
	{
		return out.toString();
	}

	ParsingResult<OptionsWithHelpOption>	result;

	@Test
	public void help_should_be_printed_on_no_args() throws Exception
	{
		parse(new String[0]);

		assertResultStatus_is_HELP();
		assertThat(outputStr(), containsString("--flagOption1"));
		assertThat(outputStr(), containsString("--option1"));
		assertThat(outputStr(), containsString("command"));
		assertThat(outputStr(), not(containsString("suboption1")));
		assertThat(outputStr(), not(containsString("subsubcommand")));
	}

	@Test
	public void description_should_be_printed() throws Exception
	{
		parse(new String[0]);

		assertThat(outputStr(), containsString("description of option1"));
		assertThat(outputStr(), containsString("description of flagOption1"));
	}

	@Test
	public void help_should_be_printed_on_help_long_option() throws Exception
	{
		parse("--haalp!", "--option1", "x");

		assertResultStatus_is_HELP();
	}

	@Test
	public void help_should_be_printed_on_help_short_option() throws Exception
	{
		parse("-eXf");

		assertResultStatus_is_HELP();
	}

	@Test
	public void help_should_be_printed_for_subcommand() throws Exception
	{
		parse("--haalp!", "command");

		assertResultStatus_is_HELP();
		assertThat(outputStr(), containsString("suboption1"));
		assertThat(outputStr(), containsString("subsubcommand"));
		assertThat(outputStr(), not(containsString("subsuboption1")));
	}

	@Test
	public void help_should_be_printed_for_subcommand_of_subcommand()
			throws Exception
	{
		parse("-X", "command", "subsubcommand");

		assertResultStatus_is_HELP();
		assertThat(outputStr(), containsString("subsuboption1"));
	}

	private void parse(String... parseArgs) throws Exception
	{
		result = examinee.parse(new OptionsWithHelpOption(), parseArgs);
	}

	private void assertResultStatus_is_HELP()
	{
		assertThat(result.status(), is(ParsingResult.Status.HELP));
	}

	private static class OptionsWithHelpOption
	{
		@Option(description="description of option1")
		private String	option1;
		@Option(description="description of flagOption1")
		private boolean	flagOption1;

		@HelpOption(longOption = "haalp!", shortOption = 'X')
		private Object	help;

		@Command(name = "command")
		SubCommand		command;

		private static class SubCommand
		{
			@Option
			String			suboption1;

			@Command(name = "subsubcommand")
			SubSubCommand	subsubcommand;

			private static class SubSubCommand
			{
				@Option
				String	subsuboption1;
			}
		}

	}
}

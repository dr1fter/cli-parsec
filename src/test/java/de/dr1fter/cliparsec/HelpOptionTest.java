package de.dr1fter.cliparsec;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import de.dr1fter.cliparsec.CliParser;
import de.dr1fter.cliparsec.ParsingResult;
import de.dr1fter.cliparsec.annotations.HelpOption;
import de.dr1fter.cliparsec.annotations.Option;

public class HelpOptionTest
{
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	CliParser examinee = CliParser.createCliParser(out);
	String outputStr(){return out.toString();}
	
	@Test
	public void help_should_be_printed_on_no_args() throws Exception
	{
		ParsingResult<OptionsWithHelpOption> result = 
				examinee.parse(new OptionsWithHelpOption(), new String[0]);
		
		assertThat(result.status(), is(ParsingResult.Status.HELP));
		assertThat(outputStr(), containsString("--flagOption1"));
		assertThat(outputStr(), containsString("--option1"));
	}
	
	@Test
	public void help_should_be_printed_on_help_long_option() throws Exception
	{
		ParsingResult<OptionsWithHelpOption> result = 
				examinee.parse(new OptionsWithHelpOption(), "--haalp!", "--option1", "x");
		
		assertThat(result.status(), is(ParsingResult.Status.HELP));
	}
	
	@Test
	public void help_should_be_printed_on_help_short_option() throws Exception
	{
		ParsingResult<OptionsWithHelpOption> result = 
				examinee.parse(new OptionsWithHelpOption(), "-eXf");
		
		assertThat(result.status(), is(ParsingResult.Status.HELP));
	}
	
	private static class OptionsWithHelpOption
	{
		@Option
		private String option1;
		@Option
		private boolean flagOption1;
		
		@HelpOption(longOption="haalp!", shortOption='X')
		private Object help;
	}
}
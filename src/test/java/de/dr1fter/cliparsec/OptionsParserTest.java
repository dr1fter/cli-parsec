package de.dr1fter.cliparsec;


import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.dr1fter.cliparsec.CliParser;
import de.dr1fter.cliparsec.ParsingResult;
import de.dr1fter.cliparsec.annotations.Option;

public class OptionsParserTest
{
	CliParser examinee = CliParser.createCliParser();
	
	@Test
	public void global_options_with_args_should_be_parsed() throws Exception
	{
		OptionsWithArgs opts = new OptionsWithArgs();
	
		examinee.parse(
				opts, 
				"-4", "four",
				"--option", "opt1",
				"--option2", "opt2",
				"--opt3", "3",
				"--arr", "ab",
				"--arr", "b",
				"--arr", "c",
				"--arr", "bc"
				);
		
		assertThat(opts.option1, equalTo("opt1"));
		assertThat(opts.option2, equalTo("opt2"));
		assertThat(opts.opt3, equalTo("3"));
		assertThat(opts.shortOpt4, equalTo("four"));
		assertThat(opts.arr, equalTo(new String[]{"ab","b","c","bc"}));
	}
	
	@Test
	public void global_options_without_args_should_be_parsed() throws Exception
	{
		OptionsWithoutArgs opts = new OptionsWithoutArgs();
		
		examinee.parse(opts, "-12", "-3");
		
		assertThat(opts.option1, is(true));
		assertThat(opts.option2, is(true));
		assertThat(opts.option3, is(true));
		assertThat(opts.option4, is(false));
	}	
	
	@Test
	public void parameters_after_operand_delimiter_should_not_be_parsed() throws Exception
	{
		OptionsWithArgs opts = new OptionsWithArgs();
		
		ParsingResult<OptionsWithArgs> operands = examinee.parse(opts, "-o", "option1", "--", "-2", "should", "--not.be", "-@parsed");
		
		
		assertThat(opts.option1, is("option1"));
		assertThat(opts.option2, is(nullValue()));
		assertThat(operands.operands(),is(new String[]{"-2", "should", "--not.be", "-@parsed"}));
	}
	
	private static class OptionsWithoutArgs
	{
		@Option(longOption="option1",shortOption='1', argCount=0)
		public boolean option1;
		
		@Option(shortOption='2', argCount=0)
		public boolean option2;
		
		@Option(shortOption='3')
		public boolean option3;
		
		@Option(shortOption='4', argCount=0)
		public boolean option4;
	}
	
	private static class OptionsWithArgs
	{
		@Option(longOption="option",shortOption='o')
		public String option1;
		@Option()
		public String option2;
		
		@Option(shortOption='2')
		public String opt3;
		@Option(longOption="long", shortOption='4')
		public String shortOpt4;
		
		@Option(maxOccurs=4)
		public String[] arr;
	}
	
	
}

package de.dr1fter.cliparsec;


import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import de.dr1fter.cliparsec.ParsingResult.Status;
import de.dr1fter.cliparsec.annotations.Option;

public class OptionsParserTest
{
	ByteArrayOutputStream	out			= new ByteArrayOutputStream();
	CliParser examinee = CliParser.createCliParser(out);

	String outputStr()
	{
		return out.toString();
	}


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
	public void options_with_attached_args_should_be_parsed() throws Exception
	{
		OptionsWithArgs opts = new OptionsWithArgs();
		examinee.parse(opts, "-4=four", "--option=opt1");
		
		assertThat(opts.option1, equalTo("opt1"));
		assertThat(opts.shortOpt4, equalTo("four"));
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
	
	@Test
	public void parameters_should_be_processed_properly() throws Exception
	{
		OptionsWithArgs opts = new OptionsWithArgs();
		
		ParsingResult<OptionsWithArgs> result = examinee.parse(opts, "first", "second", "third");
		
		assertThat(result.operands(), arrayContaining("first", "second", "third"));
	}
	
	@Test
	public void options_should_be_inherited() throws Exception
	{
		ExtendedOptions opts = new ExtendedOptions();

		examinee.parse(opts, "--option1", "value1", "--baseOption1", "value2");

		assertThat(opts.option1, is("value1"));
		assertThat(opts.baseOption1, is("value2"));
	}

	@Test
	public void required_option_should_be_enforced() throws Exception
	{
		OptionsWithRequiredOptions opts = new OptionsWithRequiredOptions();

		ParsingResult<OptionsWithRequiredOptions> result = examinee.parse(opts, "--optOption1");

		assertThat(result.status(), is(Status.ERROR));
		assertThat(outputStr(), containsString("the following arguments are required but were not present:"));
		assertThat(outputStr(), containsString("--reqOption2"));
	}

	@Test
	public void complex_required_conditions_should_work() throws Exception
	{
		OptionsWithComplexRequiredOptions opts = new OptionsWithComplexRequiredOptions();

		ParsingResult<OptionsWithComplexRequiredOptions> result = examinee.parse(opts, "--optOption1");

		assertThat(result.status(), is(Status.ERROR));
		assertThat(outputStr(), containsString("--req1"));//req1 must be set because optOption1 is present
		assertThat(outputStr(),not(containsString("--req2")));//req2 needs not be present because optOption1 is present
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

	private static class BaseOptions
	{
		@Option(longOption="baseOption1")
		String baseOption1;
	}

	private static class ExtendedOptions extends BaseOptions
	{
		@Option
		String option1;
	}

	private static class OptionsWithRequiredOptions
	{
		@Option(required="false")
		boolean optOption1;

		@Option(required="true")
		String reqOption2;
	}

	private static class OptionsWithComplexRequiredOptions
	{
		@Option
		boolean optOption1;

		@Option
		boolean optOption2;

		@Option(required="present(optOption1)|!present(optOption2)")
		boolean req1;

		@Option(required="!present(optOption1)&!present(optOption2)")
		boolean req2;
	}
}

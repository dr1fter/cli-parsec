package org.dr1ftersoft.cliparsec;


import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class GlobalOptionsParserTest
{
	GlobalOptionsParser examinee = new GlobalOptionsParser();
	
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
	
	private static class OptionsWithoutArgs
	{
		@GlobalOption(longOption="option1",shortOption='1', argCount=0)
		public boolean option1;
		
		@GlobalOption(shortOption='2', argCount=0)
		public boolean option2;
		
		@GlobalOption(shortOption='3', argCount=0)
		public boolean option3;
		
		@GlobalOption(shortOption='4', argCount=0)
		public boolean option4;
	}
	
	private static class OptionsWithArgs
	{
		@GlobalOption(longOption="option",shortOption='o')
		public String option1;
		@GlobalOption()
		public String option2;
		
		@GlobalOption(shortOption='2')
		public String opt3;
		@GlobalOption(longOption="long", shortOption='4')
		public String shortOpt4;
		
		@GlobalOption(maxOccurs=4)
		public String[] arr;
	}
	
	
}

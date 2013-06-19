package org.dr1ftersoft.cliparsec;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SubCommandTest
{
	OptionsParser	examinee	= new OptionsParser();

	@Test
	public void subCommandOptions_should_be_parsed() throws Exception
	{
		OptionsWithSubCommands opts = new OptionsWithSubCommands();
		
		examinee.parse(opts, "--option1", "command", "--subOption1");
		
		assertThat(opts.option1, is(true));
		assertThat(opts.subCommand.subOption1, is(true));
	}
	
	@Test
	public void subCommandOptions_should_be_parsed_if_multiple_subcommands_exist() throws Exception
	{
		OptionsWithSubCommands opts = new OptionsWithSubCommands();
		
		examinee.parse(opts, "--option1", "command2", "--subOption1");
		
		assertThat(opts.option1, is(true));
		
		assertThat(opts.subCommand2.subOption1, is(true));
		assertThat(opts.subCommand, is(nullValue()));
		
	}
	
	@Test
	public void subCommandOptions_with_nested_subcommands_should_be_parsed() throws Exception
	{
		OptionsWithSubCommandsWithSubCommands opts = new OptionsWithSubCommandsWithSubCommands();
		
		examinee.parse(opts, "--topOption", "abc" , "sub-command", "--option1", "command", "--subOption1");
		
		assertThat(opts.topOption, equalTo("abc"));
		assertThat(opts.subCommand.option1, is(true));
		assertThat(opts.subCommand.subCommand.subOption1, is(true));
	}
	
	private static class OptionsWithSubCommands 
	{
		@Option(argCount=0)
		public boolean option1;
		
		@Command(name="command")
		public SubCommand subCommand;
		@Command(name="command2")
		public SubCommand subCommand2;
		
		public class SubCommand
		{
			@Option(argCount=0)
			public boolean subOption1;
		}
	}
	
	private class OptionsWithSubCommandsWithSubCommands
	{
		@Option(argCount=1)
		private String topOption;
		
		@Command(name="sub-command")
		OptionsWithSubCommands subCommand;
	}
}

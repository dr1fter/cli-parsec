package de.dr1fter.cliparsec;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import de.dr1fter.cliparsec.Option;
import de.dr1fter.cliparsec.OptionsParser;

public class CollectionTests
{
	OptionsParser examinee = new OptionsParser();
	
	@Test
	public void args_should_be_assigned_to_list() throws Exception
	{
		OptionsWithList opts = new OptionsWithList();
		
		examinee.parse(opts, "--args", "a", "b", "c",
				"--args", "d", "e");
		
		assertThat(opts.args, contains("a", "b", "c", "d", "e"));
	}
	
	private class OptionsWithList
	{
		@Option()
		public List<String> args = newArrayList();		
		
	}
}

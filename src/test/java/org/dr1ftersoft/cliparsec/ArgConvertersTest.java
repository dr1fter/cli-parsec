package org.dr1ftersoft.cliparsec;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ArgConvertersTest
{
	OptionsParser examinee = new OptionsParser();
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Rule
	public TemporaryFolder tmpDir = new TemporaryFolder();
	
	private File file1;
	private File dir1;
	
	@Before
	public void setup() throws Throwable
	{
		file1 = tmpDir.newFile("file1");
		dir1 = tmpDir.newFolder("dir1");
	}
	
	@Test
	public void filesAndDirs_should_be_parsed() throws Exception
	{
		OptionsWithFiles opts = new OptionsWithFiles();
		
		File fileOrDir = new File("some/valid/path");
		examinee.parse(opts, "--option1", "opt1",
				"-1", "some/valid/path",
				"-2", dir1.getAbsolutePath(),
				"-3", file1.getAbsolutePath()
				);
		
		assertThat(opts.option1, is("opt1"));
		assertThat(opts.fileOrDirThatNeedsNotExist, is(fileOrDir));
		assertThat(opts.directory, is(dir1));
		assertThat(opts.file, is(file1));
	}
	
	@Test
	public void missing_file_is_reported_as_error() throws Exception
	{
		OptionsWithFiles opts = new OptionsWithFiles();
		
		thrown.expect(RuntimeException.class);
		examinee.parse(opts, "-1", "foo/bar", "-2", "this/dir/does/not/exist");
	}
	
	private class OptionsWithFiles
	{
		@Option()
		public String option1;
		
		@Option(converter=Converters.FileOrDirectory.class, shortOption='1')
		public File fileOrDirThatNeedsNotExist;
		
		@Option(converter=Converters.DirectoryThatExists.class, shortOption='2')
		public File directory;
		
		@Option(converter=Converters.FileThatExists.class, shortOption='3')
		public File file;
		
	}
}

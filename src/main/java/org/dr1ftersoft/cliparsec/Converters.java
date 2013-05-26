package org.dr1ftersoft.cliparsec;

import java.io.File;

import com.google.common.base.Function;

/**
 * Converters that can be attached to Option specifications that in turn perform type conversions from 
 * the original string values to other types (e.g. file, directory, ..)
 * 
 * @author dr1fter
 *
 */
class Converters
{
	static class Identity implements Function<String,String>
	{
		public String apply(String str)
		{
			return str;
		}
	}
	
	static class FileOrDirectory implements Function<String,File>
	{
		public File apply(String str)
		{
			return new File(str);
		}
	}
	
	static class FileThatExists implements Function<String,File>
	{
		public File apply(String str)
		{
			File f = new File(str);
			if (! f.isFile()) throw new RuntimeException("file does not exist: " + str);
			return f;
		}
	}
	
	static class DirectoryThatExists implements Function<String,File>
	{
		public File apply(String str)
		{
			File f = new File(str);
			if (! f.isDirectory()) throw new RuntimeException("directory does not exist: " + str);
			return f;
		}
	}
}
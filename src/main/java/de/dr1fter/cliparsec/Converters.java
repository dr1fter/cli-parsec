package de.dr1fter.cliparsec;

import java.io.File;

import com.google.common.base.Function;

/**
 * Converters that can be attached to Option specifications that in turn perform type conversions from the original
 * string values to other types (e.g. file, directory, ..)
 * 
 * @author dr1fter
 * 
 */
public class Converters
{
	public static class Identity implements Function<String, String>
	{
		public String apply(String str)
		{
			return str;
		}
	}

	/**
	 * converts given <code>strings</code> to <code>File</code> objects. No verifications are done.
	 * 
	 * @author Christian Cwienk (dr1fter)
	 * 
	 */
	public static class FileOrDirectory implements Function<String, File>
	{
		public File apply(String str)
		{
			return new File(str);
		}
	}

	/**
	 * converts given <code>strings</code> that denote existing, accessible files to <code>File</code> objects. File
	 * existence is verified.
	 * 
	 * @author Christian Cwienk (dr1fter)
	 * 
	 */
	public static class FileThatExists implements Function<String, File>
	{
		public File apply(String str)
		{
			File f = new File(str);
			if (!f.isFile())
				throw new RuntimeException("file does not exist: " + str);
			return f;
		}
	}

	/**
	 * converts given <code>strings</code> that denote existing, accessible directories to <code>File</code> objects.
	 * Directory existence is verified.
	 * 
	 * @author Christian Cwienk (dr1fter)
	 * 
	 */
	public static class DirectoryThatExists implements Function<String, File>
	{
		public File apply(String str)
		{
			File f = new File(str);
			if (!f.isDirectory())
				throw new RuntimeException("directory does not exist: " + str);
			return f;
		}
	}

	/**
	 * converts given <code>strings</code> containing integer values to <code>Integer</code> objects.
	 * 
	 * @author Christian Cwienk (dr1fter)
	 * 
	 */
	public static class IntegerValue implements Function<String, Integer>
	{
		public Integer apply(String str)
		{
			return Integer.parseInt(str);
		}
	}
}

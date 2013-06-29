package de.dr1fter.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.copyOfRange;

class ArrayUtils
{
	/**
	 * returns the remainder of the given array after the first element was removed
	 * 
	 * @param arr
	 *            not <code>null</code>, not empty
	 * @return the remainder, never null, possibly the empty array
	 * @throws IllegalArgumentException
	 *             on empty array
	 */
	static <T> T[] tail(T[] arr)
	{
		checkNotNull(arr);
		return copyOfRange(arr, 1, arr.length);
	}
}

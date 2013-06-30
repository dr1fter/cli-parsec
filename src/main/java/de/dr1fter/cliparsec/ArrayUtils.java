package de.dr1fter.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.copyOfRange;

import java.lang.reflect.Array;

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

	/**
	 * creates and returns a copy of the given array containing all previously existing elements and the element to
	 * insert at the given position.
	 * 
	 * @param arr not <code>null</code>
	 * @param insertPos the element after which to insert
	 * @param value may be <code>null</code>
	 * @return the new array, never <code>null</code>
	 */
	static <T> T[] insertAfter(T[] arr, int insertPos, T value)
	{
		checkNotNull(arr);
		if (insertPos > arr.length)
			throw new ArrayIndexOutOfBoundsException(insertPos);

		@SuppressWarnings("unchecked")
		T[] narr = (T[]) Array.newInstance(arr.getClass().getComponentType(),
				arr.length + 1);
		for (int i = 0; i < insertPos + 1;)
			narr[i] = arr[i++];
		narr[insertPos + 1] = value;
		for (int i = insertPos + 2; i < narr.length;)
			narr[i] = arr[i++ - 1];

		return narr;
	}
}

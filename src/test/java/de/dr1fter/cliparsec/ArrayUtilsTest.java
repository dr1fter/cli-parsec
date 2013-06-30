package de.dr1fter.cliparsec;

import static de.dr1fter.cliparsec.ArrayUtils.insertAfter;
import static de.dr1fter.cliparsec.ArrayUtils.tail;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ArrayUtilsTest
{
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void tail_should_return_the_tail() throws Exception
	{
		final Integer[] arr = { 1, 2, 3 };

		Integer[] tail = tail(arr);

		assertThat(tail, is(new Integer[] { 2, 3 }));
	}

	@Test
	public void tail_should_return_the_empty_array_on_array_with_one_element()
			throws Exception
	{
		final Object[] arr = { new Object() };

		Object[] tail = tail(arr);

		assertThat(tail, is(new Object[0]));
	}

	@Test
	public void tail_should_fail_on_empty_array()
			throws Exception
	{
		final Object[] arr = {};
		
		thrown.expect(IllegalArgumentException.class);
		tail(arr);
	}
	
	@Test
	public void tail_should_fail_on_null_reference() throws Exception
	{
		thrown.expect(NullPointerException.class);
		tail(null);
	}
	
	@Test
	public void insertAfter_should_insert_after_the_specified_position() throws Exception
	{
		final Integer[] arr = {1,2,4,5};
		
		final Integer[] inserted = insertAfter(arr, 1, 3);
		
		assertThat(inserted, is(new Integer[]{1,2,3,4,5}));
	}
	
	@Test
	public void insertAfter_last_element_should_work() throws Exception
	{
		final Integer[] arr = {1,2,3};
		
		final Integer[] inserted = insertAfter(arr, 2, 4);
		
		assertThat(inserted, is(new Integer[]{1,2,3,4}));
	}
}

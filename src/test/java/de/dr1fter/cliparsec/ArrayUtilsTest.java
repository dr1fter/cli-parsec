package de.dr1fter.cliparsec;

import static de.dr1fter.cliparsec.ArrayUtils.tail;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
}

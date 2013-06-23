package org.dr1ftersoft.cliparsec;

import static com.google.common.collect.Iterables.size;
import static org.dr1ftersoft.cliparsec.ReflectionUtils.isPublic;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;

import org.junit.Test;

public class ReflectionUtilsTest
{
	/**
	 * Embedded class must remain static - otherwise it will implicitly gain an additional member referring to 
	 * its outer class, which will break tests that test for the returning of e.g. the proper amount of fields.
	 * <p>
	 * Making the type non-static will lead to having to adjust the then broken tests.
	 */
	static class Helper   
	{
		public Object _publicField = new Object();
		Object _defaultField = new Object();
		@SuppressWarnings("unused") //access via reflection
		private Object _privateField = new Object();
		protected Object _protectedField = new Object();
	}
	
	private final Helper helper = new Helper();
	
	private Field publicField() throws Exception
	{
		return helper.getClass().getField("_publicField");
	}
	
	private Field defaultField() throws Exception
	{
		return helper.getClass().getDeclaredField("_defaultField");
	}
	
	private Field privateField() throws Exception
	{
		return helper.getClass().getDeclaredField("_privateField");
	}
	
	private Field protectedField() throws Exception
	{
		return helper.getClass().getDeclaredField("_protectedField");
	}
	
	@Test
	public void isPublic_should_return_true_iff_field_is_public() throws Exception
	{
		assertThat(isPublic.apply(publicField()), is(true));
		assertThat(isPublic.apply(privateField()), is(false));
		assertThat(isPublic.apply(defaultField()), is(false));
		assertThat(isPublic.apply(protectedField()), is(false));
	}
	
	@Test
	public void allFieldsAsAccessible_should_return_all_fields_exactly_once() throws Exception
	{
		Iterable<Field> allFieldsAsAccessible = ReflectionUtils.allFieldsAsAccessible(helper.getClass());
		assertThat(size(allFieldsAsAccessible),is(4));
		assertThat(allFieldsAsAccessible, 
				containsInAnyOrder(publicField(), privateField(), defaultField(), protectedField()));
	}
}

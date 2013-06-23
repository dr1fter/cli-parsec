package org.dr1ftersoft.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * 
 * @author dr1fter
 *
 */
final class ReflectionUtils
{
	final static <T> T tryToCreateInstance(Class<T> clazz, Object outer)
	{
		checkNotNull(clazz);
		
		if (clazz.isMemberClass())
			try //try constructing in case class is embedded in an outer class  
			{
				Constructor<T> ctor = clazz.getDeclaredConstructor(outer.getClass());
				ctor.setAccessible(true);
				return ctor.newInstance(outer);
			}
			catch (Exception e)
			{//ignore
			}
		
		try //try again (non-static embedded or toplevel class)  
		{
			Constructor<T> ctor = clazz.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor.newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to create an instance of: " + clazz
					+"\nCan only automatically instantiate top-level types, embedded static or "
					+ "non-static embedded types that are embedded into: " + outer.getClass(),e);
		}
	}

	/**
	 * predicate that accepts accessible objects that are public and only those
	 */
	final static Predicate<Field> isPublic = 
	new Predicate<Field>()
	{
		public boolean apply(Field f)
		{
			checkNotNull(f);
			return f.getModifiers() == Modifier.PUBLIC;
			
		}
	};

	static Iterable<Field> allFieldsAsAccessible(Class<?> clazz)
	{
		Function<Field,Field> makeAccessible = makeAccessible();
		Iterable<Field> publicFields =
				from(asList(clazz.getFields()));
		
		Iterable<Field> nonpublicFields =
				from(asList(clazz.getDeclaredFields()))				
				.filter(Predicates.not(ReflectionUtils.isPublic))
				.transform(makeAccessible);
		//nonpublic fields are both inherited or declared fields of any visiblity
		// while public fields are only those fields that are public and declared in 'clazz'
		
		return concat(publicFields,nonpublicFields);
	}

	static <T extends AccessibleObject> Function<T,T> makeAccessible()
	{
		return new Function<T,T>()
		{
			public T apply(T o)
			{
				checkNotNull(o);
				o.setAccessible(true);
				return o;
			}
		};
	}

	static Predicate<Field> hasAnnotation(final Class<? extends Annotation> annotation)
	{
		return new Predicate<Field>()
		{
			public boolean apply(Field f)
			{
				checkNotNull(f);
				return f.getAnnotation(annotation) != null;
			}
		};
	}
}

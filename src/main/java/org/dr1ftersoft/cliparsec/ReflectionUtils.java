package org.dr1ftersoft.cliparsec;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Constructor;

final class ReflectionUtils
{
	final static <T> T tryToCreateInstance(Class<T> clazz, Object outer)
	{
		checkNotNull(clazz);
		
		Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
		
		if (clazz.isMemberClass())
			try //try contructing in case class is embedded in an outer class  
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
}

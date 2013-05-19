package org.dr1ftersoft.cliparsec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Command
{
	/**
	 * The command's name as it is to be used in the command line.
	 * <p>
	 * For example: <code>git <b>status</b></code> 	(in this case, 'status' is the command). 
	 * @return never <code>null</code>
	 */
	String name();
}

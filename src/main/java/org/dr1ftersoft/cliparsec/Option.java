package org.dr1ftersoft.cliparsec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.common.base.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option
{
	public final int MAX_OCCURS_DEFAULT_BEHAVIOUR = -1;
	public final char NOT_SET = '\00';
	
	char shortOption() default NOT_SET;

	String longOption() default "";

	/**
	 * The maximum amount of occurrences of this option. If not specified, the default behaviour is
	 * <ul>
	 * <li>1 - if the annotated field is a simple string</li>
	 * <li>unlimited - if the annotated field is an array of string</li>
	 * </ul>
	 * 
	 * <em>must not</em> be set to a value less than -1.
	 * 
	 * Use {@link Option#MAX_OCCURS_DEFAULT_BEHAVIOUR} to explicitly set this default behaviour. Setting a value > 1 for non-array fields will allow an option to
	 * be specified multiple times. In such a case, option values will be overwritten.
	 * 
	 * @return the maximum amount of occurrences
	 */
	int maxOccurs() default MAX_OCCURS_DEFAULT_BEHAVIOUR;
	
	/**
	 * The amount of arguments that follow this option (space-separated). Defaults to 1.
	 * @return
	 */
	int argCount() default 1;
	
	Class<?extends Function<String,?>> converter() default Converters.Identity.class;
}

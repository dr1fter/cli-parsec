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
	public final int	MAX_OCCURS_DEFAULT_BEHAVIOUR	= -1;
	public final int 	ARG_COUNT_DEFAULT_BEHAVIOUR 	= -1;
	public final char	NOT_SET							= '\00';

	char shortOption() default NOT_SET;

	String longOption() default "";

	/**
	 * The maximum amount of occurrences of this option. If not specified, the default behaviour is
	 * <ul>
	 * <li>1 - if the annotated field is a simple string</li>
	 * <li>unlimited - if the annotated field is an array or collection type</li>
	 * </ul>
	 * 
	 * <em>must not</em> be set to a value less than -1.
	 * 
	 * Use {@link Option#MAX_OCCURS_DEFAULT_BEHAVIOUR} to explicitly set this default behaviour. Setting a value > 1 for
	 * non-array fields will allow an option to be specified multiple times. In such a case, option values will be
	 * overwritten.
	 * 
	 * @return the maximum amount of occurrences
	 */
	int maxOccurs() default MAX_OCCURS_DEFAULT_BEHAVIOUR;

	/**
	 * The amount of arguments that follow this option (space-separated). Defaults to -1
	 * (one argument for regular options, unlimited for array type options).
	 * 
	 * @return
	 */
	int argCount() default ARG_COUNT_DEFAULT_BEHAVIOUR;

	/**
	 * An optional converter that is invoked for option arguments immediately after they are read from the raw cmd line
	 * args prior to assigning them to the annotated option field. If not specified, arguments remain strings (and thus
	 * the annotated field must be of type {@code String} (or {@code boolean} for options w/o args).
	 * <p>
	 * Apart from type conversion, any custom preprocessing steps may be performed.
	 * <p>
	 * 
	 * <em>Note: the return type of the converter <b>must</b> be assignment-compatible to the annotated field.</em>
	 * 
	 * @return the converter
	 */
	Class<? extends Function<String, ?>> converter() default Converters.Identity.class;

}

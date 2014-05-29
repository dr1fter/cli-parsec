package de.dr1fter.cliparsec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.common.base.Function;

import de.dr1fter.cliparsec.Converters;

/**
 * declares an option in the context of a command. The option's value is mapped to the field's value.
 * 
 * @author Christian Cwienk (dr1fter)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option
{
	public final int	MAX_OCCURS_DEFAULT_BEHAVIOUR	= -1;
	public final int	ARG_COUNT_DEFAULT_BEHAVIOUR		= -1;
	public final char	NOT_SET							= '\00';

	char shortOption() default NOT_SET;

	/**
	 * The long option as it is to be expected from the command line. Defaults to the field's name if not specified.
	 *  
	 * @return not <code>null</code>
	 */
	String longOption() default "";

	/**
	 * An expression desribing whether or not the option is "required" (meaning that its absence will be regarded
	 * as an error). Defaults to the empty string, which means that the option is optional.
	 * <p>
	 * The simplest possible expressions would be <code>true</code> or <code>false</code> (the latter being the 
	 * default.
	 * <p>
	 * The underlying expression language allows for the declarations of arbitrarily complex conditions of when a
	 * given option is to be regarded 'required' by the parser (e.g. an option1 might only be required if and only if
	 * an option2 is absent).
	 * <p>
	 * The grammar is defined as follows. Whitespace is discarded and may be placed arbitrarily.
	 * <p>
	 * &ltexpr&gt ::= &ltbool&gt | ( &ltexpr&gt ) | !&ltexpr&gt | &ltexpr&gt &ltoperator&gt &ltexpr&gt | present(&ltfield&gt)</br>
	 * &ltbool&gt ::= "true" | "false"</br>
	 * &ltoperator&gt ::= "&" | "|"</br>
	 * &ltfield&gt ::= alphanumeric+underscore (same as for fields in the Java language)
	 * 
	 * <p>
	 * <b>Precedence</b></br>
	 * The <code>AND (&)</code> operator has precedence over the <code>OR (|)</code> operator. Brackets may be
	 * used to enforce a different evaluation order. The <code>NOT (!)</code> operator is an unary operator 
	 * negating its <em>right</em> expression. It has higher precedence than the binary boolean operators.
	 * <p>
	 * <b>present function</b></br>
	 * The <code>present(field)</code> function is evaluated to <code>true</code> iff an argument for the 
	 * parameter described by the field is present at least once in the actual command line arguments (otherwise
	 * it evaluates to <code>false</code>). Only fields describing options (i.e. that are annotated with
	 * the @Option annotation and declared in the same type or a supertype) may be described (this may be extended
	 * in future versions of this library). Using field names that do not match the name of a field for which the
	 * afforedescribed conditions apply will lead to a <code>RuntimeException</code>.
	 * <code>
	 * Note: <em>Referencing fields this way is intrinsically refactoring-unsafe.</em> Therefore, it is recommended
	 * to document this at all referenced fields.
	 * 
	 * @Beta
	 * @return not <code>null</code>
	 */
	String required() default "";

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
	 * The amount of arguments that follow this option (space-separated). Defaults to
	 * {@link #ARG_COUNT_DEFAULT_BEHAVIOUR} (one argument for regular options, unlimited for array type options).
	 * 
	 * @return the configured amount of arguments or {@link #ARG_COUNT_DEFAULT_BEHAVIOUR}
	 */
	int argCount() default ARG_COUNT_DEFAULT_BEHAVIOUR;

	/**
	 * A brief description of the option that will be displayed in the generated help
	 *
	 * @return the description text to be displayed for the option. not <code>null</code>
	 */
	String description() default "";

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

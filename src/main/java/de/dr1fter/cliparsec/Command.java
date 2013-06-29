package de.dr1fter.cliparsec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A sub-command within a command line option object definition. The annotated field should be a complex type possessing
 * fields that are in turn annotated with {@link Option} or {@link Command} annotations. Fields may be initialised
 * before a option object is passed to the parser (in which case the existing instance will be kept). Otherwise the
 * field will be initialised during the parsing process (iff the corresponding sub-command is contained in the parsed
 * command line args).
 * <p>
 * <b>Limitations</b> Please note that automatic initialisation of sub-command fields will only work for types that are
 * <ul>
 * <li>top-level types</li>
 * <li>static embedded types</li>
 * <li>non-static embedded types that are enclosed by the same type in which the annotated member is declared</li>
 * </ul>
 * 
 * More concrete, automatic initialisation <em>will fail</em> for non-static embedded types that reside in any other
 * hosting type than the one possessing the member annotated with this annotation. In those cases, the annotated field
 * must be initialised with a non-null reference.
 * 
 * 
 * @author dr1fter
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Command
{
	/**
	 * The command's name as it is to be used in the command line.
	 * <p>
	 * For example: <code>git <b>status</b></code> (in this case, 'status' is the command).
	 * 
	 * @return never <code>null</code>
	 */
	String name();
}

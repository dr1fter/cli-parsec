package org.dr1ftersoft.cliparsec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HelpOption
{
	char shortOption() default 'h';
	String longOption() default "help";
}

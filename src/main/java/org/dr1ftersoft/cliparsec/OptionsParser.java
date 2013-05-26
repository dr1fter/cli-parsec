package org.dr1ftersoft.cliparsec;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.lang.reflect.Array.newInstance;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class OptionsParser
{
	public <T> T parse(T options, String... rawArgs) throws Exception
	{
		Class<?> clazz = options.getClass();
		Iterable<Field> fields = annotatedFields(clazz);
		Iterable<CommandRegistration> commands = annotatedCommands(clazz);

		ParsingCtx ctx = new ParsingCtx(rawArgs, fields, commands);

		for (; ctx.hasNext();)
		{
			ctx.determineAndConsumeNextFields();
			ctx.setOrAppendToField(options);
		}

		String[] remainder = ctx.remainingArgs();
		if (remainder.length == 0)
			return options;

		// parse sub command if such a command exists.
		CommandRegistration subCommand = determineSubCommand_orFail(
				remainder[0], commands);

		parse(subCommand.field.get(options), tail(remainder));

		return options;
	}

	private String[] tail(String[] args)
	{
		return copyOfRange(args, 1, args.length);
	}

	private CommandRegistration determineSubCommand_orFail(String rawCmdArg,
			Iterable<CommandRegistration> commands)
	{
		Optional<CommandRegistration> subCommand = 
				from(commands)
				.firstMatch(CommandRegistration.commandWithName(rawCmdArg));
		
		if(subCommand.isPresent()) return subCommand.get();
		
		Iterable<String> commandNames = transform(commands, CommandRegistration.getCommandName);		
		String subCmdDescription = on(',').join(commandNames);

		throw new RuntimeException(format(
				"unexpected token: '%s' - expected a sub-command (one of: %s)",
				rawCmdArg, subCmdDescription));
	}

	/**
	 * determines and returns all fields from the given class that are annotated with the GlobalOptions annotation
	 * 
	 * @param clazz
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 */
	private Iterable<Field> annotatedFields(Class<?> clazz)
	{
		return from(Utils.allFieldsAsAccessible(clazz))
				.filter(Utils.hasAnnotation(Option.class));
	}
	
	private Iterable<CommandRegistration> annotatedCommands(Class<?> clazz)
	{		
		return
				from(Utils.allFieldsAsAccessible(clazz))
				.filter(Utils.hasAnnotation(Command.class))
				.filter(notNull())
				.transform(CommandRegistration.createCommandRegistation);		
	}

	private static class CommandRegistration
	{
		public final Field		field;
		public final Command	annotation;

		public CommandRegistration(Field field, Command annotation)
		{
			this.field = field;
			this.annotation = annotation;
		}

		/**
		 * function for usage w/ 'google-collections' - returns a cmd registration's command name.
		 */
		private final static Function<CommandRegistration, String>	getCommandName	= 
				new Function<CommandRegistration, String>()
				{
					public String apply(CommandRegistration commandRegistration)
					{
						return commandRegistration.annotation.name();
					}
				};
				
		private final static Function<Field,CommandRegistration> createCommandRegistation =
				new Function<Field,CommandRegistration>()
				{
					public CommandRegistration apply(Field field)
					{
						checkNotNull(field);
						Command commandAnnotation = field.getAnnotation(Command.class);
						if (commandAnnotation == null) return null;
						return new CommandRegistration(field, commandAnnotation);
					}
				};
				
		public final static Predicate<CommandRegistration> commandWithName(final String name)
		{
			return Predicates.compose(Predicates.equalTo(name), getCommandName);			
		}
	}

	private static class ParsingCtx
	{
		private final Set<FieldRegistration>		allFields;

		private Iterable<FieldRegistration>			currentFields	= null;
		private String[]							args;
		private int									pos				= 0;

		private final Iterable<CommandRegistration>	subCommands;

		public ParsingCtx(String[] args, Iterable<Field> fields,
				Iterable<CommandRegistration> subCommands)
		{
			this.args = args;
			this.allFields = newHashSet();
			for (Field f : fields)
				this.allFields.add(new FieldRegistration(f));
			this.subCommands = subCommands;
		}

		/**
		 * Returns a value indicating whether or not parsing in the current context ought to be continue. This is true
		 * in cases where there still are arguments to process that belong to the this parsing context.
		 * 
		 * @return <code>true</code> iff there is at least one more raw arg <em>and</em> the upcoming arg is
		 *         <em>not</em> a sub command. Otherwise <code>false</code>
		 */
		public boolean hasNext()
		{
			if (pos >= args.length)
				return false;
			// we have at least one more arg

			// --> ensure the next arg is not a subcommand (in which case we do not want to continue)
			String currentRawArg = args[pos];
			return !from(subCommands)
					.anyMatch(compose(equalTo(currentRawArg), CommandRegistration.getCommandName));
		}

		public String[] remainingArgs()
		{
			return copyOfRange(args, pos, args.length);
		}

		private String consume()
		{
			return args[pos++];
		}

		private Iterable<FieldRegistration> determineFields(String rawArg)
		{
			checkNotNull(rawArg);
			
			boolean longArg = rawArg.startsWith("--");
			boolean shortArg = !longArg && rawArg.startsWith("-");

			if (!(longArg | shortArg))
				throw new RuntimeException(
						format("not a valid token: '%s'"
								+ " - expected a long or short option (prefixed with -/--).",
								rawArg));
			String argName = rawArg.replaceFirst("^(--|-)", ""); // rm leading -/--

			if (shortArg)
				return _determineShortOptionField(argName);

			// we are handling a long option

			for (FieldRegistration fr : allFields)
			{
				String optionStr = Utils.longOption(fr);
				if (argName.equals(optionStr))
					return singleton(fr);
			}
			return null;
		}

		/**
		 * 
		 * @param argWithoutPrefix
		 *            argument without leading option character ('-'..)
		 * @return the matching field registrations or <code>null</code> if option could not be determined
		 */
		private Iterable<FieldRegistration> _determineShortOptionField(
				String argWithoutPrefix)
		{
			List<FieldRegistration> matchedRegistrations = newArrayList(); 
			// multiple short options may be specified - iterable over all chars:
			for (char shortOptionChar : argWithoutPrefix.toCharArray())
			{
				for (FieldRegistration fr : allFields)
				{
					Character optionChar = Utils.shortOption(fr);
					if (optionChar == null
							|| shortOptionChar != optionChar.charValue())
						continue;
					matchedRegistrations.add(fr);
				}
			}
			return matchedRegistrations;
		}

		public Iterable<FieldRegistration> determineAndConsumeNextFields()
		{
			String rawArg = consume();
			Iterable<FieldRegistration> fields = determineFields(rawArg);

			// TODO: state which tokens were expected
			if (fields == null)
				throw new RuntimeException(format("unexpected token: %s.",
						rawArg));

			int optsWithArg = 0;
			for (FieldRegistration fr : fields)
				if ((optsWithArg += fr.annotation.argCount() > 0 ? 1 : 0) > 1)
					throw new RuntimeException(
							"only a maximum of one option with arguments is allowed "
									+ "when grouping multiple short options: "
									+ rawArg);

			for (FieldRegistration field : fields)
				if (!field.hasAllowedOccursLeft())
					throw new RuntimeException(format(
							"no more occurrences allowed for token '%s'. "
									+ "Allowed occurences: %s", rawArg,
							field.describeAllowedOccurences()));

			return (currentFields = fields);
		}

		public <T> void setOrAppendToField(T options) throws Exception
		{
			for (FieldRegistration fieldRegistration : currentFields)
			{
				Field field = fieldRegistration.field;
				fieldRegistration.occurs++;

				if (fieldRegistration.annotation.argCount() == 0)
				{ // option w/o args
					field.set(options, true);
					continue;
				}

				int argCount = fieldRegistration.annotation.argCount();
				if (fieldRegistration.annotation.argCount() == Option.ARG_COUNT_DEFAULT_BEHAVIOUR)
				{
					if (!isCollectionOrArray(field))
						argCount = 1;
					else
						argCount = from(asList(remainingArgs()))
									.filter(Utils.allUntilNextOption())
									.size();
				}
				
				for (int i = 0; i < argCount; i++)
				{
					String valueStr = consume();
					Object value = fieldRegistration.converter.apply(valueStr);

					Class<?> fieldType = field.getType();
					if (fieldType.isArray())
					{
						Object[] originalValues = (Object[]) field.get(options);
						if (originalValues == null)
						{
							Object newArray = newInstance(fieldType.getComponentType(), 1);
							Array.set(newArray, 0, value);
							
							field.set(options, newArray);
						}
						else
						{
							Object[] newValues = copyOf(originalValues,
									originalValues.length + 1);
							newValues[newValues.length - 1] = value;
							field.set(options, newValues);
						}
						continue;
					}
					if (isCollectionType(field))
					{
						@SuppressWarnings("unchecked")
						Collection<Object> collection = (Collection<Object>)field.get(options);
						if (collection == null) continue;	//TODO: enable initialisation
						
						collection.add(value);
						continue;
					}
					field.set(options, value);
				}
			}
		}
		
		private boolean isCollectionOrArray(Field f)
		{
			return isCollectionType(f) || f.getType().isArray();
		}
		
		private boolean isCollectionType(Field f)
		{			
			return Collection.class.isAssignableFrom(f.getType());
		}

		private class FieldRegistration
		{
			public final Field	field;
			public final int	maxOccurs;
			public int			occurs;
			public final Option	annotation;
			public final Function<String,?> converter;

			public FieldRegistration(Field field)
			{
				this.field = field;
				this.annotation = field.getAnnotation(Option.class);
				this.maxOccurs = annotation.maxOccurs();
				try
				{
					this.converter = annotation.converter().newInstance();
				}catch(Throwable t){throw new RuntimeException(t); }
			}

			public boolean hasAllowedOccursLeft()
			{
				switch (maxOccurs)
				{
				case Option.MAX_OCCURS_DEFAULT_BEHAVIOUR:
					if (isCollectionOrArray(field))
						return true;
					return occurs < 1;
				default:
					return occurs < maxOccurs;
				}
			}

			public String describeAllowedOccurences()
			{
				switch (maxOccurs)
				{
				case Option.MAX_OCCURS_DEFAULT_BEHAVIOUR:
					if (field.getType().isArray())
						return "unlimited";
					return "1";
				default:
					return String.valueOf(maxOccurs);
				}
			}
		}

		private static class Utils
		{
			/**
			 * @param fr
			 *            not <code>null</code>
			 * @return the character denoting the short option or <code>null</code> if such a character cannot be
			 *         determined
			 */
			static final Character shortOption(FieldRegistration fr)
			{
				if (fr == null)
					throw new NullPointerException();

				if (fr.annotation.shortOption() != Option.NOT_SET)
					return fr.annotation.shortOption();

				// fallback to field name:
				String fieldName = fr.field.getName();

				if (fieldName.length() == 1)
					return fieldName.toCharArray()[0];

				return null;
			}

			static final String longOption(FieldRegistration fr)
			{
				if (fr == null)
					throw new NullPointerException();

				if (fr.annotation.longOption() != null
						&& !fr.annotation.longOption().isEmpty())
					return fr.annotation.longOption();

				// fallback to field name:
				return fr.field.getName();
			}
			
			
			static final Predicate<String> allUntil (final Predicate<String> predicate)
			{				
				return new Predicate<String>()
						{
							private boolean encounteredOption = false; 
							public boolean apply(String arg)
							{
								if (encounteredOption) return false;
								return !(encounteredOption = predicate.apply(arg));
							}
						};
			}
			
			static final Predicate<String> shortOption = new Predicate<String>()
					{
						public boolean apply(String arg)
						{
							return arg.startsWith("-");
						}
					};
			static final Predicate<String> longOption = new Predicate<String>()
					{
						public boolean apply(String arg)
						{
							return arg.startsWith("--");
						}
					};
					
			static final Predicate<String> allUntilNextOption()
			{
				return allUntil(or(shortOption,longOption));
			}
		}
	}
	
	private static class Utils
	{
		private static Iterable<Field> allFieldsAsAccessible(Class<?> clazz)
		{
			Function<Field,Field> makeAccessible = Utils.makeAccessible();
			Iterable<Field> publicFields =
					from(asList(clazz.getFields()));
			
			Iterable<Field> nonpublicFields =
					from(asList(clazz.getDeclaredFields()))				
					.filter(Predicates.not(Utils.isPublic))
					.transform(makeAccessible);
			//nonpublic fields are both inherited or declared fields of any visiblity
			// while public fields are only those fields that are public and declared in 'clazz'
			
			return concat(publicFields,nonpublicFields);
		}
		
		private static Predicate<Field> hasAnnotation(final Class<? extends Annotation> annotation)
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
		
		private static Predicate<Field> isPublic = 
			new Predicate<Field>()
			{
				public boolean apply(Field f)
				{
					checkNotNull(f);
					boolean isPublic = (f.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC;
					return isPublic;
				}
			};			
		
		private static <T extends AccessibleObject> Function<T,T> makeAccessible()
		{
			return new Function<T,T>()
			{
				public T apply(T o)
				{
					o.setAccessible(true);
					return o;
				}
			};
		}
	}
}

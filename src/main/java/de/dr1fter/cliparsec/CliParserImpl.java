package de.dr1fter.cliparsec;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Predicates.or;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static de.dr1fter.cliparsec.ArrayUtils.insertAfter;
import static de.dr1fter.cliparsec.ArrayUtils.tail;
import static de.dr1fter.cliparsec.ParsingResult.Status.HELP;
import static de.dr1fter.cliparsec.ParsingResult.Status.SUCCESS;
import static de.dr1fter.cliparsec.ParsingResultImpl.fromCommandStrStack;
import static de.dr1fter.cliparsec.ReflectionUtils.allFieldsAsAccessible;
import static de.dr1fter.cliparsec.ReflectionUtils.hasAnnotation;
import static de.dr1fter.cliparsec.ReflectionUtils.tryToCreateInstance;
import static java.lang.String.format;
import static java.lang.reflect.Array.newInstance;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import de.dr1fter.cliparsec.annotations.Command;
import de.dr1fter.cliparsec.annotations.HelpOption;
import de.dr1fter.cliparsec.annotations.Option;

/**
 * this is an internal implementation class.
 * @author dr1fter
 */
class CliParserImpl extends CliParser
{
	CliParserImpl()
	{		super(System.out);
	}
	
	CliParserImpl(OutputStream outStream)
	{
		super(checkNotNull(outStream));
	}
	
	/**
	 * parses the given command line arguments based on the command line interface definition derived from
	 * the given options object. The command line arguments are written to the given options object 
	 * (replacing any previously set values).
	 *
	 * 
	 * @param options the options object, not <code>null</code>
	 * @param rawArgs the command line arguments, not <code>null</code>
	 * @return the same options object that was passed in containing the parsed args
	 *  
	 * @throws Exception in case any parsing error occurred
	 */
	public <T> ParsingResult<T> parse(T options, String... rawArgs) throws Exception
	{
		return parse(null,options,rawArgs);
	}

	@SuppressWarnings("unchecked")
	private <T> ParsingResult<T> parse(ParsingCtx lastCtx, T options, String... rawArgs) throws Exception
	{
		checkNotNull(options);
		checkNotNull(rawArgs);

		Class<?> clazz = options.getClass();
		Iterable<Field> optionFields = annotatedOptionFields(clazz);
		Iterable<Field> helpOptionFields = annotatedHelpOptionFields(clazz);

		Iterable<CommandRegistration> commands = annotatedCommands(clazz);

		ParsingCtx ctx = new ParsingCtx(rawArgs, optionFields, helpOptionFields, commands);
		cpLastCtx(lastCtx,ctx);

		for (; ctx.hasNext();)
		{
			ctx.determineAndConsumeNextFields();
			ctx.setOrAppendToField(options);
		}
		String[] remainder = ctx.remainingArgs();

		//display help and exit if help option was specified or there was no arg at all
		if(ctx.helpOption() && remainder.length == 0 
				|| (rawArgs.length == 0 && helpOptionFields.iterator().hasNext()))
		{
			OutputStreamWriter osw = new OutputStreamWriter(out);
			osw.write(HelpFormatter.formatHelp(ctx));
			osw.flush();
			return new ParsingResultImpl<T>(options,HELP,remainder, fromCommandStrStack(ctx.getCmdStack()));
		}

		if (remainder.length == 0 || ctx.state == ParsingCtx.ParsingState.OPERANDS)
			return new ParsingResultImpl<T>(options,SUCCESS, remainder, fromCommandStrStack(ctx.getCmdStack()));

		// parse sub command if such a command exists.
		CommandRegistration subCommand = determineSubCommand_orFail(
				remainder[0], commands);
		initialiseSubCommand_ifRequired(subCommand,options);
		ctx.pushCommand(remainder[0]);

		return (ParsingResult<T>) parse(ctx, subCommand.field.get(options), tail(remainder));
	}

	/**
	 * copies a subset of a previous parsing ctx into the given new parsing ctx. if previous parsing ctx is the
	 * null reference, nothing is done.
	 * 
	 * @param lastCtxOrNull may be <code>null</code>
	 * @param newCtx the new ctx to copy to, not <code>null</code>
	 */
	private void cpLastCtx(ParsingCtx lastCtxOrNull, ParsingCtx newCtx)
	{
		if(lastCtxOrNull == null) return;
		newCtx.commands = lastCtxOrNull.commands;
		newCtx.helpOption = lastCtxOrNull.helpOption;
		//TODO: use another approach for ctx-inheritance / nesting. e.g. make ctx hierarchical?
	}

	private <T> void initialiseSubCommand_ifRequired(CommandRegistration subCommand, T options)
	{
		Field commandField = subCommand.field;
		commandField.setAccessible(true);
		
		try
		{
			Field declaredField = options.getClass().getDeclaredField(commandField.getName());
			if(declaredField.isAccessible())
				options.getClass().getField(commandField.getName());
			declaredField.setAccessible(true);
			
			if (commandField.get(options) != null) return; //was initialised - keep existing
			Object subCommandObject = tryToCreateInstance(commandField.getType(),options);
			commandField.set(options, subCommandObject);
		}
		catch(Exception e){ /*ignore*/ }
		
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
	 * determines and returns all fields from the given class that are annotated with the Options annotation
	 * 
	 * @param clazz
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 */
	private Iterable<Field> annotatedOptionFields(Class<?> clazz)
	{
		return from(allFieldsAsAccessible(clazz))
				.filter(hasAnnotation(Option.class));				
	}
	
	private Iterable<Field> annotatedHelpOptionFields(Class<?> clazz)
	{
		return from(allFieldsAsAccessible(clazz))
				.filter(hasAnnotation(HelpOption.class));
	}
	
	private Iterable<CommandRegistration> annotatedCommands(Class<?> clazz)
	{		
		return
				from(allFieldsAsAccessible(clazz))
				.filter(hasAnnotation(Command.class))
				.filter(notNull())
				.transform(CommandRegistration.createCommandRegistation);		
	}

	private static class CommandRegistration
	{
		public final Field		field;
		public final Command	annotation;	//TODO: implement a type 'EvaluatedCommand' that wraps interpreting 
											//the annotation (calculate effective argCount etc.)

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
	
	/**
	 * the delimiter character sequence that may optionally be specified to mark the end of options.
	 * any arguments that occur after this separator are treated as operands which are to be passed
	 * without any further modification.
	 */
	private static final String	OPTION_OPERAND_DELIMITER	= "--";
	private static final String	DASH	= "-";
	private static final String DDASH	= DASH + DASH;
	
	private static class ParsingCtx
	{
		private Deque<String> commands = new ArrayDeque<String>();
		
		private final Set<FieldRegistration>		allOptionFields;
		private final Set<HelpOptionFieldRegistration>	helpOptionFields;

		private Iterable<FieldRegistration>			currentFields	= null;
		private String[]							args;
		private int									pos				= 0;
		private ParsingState						state			= ParsingState.OPTIONS;
		private boolean 							helpOption 		= false;

		private final Iterable<CommandRegistration>	subCommands;

		public ParsingCtx(String[] args, Iterable<Field> optionFields, Iterable<Field> helpOptionFields,
				Iterable<CommandRegistration> subCommands)
		{
			this.args = args;
			this.allOptionFields = newHashSet();
			for (Field f : optionFields)
				this.allOptionFields.add(new FieldRegistration(f));
			this.helpOptionFields = newHashSet();
			for(Field f : helpOptionFields)
				this.helpOptionFields.add(new HelpOptionFieldRegistration(f));
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
			if (state == ParsingState.OPERANDS) return false;
			
			if (pos >= args.length)
				return false;
			// we have at least one more arg

			String currentRawArg = peek();
			
			//if delimiter sequence (--) is reached, any following args are not to be parsed
			if (currentRawArg.equals(OPTION_OPERAND_DELIMITER))
				return (state= ParsingState.OPERANDS)==null&consume()!=null;
			
			// --> ensure the next arg is not a subcommand (in which case we do not want to continue)
			boolean isSubCommand = from(subCommands)
					.anyMatch(compose(equalTo(currentRawArg), CommandRegistration.getCommandName));
			
			if(isSubCommand) return false;
			
			if(currentRawArg.startsWith(DASH)) return true;
			
			//if current arg is no option, it supposedly is a bare argument -> stop parsing
			return state == (state = ParsingState.OPERANDS);			
		}
		
		public boolean helpOption()
		{
			return helpOption;
		}
		
		public void pushCommand(String commandStr)
		{
			commands.push(commandStr);
		}
		
		public Deque<String> getCmdStack()
		{
			return commands;
		}
		

		public String[] remainingArgs()
		{
			return copyOfRange(args, pos, args.length);
		}

		private String consume()
		{
			return args[pos++];
		}
		
		private String peek()
		{
			return args[pos];
		}

		/**
		 * peeks the next argument (which must be an option). If the next arg contains a value separator ('='), then
		 * the value is split: the current arg is reset to the arg part before the separator, the remainder (without
		 * the separator) is inserted into the arguments array.
		 */
		private void splitArgOnPresentArg()
		{
			String currentArg = peek();
			if(!currentArg.contains("=") || currentArg.startsWith("=")) return;
			
			String[] argParts = currentArg.split("=",2);
			args[pos] = argParts[0];
			args = insertAfter(args, pos, argParts[1]);
		}

		private Iterable<FieldRegistration> determineFields(Arg arg)
		{
			checkNotNull(arg);
			
			boolean longArg = arg.longOption;
			boolean shortArg = arg.shortOption;

			if (!(longArg | shortArg))
				throw new RuntimeException(
						format("not a valid token: '%s'"
								+ " - expected a long or short option (prefixed with -/--).",
								arg.rawArg));
			String argName = arg.argName;
			if(argName.contains("="))
			{
				String[] args = argName.split("=",2);
				argName = args[0];
				
			}

			if (shortArg)
				return _determineShortOptionField(argName);

			// we are handling a long option

			for (FieldRegistration fr : allOptionFields)
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
				for (FieldRegistration fr : allOptionFields)
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
			splitArgOnPresentArg();
			String rawArg = consume();
			Arg arg = new Arg(rawArg);
			if(isHelpOption(arg))
			{
				this.state = ParsingState.HELP;
				this.helpOption = true;
				//help options do not have arguments by definition. --> return early.
				return (currentFields = emptySet()); 
			}
			Iterable<FieldRegistration> fields = determineFields(arg);

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

				if (fieldRegistration.argCount() == 0)
				{ // option w/o args
					field.set(options, true);
					continue;
				}

				int argCount = fieldRegistration.argCount();
				
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
		
		private boolean isHelpOption(Arg arg)
		{
			checkNotNull(arg);
			
			if(helpOptionFields.size() == 0) return false;
			
			if(arg.longOption || !arg.shortOption)
				return from(helpOptionFields).transform(Utils.toLongOption())
						.anyMatch(equalTo(arg.argName));
			if(arg.shortOption)
				return from(helpOptionFields).transform(Utils.toShortOption())
						.anyMatch(CharMatcher.anyOf(arg.argName));
			
			throw new IllegalStateException("can not happen.");
		}
		
		private boolean isCollectionOrArray(Field f)
		{
			return isCollectionType(f) || f.getType().isArray();
		}
		
		private boolean isCollectionType(Field f)
		{			
			return Collection.class.isAssignableFrom(f.getType());
		}

		private enum ParsingState
		{
			OPTIONS, OPERANDS, HELP
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
				this.field = checkNotNull(field);
				this.annotation = checkNotNull(field.getAnnotation(Option.class));
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
			
			public boolean hasArgs()
			{
				return argCount() > 0;
			}
			
			/**
			 * determines and returns the amount of arguments the registered option has in the context of the
			 * actual arguments that are currently being parsed(*).
			 * <p>
			 * This amount is determined using the following approach (descending precedence):
			 * 
			 * <ul> 
			 * <li>if the option has a user-set amount of arguments, this this value is used</li>
			 * <li>if the option does not have a user-set amount of arguments, the default behaviour applies</li>
			 * <li>boolean fields have by default no option</li>
			 * <li>non-boolean, non-collection, non-array typed fields have exactly one option</li>
			 * <li>for collection or array-typed fields every non-option argument immediately following the option
			 *     is assumed to be argument of said option</li> 
			 * </ul> 
			 * <p>
			 * (*) for collection- or array-typed option fields, this is a positive integer value 
			 * 
			 * @return the amount of arguments this option has, interpreted in the context of the actual args,
			 * 	always >=0
			 */
			public int argCount()
			{
				if (annotation.argCount() != Option.MAX_OCCURS_DEFAULT_BEHAVIOUR) return annotation.argCount();
				//default behaviour depends on annotated field's type
				
				if(isCollectionOrArray(field))	//read until next option
					return from(asList(remainingArgs()))
					.filter(Utils.allUntilNextOption())
					.size();
				
				if(field.getType() == Boolean.TYPE) return 0;	//flags do not have options
				
				return 1;	//one arg by default for non-array, non-collection, non-boolean fields
			}
			
			/**
			 * determines and returns the amount of parameters the option field expects. The returned value is 
			 * (as opposed to {@link #argCount()} insensitive of the actual arguments. I.e. for an arbitrary
			 * amount of arguments, -1 is returned.
			 * 
			 * @return the amount of formal arguments
			 */
			public int formalArgCount()
			{
				if (annotation.argCount() != Option.MAX_OCCURS_DEFAULT_BEHAVIOUR
						|| !isCollectionOrArray(field))
					return argCount();
				
				return annotation.argCount();
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

		private class HelpOptionFieldRegistration
		{
			private final Field field;
			private final HelpOption annotation;
			
			public HelpOptionFieldRegistration(Field field)
			{
				this.field = checkNotNull(field);
				this.annotation = checkNotNull(field.getAnnotation(HelpOption.class));
			}
			
		}
		
		private class Arg
		{
			private final String rawArg;
			private final boolean shortOption;
			private final boolean longOption;
			private final String argName;
			
			public Arg(String rawArg)
			{
				this.rawArg = checkNotNull(rawArg);
				
				this.longOption = rawArg.startsWith(DDASH);
				this.shortOption = !longOption && rawArg.startsWith(DASH);
				
				this.argName = rawArg.replaceFirst(format("^(%s|%s)",DDASH, DASH), ""); // rm leading -/--
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
			
			static final Character shortOption(HelpOptionFieldRegistration fr)
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
				
				if (!isNullOrEmpty(fr.annotation.longOption()))
					return fr.annotation.longOption();

				// fallback to field name:
				return fr.field.getName();
			}
			
			static final String longOption(HelpOptionFieldRegistration fr)
			{
				if (fr == null)
					throw new NullPointerException();
				
				if (!isNullOrEmpty(fr.annotation.longOption()))
					return fr.annotation.longOption();
				
				// fallback to field name:
				return fr.field.getName();
			}
			
			static final Function<HelpOptionFieldRegistration,String> toLongOption()
			{
				return new Function<HelpOptionFieldRegistration,String>()
				{
					public String apply(HelpOptionFieldRegistration input)
					{
						return longOption(input);
					}
				};
			}
			
			static final Function<HelpOptionFieldRegistration,Character> toShortOption()
			{
				return new Function<HelpOptionFieldRegistration,Character>()
						{
					public Character apply(HelpOptionFieldRegistration input)
					{
						return shortOption(input);
					}
						};
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
							return arg.startsWith(DASH);
						}
					};
			static final Predicate<String> longOption = new Predicate<String>()
					{
						public boolean apply(String arg)
						{
							return arg.startsWith(OPTION_OPERAND_DELIMITER);
						}
					};
					
			static final Predicate<String> allUntilNextOption()
			{
				return allUntil(or(shortOption,longOption));
			}
		}
	}
	
	/**
	 * don't use.
	 * 
	 * @author dr1fter
	 *
	 */
	@Beta
	static class HelpFormatter
	{
		@Beta
		public static String formatHelp(ParsingCtx ctx)
		{
			StringBuilder s = new StringBuilder();
			
			s.append("Options:\n");
			//handle options
			for(de.dr1fter.cliparsec.CliParserImpl.ParsingCtx.FieldRegistration f : ctx.allOptionFields)
			{
				Character shortOption = ParsingCtx.Utils.shortOption(f);
				String longOption = ParsingCtx.Utils.longOption(f);
				
				String shortText = shortOption != null? DASH + shortOption : null;
				String longText = longOption != null? DDASH + longOption : null;
				String beginning =  on('|').skipNulls().join(shortText,longText);
				
				String argList = null;
				
				if (f.hasArgs())
				{
					if(f.formalArgCount() == -1)
						argList = "<list>";
					else
					{
						List<String> args = newArrayList();
						for(int i = 0; i < f.argCount(); )
							args.add("arg" + i++);
						argList = on(' ').join(args);
					}
				}
				
				String result = "[" + on(' ').skipNulls().join(beginning,argList) + "]\n";
				s.append(result);
			}
			
			//handle commands
			if (Iterables.size(ctx.subCommands) > 0)
				s.append("\nsub commands:\n");
			for(CommandRegistration command : ctx.subCommands)
				s.append(CommandRegistration.getCommandName.apply(command)).append("\n");
			
			return s.toString();
		}
	}
}

package org.dr1ftersoft.cliparsec;

import static java.lang.String.format;
import static java.util.Arrays.copyOf;
import static java.util.Collections.singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalOptionsParser
{
	public <T> T parse(T options, String... rawArgs) throws Exception
	{
		Class<?> clazz = options.getClass();
		Iterable<Field> fields = annotatedFields(clazz);

		ParsingCtx ctx = new ParsingCtx(rawArgs, fields);

		for (; ctx.hasNext();)
		{
			ctx.determineAndConsumeNextFields();
			ctx.setOrAppendToField(options);
		}

		return options;
	}

	/**
	 * determines and returns all fields from the given class that are annotated with the GlobalOptions annotation
	 * 
	 * @param clazz not <code>null</code>
	 * @return never <code>null</code>
	 */
	private Iterable<Field> annotatedFields(Class<?> clazz)
	{
		List<Field> annotatedFields = new ArrayList<Field>();
		for (Field f : clazz.getFields())
		{
			Annotation a = f.getAnnotation(GlobalOption.class);
			if (a == null)
				continue;
			annotatedFields.add(f);
		}
		return annotatedFields;
	}

	private static class ParsingCtx
	{
		private final Set<FieldRegistration>	allFields;

		private Iterable<FieldRegistration>		currentFields	= null;
		private String[]						args;
		private int								pos				= 0;

		public ParsingCtx(String[] args, Iterable<Field> fields)
		{
			this.args = args;
			this.allFields = new HashSet<FieldRegistration>();
			for (Field f : fields)
				this.allFields.add(new FieldRegistration(f));
		}

		public boolean hasNext()
		{
			return pos < args.length;
		}

		private String consume()
		{
			return args[pos++];
		}

		private Iterable<FieldRegistration> availableFields()
		{
			List<FieldRegistration> fields = new ArrayList<FieldRegistration>();
			for (FieldRegistration fr : allFields)
				if (fr.hasAllowedOccursLeft())
					fields.add(fr);
			return fields;
		}

		private Iterable<FieldRegistration> determineFields(String rawArg)
		{
			if (rawArg == null)
				throw new NullPointerException();

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
				Field f = fr.field;
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
			List<FieldRegistration> matchedRegistrations = new ArrayList<FieldRegistration>();
			// multiple short options may be specified - iterable over all chars:
			for (char shortOptionChar : argWithoutPrefix.toCharArray())
			{
				for (FieldRegistration fr : allFields)
				{
					Field f = fr.field;
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

				for (int i = 0; i < fieldRegistration.annotation.argCount(); i++)
				{
					String value = consume();

					Class<?> fieldType = field.getType();
					if (fieldType.isArray())
					{
						String[] originalValues = (String[]) field.get(options);
						if (originalValues == null)
							field.set(options, new String[] { value });
						else
						{
							String[] newValues = copyOf(originalValues,
									originalValues.length + 1);
							newValues[newValues.length - 1] = value;
							field.set(options, newValues);
						}
						return;
					}
					// TODO: also support Collections
					field.set(options, value);
				}
			}
		}

		private class FieldRegistration
		{
			public final Field			field;
			public final int			maxOccurs;
			public int					occurs;
			public final GlobalOption	annotation;

			public FieldRegistration(Field field)
			{
				this.field = field;
				this.annotation = field.getAnnotation(GlobalOption.class);
				this.maxOccurs = annotation.maxOccurs();
			}

			public boolean hasAllowedOccursLeft()
			{
				switch (maxOccurs)
				{
				case GlobalOption.MAX_OCCURS_DEFAULT_BEHAVIOUR:
					if (field.getType().isArray())
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
				case GlobalOption.MAX_OCCURS_DEFAULT_BEHAVIOUR:
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

				if (fr.annotation.shortOption() != GlobalOption.NOT_SET)
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
		}
	}
}

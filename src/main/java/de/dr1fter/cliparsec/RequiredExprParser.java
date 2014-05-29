package de.dr1fter.cliparsec;

import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import java.util.ArrayDeque;
import java.util.Deque;

import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.primitives.Chars;

import de.dr1fter.cliparsec.CliParserImpl.ParsingCtx;
import de.dr1fter.cliparsec.CliParserImpl.ParsingCtx.FieldRegistration;

@Beta
public class RequiredExprParser
{
	enum Symbol
	{
		TRUE, FALSE, NOT, AND, OR, OB, CB
	}

	private final ParsingCtx	ctx;

	public RequiredExprParser(ParsingCtx ctx)
	{
		this.ctx = ctx;
	}

	public boolean parse(String expr)
	{
		return new ParsingExec().parse(expr);
	}

	public boolean parse(FieldRegistration fr)
	{
		return parse(fr.annotation.required());
	}

	class ParsingExec
	{

		private char[]	expr;
		private int		pos		= 0;
		private int		depth	= 0;

		Deque<Symbol>	stack	= new ArrayDeque<Symbol>();

		private char next()
		{
			return expr[pos++];
		}

		private char peek()
		{
			return expr[pos];
		}

		private boolean hasNext()
		{
			return pos < expr.length;
		}

		public boolean parse(FieldRegistration registration)
		{
			return parse(registration.annotation.required());
		}

		public boolean parse(String expr)
		{
			if (Strings.isNullOrEmpty(expr.trim()))
				return false; // per definitionem, the empty string means "not required" (==false)
			this.expr = expr.toCharArray();

			for (; hasNext();)
			{
				char c = peek();
				switch (c)
				{
				case '(':
					stack.push(Symbol.OB);
					depth++;
					next();
					break;
				case ')':
					next();
					depth--;
					if (depth < 0)
						throw new RuntimeException("closed bracket w/o opening");
					evalBracketExpr();
					break;
				case 't':
					acceptTrue();
					stack.push(Symbol.TRUE);
					break;
				case 'f':
					acceptFalse();
					stack.push(Symbol.FALSE);
					break;
				case 'p':
					acceptPresent();
					break;
				case '&':
					stack.push(Symbol.AND);
					next();
					break;
				case '|':
					stack.push(Symbol.OR);
					next();
					break;
				case '!':
					stack.push(Symbol.NOT);
					next();
					break;
				case ' ':
				case '\t':
				case '\n':
					next();
					break;// whitespace is ignored
				default:
					throw new RuntimeException("unexpected token: " + c);
				}
			}
			stack=reverse(stack);
			stack = evalNegations(stack);
			Symbol r = evalStack(stack);
			stack.push(r);

			if (stack.size() != 1)
				throw new RuntimeException(
						"internal parsing error: stack size not as expected. Please report this as a bug.");
			requireTerminal(r);
			return r == Symbol.TRUE;// TODO: this will break if additional terminal symbols (other than true/false)
									// were to be introduced
		}

		private void evalBracketExpr()
		{
			Deque<Symbol> exprStack = new ArrayDeque<Symbol>();

			for (;;)
			{
				Symbol elem = stack.pop();
				Symbol pred = stack.peek();
				boolean endReached = pred == Symbol.OB;

				// if(! terminal(elem))
				// throw new RuntimeException("unexpected token: " + elem.toString());

				exprStack.push(elem);

				if (endReached)
				{
					stack.pop();// discard opening bracket
					break;
				}
				// TODO: handle missing opening bracket
			}

			// process negations			
			Deque<Symbol> evalStack = evalNegations(exprStack);

			// process elements from exprStack and push result back to stack
			Symbol result = evalStack(evalStack);
			stack.push(result);
		}

		private Deque<Symbol> reverse(Deque<Symbol> stack)
		{
			Deque<Symbol> rStack = new ArrayDeque<RequiredExprParser.Symbol>(newArrayList(stack.descendingIterator()));
			return rStack;
		}
		private Deque<Symbol> evalNegations(Deque<Symbol> lstack)
		{
			Deque<Symbol> evalStack = new ArrayDeque<Symbol>();
			boolean negate = false;
			for (; lstack.peek() != null;)
			{
				Symbol cs = lstack.pop();
				switch (cs)
				{
				case NOT:
					negate = !negate;
					break;
				case TRUE:
				case FALSE:
					cs = negate ? negate(cs) : cs;
					if (negate)
						negate = false;// reset negate flag
					evalStack.push(cs);
					break;
				default:
					evalStack.push(cs);
				}
			}
			return evalStack;
		}

		private Symbol evalStack(Deque<Symbol> lstack)
		{
			Symbol s = lstack.pop();
			requireTerminal(s);

			if (lstack.isEmpty()) // terminate
				return s;

			Symbol booleanOperator = lstack.pop();
			if (!booleanOperator(booleanOperator))
				throw new RuntimeException(
						"syntax error: expected boolean operand, but encountered: "
								+ booleanOperator);

			if (booleanOperator == Symbol.OR)
				return or(s, evalStack(lstack));

			// and has higher precedence than or
			// we may safely assume that the next symbol is again a terminal (true or false)
			// evaluate current symbol&next, push back to stack and continue w/ recursion
			lstack.push(and(s, lstack.pop()));
			return evalStack(lstack);
		}

		private Symbol requireTerminal(Symbol s)
		{
			if (!terminal(s))
				throw new RuntimeException(
						"syntax error: terminal symbol required, but encountered: "
								+ s);
			return s;
		}

		private boolean terminal(Symbol s)
		{
			return s == Symbol.TRUE || s == Symbol.FALSE;
		}

		private boolean booleanOperator(Symbol s)
		{
			return contains(
					newArrayList(new Symbol[] { Symbol.AND, Symbol.OR }), s);
		}

		private Symbol or(Symbol left, Symbol right)
		{
			requireTerminal(right);
			requireTerminal(left);
			return (left == Symbol.TRUE || right == Symbol.TRUE) ? Symbol.TRUE
					: Symbol.FALSE;
		}

		private Symbol and(Symbol left, Symbol right)
		{
			requireTerminal(right);
			requireTerminal(left);
			return (left == Symbol.TRUE && right == Symbol.TRUE) ? Symbol.TRUE
					: Symbol.FALSE;
		}

		private Symbol negate(Symbol s)
		{
			if (!terminal(s))
				throw new RuntimeException();
			return s == Symbol.TRUE ? Symbol.FALSE : Symbol.TRUE;
		}

		private void acceptChr(char c)
		{
			if (!(peek() == c))
				throw new RuntimeException(String.format(
						"expected %s but encountered %s", c, peek()));
			next();
		}

		private void acceptStr(String str)
		{
			for (char c : str.toCharArray())
				acceptChr(c);
		}

		private void skipWhitespace()
		{
			skipIfPresent(' ', '\t', '\n');
		}

		private void skipIfPresent(char... cs)
		{
			for (; hasNext();)
			{
				char c = peek();
				if (Chars.contains(cs, c))
					next();
				else
					break;
			}

		}

		private void acceptPresent()
		{
			acceptStr("present");
			skipWhitespace();
			acceptStr("(");
			StringBuilder s = new StringBuilder();
			for (; hasNext();)
			{
				s.append(next());
				if (peek() == ')')
				{
					next();// consume closing bracket
					evaluateDefined(s.toString());// TODO
					return;
				}
			}
			throw new RuntimeException(
					"no matching closing bracket for defined expression: "
							+ s.toString());
		}

		private void evaluateDefined(String field)
		{
			FieldRegistration fieldRegistration = ctx.fieldRegistration(field)
					.orNull();
			if (fieldRegistration == null)
				throw new RuntimeException(format(
						"no such field in args object: '%s'", field));

			stack.push(fieldRegistration.occurs > 0 ? Symbol.TRUE
					: Symbol.FALSE);
		}

		private void acceptTrue()
		{
			acceptStr("true");
		}

		private void acceptFalse()
		{
			acceptStr("false");
		}
	}

	static class Utils
	{
		static Predicate<FieldRegistration> required(
				final RequiredExprParser rep)
		{
			return new Predicate<CliParserImpl.ParsingCtx.FieldRegistration>()
			{
				@Override
				public boolean apply(FieldRegistration input)
				{
					return rep.parse(input);
				}
			};
		}
	}
}

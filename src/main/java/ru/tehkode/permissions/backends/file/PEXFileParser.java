package ru.tehkode.permissions.backends.file;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.support.StringBuilderVar;
import org.parboiled.support.ValueStack;
import org.parboiled.support.Var;

import java.util.Collections;
import java.util.LinkedList;

/**
 * Parser for PEX configuration format. This format is INI-like, except for the addition of qualifiers on the section headings
 */
//@BuildParseTree
public class PEXFileParser extends BaseParser<Object> {

	public Rule Document() {
		return FirstOf(EOI, Sequence(OneOrMore(Section(), Optional(LineBreak())), EOI, nodesToParent("root", Node.Type.ROOT)));
	}

	Rule Section() {
		Var<Object> sentinel = new Var<>();
		return Sequence(
				createSentinel(sentinel),
				CommentCapable(Heading()),
				OneOrMore(TestNot("["), Element()),
				nodesToParent(popString(), Node.Type.MAPPING, sentinel.get()),
				swap(),
				popSentinel(sentinel));
	}

	Rule Heading() {
		Var<Object> sentinel = new Var<>();
		return Sequence("[ ", StringUntil(AnyOf(" ]")).label("Heading key"),
				ZeroOrMore(
						TestNot("]"),
						createSentinel(sentinel),
						Mapping(),
						nodesToParent(popString(), Node.Type.QUALIFIER, sentinel.get()),
						swap(),
						popSentinel(sentinel)
				),
				"]");
	}

	Rule Element() {
		Var<Object> sentinel = new Var<>();
		return Sequence(createSentinel(sentinel), WhiteSpace(),
				FirstOf(
					Sequence(CommentCapable(Sequence(ListEntryStart(), String())), nodesToParent(popString(), Node.Type.SCALAR, sentinel.get())),
					Sequence(CommentCapable(Mapping()), nodesToParent(popString(), Node.Type.MAPPING, sentinel.get()))
				),
				swap(),
				popSentinel(sentinel)
		);
	}

	Rule Mapping() {
		return Sequence(StringUntil(MappingDelimeter()),
				MappingDelimeter(),
				StringUntil(FirstOf(AnyOf(" ]"), LineBreak())),
				push(new Node(popString(), Node.Type.SCALAR)),
				swap());
	}

	Rule String() {
		return StringUntil(FirstOf(LineBreak(), "#"));
	}

	Rule StringUntil(final Object untilMatch) {
		final StringBuilderVar ret = new StringBuilderVar();
		final StringBuilderVar heredocMarker = new StringBuilderVar();
		return Sequence(
				FirstOf(Sequence("\"", CharSequence("\"", ret), push(ret.getString()), "\""), // Double-quoted strings
						Sequence("'", CharSequence("'", ret), push(ret.getString()), "'"), // Single-quoted strings
						Sequence(
								"<<",
								CharSequence(LineBreak(), heredocMarker), // append heredoc marker to marker var
								push(heredocMarker.getString()) && heredocMarker.clearContents(), // stack: [marker], build: ""
								LineBreak(),
								ZeroOrMore(
										CharSequence(LineBreak(), heredocMarker), // build: current line
										LineBreak(),
										TestNot(heredocMarker.getString().trim().equals(pop())), // break off if this line is the heredoc marker
										ret.append(heredocMarker.getString()) && heredocMarker.clearContents(),
										heredocMarker.append("\n")
								),
								ACTION(pop() != null),
								push(ret.getString()),
								CharSequence(LineBreak(), null)), // collect heredoc marker on line
						Sequence(CharSequence(untilMatch, ret), push(ret.getString().trim()))),
				WhiteSpace());
	}

	Rule CharSequence(final Object charTest, final StringBuilderVar addVar) {
		return OneOrMore(FirstOf(Sequence("\\", CharacterEscape(addVar)), Sequence(TestNot(charTest), ANY, addVar == null || addVar.append(match()))));
	}

	Rule CharacterEscape(final StringBuilderVar addVar) {
		return FirstOf(Sequence("u", NTimes(4, CharRange('0', 'F')) , addVar == null || addVar.append((char) Integer.parseInt(match(), 16))), Sequence(ANY, addVar == null || addVar.append(match())));
	}

	Rule CommentCapable(Object rule) {
		return Sequence(Optional(Comment(), LineBreak()), rule, WhiteSpace(), Optional(Comment()), LineBreak());
	}

	Rule Comment() {
		return Sequence("# ", ZeroOrMore(TestNot(LineBreak()), ANY).suppressSubnodes(), matchOrDefault("").length() > 0 && push(new Node(match(), Node.Type.COMMENT)));
	}

	Rule LineBreak() {
		return FirstOf(OneOrMore(AnyOf("\r\n")), EOI);
	}

	Rule ListEntryStart() {
		return Sequence(AnyOf("-*+"), WhiteSpace());
	}

	Rule MappingDelimeter() {
		return FirstOf("= ", "-> ", ": ");
	}

	/* Copied from parboiled example code:
	https://github.com/sirthias/parboiled/blob/master/examples-java/src/main/java/org/parboiled/examples/calculators/CalculatorParser3.java */

	Rule WhiteSpace() {
		return ZeroOrMore(AnyOf(" \t\f"));
	}


	// we redefine the rule creation for string literals to automatically match trailing whitespace if the string
	// literal ends with a space character, this way we don't have to insert extra whitespace() rules after each
	// character or string literal

	@Override
	protected Rule fromStringLiteral(String string) {
		return string.endsWith(" ") && !string.equals(" ") ?
				Sequence(String(string.substring(0, string.length() - 1)), WhiteSpace()).label("Literal '" + string + "'") :
				String(string).label("Literal '" + string + "'");
	}
	/* End copied */

	// Utility methods to mess with value stack

	protected boolean nodesToParent(String key, Node.Type type) {
		return nodesToParent(key, type, null);
	}

	protected boolean nodesToParent(String key, Node.Type type, Object sentinel) {
		ValueStack<Object> stack = getContext().getValueStack();
		if (stack.isEmpty()) {
			return false;
		}
		LinkedList<Node> nodes = new LinkedList<>();
		while (!stack.isEmpty()) {
			Object o = stack.peek();
			if (sentinel != null && o == sentinel) {
				break;
			}
			if (!(o instanceof Node)) {
				throw new IllegalStateException("Unexpected object not of Node type: " + o);
			}

			nodes.add((Node) stack.pop());
		}
		Collections.reverse(nodes);
		push(new Node(key, type, nodes));
		return true;
	}

	protected String popString() {
		ValueStack<Object> valueStack = getContext().getValueStack();
		int i = 0;
		while (i < valueStack.size()) {
			if (valueStack.peek(i) instanceof String) {
				return (String) valueStack.pop(i);
			}
			i++;
		}
		throw new IllegalStateException("No string-typed objects contained in value stack");
	}

	protected boolean createSentinel(Var<Object> sentinel) {
		sentinel.set(new Object());
		return push(sentinel.get());
	}

	protected boolean popSentinel(Var<Object> sentinel) {
		if (peek() != sentinel.get()) {
			throw new IllegalStateException("Object on top of stack is not sentinel! Not all objects consumed");
		}
		pop();
		return true;
	}

	protected boolean log(String msg) {
		System.out.println(msg);
		return true;
	}

}

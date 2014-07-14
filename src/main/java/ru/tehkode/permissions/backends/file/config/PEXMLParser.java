package ru.tehkode.permissions.backends.file.config;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.support.StringBuilderVar;
import org.parboiled.support.ValueStack;
import org.parboiled.support.Var;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for PEX configuration format. This format is INI-like, except for the addition of qualifiers on the section headings and support for list values.
 */
public class PEXMLParser extends BaseParser<Object> {
	public Rule Document() {
		return FirstOf(EOI, Sequence(Optional(LineBreak()), OneOrMore(Section(), Optional(LineBreak())), ZeroOrMore(Comment(), LineBreak()), EOI, nodesToParent("root", Node.Type.ROOT)));
	}

	Rule Section() {
		Var<Object> sentinel = new Var<>();
		return Sequence(
				createSentinel(sentinel),
				CommentCapable(Heading()),
				ZeroOrMore(TestNot("["), Element()),
				nodesToParent(null, Node.Type.SECTION, sentinel.get()),
				swap(),
				popSentinel(sentinel));
	}

	Rule Heading() {
		Var<Object> sentinel = new Var<>();
		return Sequence("[ ", StringUntil(AnyOf(" ]")).label("Heading key"),
				ZeroOrMore(
						TestNot("]"),
						createSentinel(sentinel),
						Mapping(FirstOf(AnyOf(" ]"), LineBreak())),
						nodesToParent(null, Node.Type.QUALIFIER, sentinel.get()),
						swap(),
						popSentinel(sentinel)
				),
				"]");
	}

	Rule Element() {
		Var<Object> sentinel = new Var<>();
		return Sequence(createSentinel(sentinel), WhiteSpace(),
				FirstOf(
					Sequence(CommentCapable(Sequence(ListEntryStart(), String())), nodesToParent(null, Node.Type.SCALAR, sentinel.get())),
					Sequence(CommentCapable(Mapping(LineBreak())), nodesToParent(null, Node.Type.MAPPING, sentinel.get()))
				),
				swap(),
				popSentinel(sentinel)
		);
	}

	Rule Mapping(Rule ruleUntil) {
		return Sequence(StringUntil(MappingDelimeter()),
				MappingDelimeter(),
				FirstOf(StringUntil(ruleUntil), push("")),
				push(new Node((String) pop(), Node.Type.SCALAR)),
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
		return Sequence(ZeroOrMore(Comment(), LineBreak()), rule, WhiteSpace(), Optional(Comment()), LineBreak());
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
		return Sequence(FirstOf("=", "->", ":"), WhiteSpace());
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
		List<Node> nodes = new ArrayList<>(Math.max(stack.size() - 5, 1));
		while (!stack.isEmpty()) {
			Object o = stack.peek();
			if (sentinel != null && o == sentinel) {
				break;
			}
			if (o instanceof String && key == null) {
				key = (String) stack.pop();
			} else {
				if (!(o instanceof Node)) {
					throw new IllegalStateException("Unexpected object not of Node type: " + o);
				}
				nodes.add((Node) stack.pop());
			}
		}
		Collections.reverse(nodes);
		push(new Node(key, type, nodes));
		return true;
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

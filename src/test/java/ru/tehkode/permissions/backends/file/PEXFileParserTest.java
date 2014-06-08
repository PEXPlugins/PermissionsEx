package ru.tehkode.permissions.backends.file;

import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.ParsingException;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import static org.junit.Assert.*;

public class PEXFileParserTest {
	private PEXFileParser parser;

	@Before
	public void setUp() {
		parser = Parboiled.createParser(PEXFileParser.class);
	}

	// Basic test

	@Test
	public void testSimpleFile() {
		final String file = "[permissions group=Test user=zml2008] # A list of permissions that apply to the group test or the user zml2008\n" +
				"- \"pex.user.thing\"\n" +
				"# Commandbook teleporty\n" +
				"- commandbook.teleport.*\n" +
				"\n" +
				"[options group=admins] # A set of options for admins\n" +
				"prefix=<<EOF\n" +
				"Admin\n" +
				"Another Thing\n" +
				"EOF\n" +
				"\n" +
				"[inheritance user=zml2008]\n" +
				"- admins";
		Node ret = runParse(parser.Document(), file);

		Node test = new Node("root", Node.Type.ROOT,
				new Node("permissions", Node.Type.MAPPING,
						new Node("group", Node.Type.QUALIFIER,
								new Node("Test", Node.Type.SCALAR)
						),
						new Node("user", Node.Type.QUALIFIER,
								new Node("zml2008", Node.Type.SCALAR)
						),
						new Node("A list of permissions that apply to the group test or the user zml2008", Node.Type.COMMENT),
						new Node("pex.user.thing", Node.Type.SCALAR),
						new Node("commandbook.teleport.*", Node.Type.SCALAR,
								new Node("Commandbook teleporty", Node.Type.COMMENT)
						)
					),
				new Node("options", Node.Type.MAPPING,
						new Node("group", Node.Type.QUALIFIER,
								new Node("admins", Node.Type.SCALAR)
						),
						new Node("A set of options for admins", Node.Type.COMMENT),
						new Node("prefix", Node.Type.MAPPING,
								new Node("Admin\nAnother Thing", Node.Type.SCALAR)
						)
				),
				new Node("inheritance", Node.Type.MAPPING,
						new Node("user", Node.Type.QUALIFIER,
								new Node("zml2008", Node.Type.SCALAR)
						),
						new Node("admins", Node.Type.SCALAR)
				)
		);
		assertEquals(test, ret);
	}

	@Test
	public void testComments() {
		Node node = runParse(parser.Document(), "# Pre-mapping comment\n" +
				"[section] # After-section comment\n" +
				"- node # after-node\n" +
				"# before-node\n" +
				"- node2\n");

		Node expected = new Node("root", Node.Type.ROOT,
				new Node("section", Node.Type.MAPPING,
						new Node("Pre-mapping comment", Node.Type.COMMENT),
						new Node("After-section comment", Node.Type.COMMENT),
						new Node("node", Node.Type.SCALAR,
								new Node("after-node", Node.Type.COMMENT)
						),
						new Node("node2", Node.Type.SCALAR,
								new Node("before-node", Node.Type.COMMENT)
						)
				)
		);
		assertEquals(expected, node);
	}

	// Test list & mapping elements

	@Test
	public void testListStarters() {
		Node node = runParse(parser.Document(), "[varieditems]\n" +
				"- minus.start\n" +
				"* star-start\n" +
				"+ plus-start\n");

		Node expected = new Node("root", Node.Type.ROOT,
				new Node("varieditems", Node.Type.MAPPING,
						new Node("minus.start", Node.Type.SCALAR),
						new Node("star-start", Node.Type.SCALAR),
						new Node("plus-start", Node.Type.SCALAR)
				)
		);
		assertEquals(expected, node);
	}

	@Test
	public void testMapDelimeters() {
		Node node = runParse(parser.Document(), "[varieditems]\n" +
				"a=b\n" +
				"c->d\n" +
				"e: f\n");

		Node expected = new Node("root", Node.Type.ROOT,
				new Node("varieditems", Node.Type.MAPPING,
						new Node("a", Node.Type.MAPPING,
								new Node("b", Node.Type.SCALAR)
						),
						new Node("c", Node.Type.MAPPING,
								new Node("d", Node.Type.SCALAR)
						),
						new Node("e", Node.Type.MAPPING,
								new Node("f", Node.Type.SCALAR))
						)
		);
		assertEquals(expected, node);
	}

	@Test
	public void testSpacing() {
		Node node = runParse(parser.Document(), "[items a = b b= c c =d]\n" +
				"     - randomindent\n" +
				"-plaindash\n" +
				"-     lotsofspaces\n" +
				"-    \"quotedindent\"\n" +
				"- \"trailingquoted\"      \n" +
				"\n" +
				"[mappings]\n" +
				"     severely=indented");

		Node expected = new Node("root", Node.Type.ROOT,
				new Node("items", Node.Type.MAPPING,
						new Node("a", Node.Type.QUALIFIER,
								new Node("b", Node.Type.SCALAR)
						),
						new Node("b", Node.Type.QUALIFIER,
								new Node("c", Node.Type.SCALAR)
						),
						new Node("c", Node.Type.QUALIFIER,
								new Node("d", Node.Type.SCALAR)
						),
						new Node("randomindent", Node.Type.SCALAR),
						new Node("plaindash", Node.Type.SCALAR),
						new Node("lotsofspaces", Node.Type.SCALAR),
						new Node("quotedindent", Node.Type.SCALAR),
						new Node("trailingquoted", Node.Type.SCALAR)
				),
				new Node("mappings", Node.Type.MAPPING,
						new Node("severely", Node.Type.MAPPING,
								new Node("indented", Node.Type.SCALAR)
						)
				)
		);
		assertEquals(expected, node);
	}

	// Test odd conditions
	@Test
	public void testEmptyFile() {
		runParse(parser.Document(), "");
	}

	@Test(expected = ParsingException.class)
	public void testUnclosedHeader() {
		runParse(parser.Document(), "[permissions");
	}

	@Test(expected = ParsingException.class)
	public void testUnclosedHeader2() {
		runParse(parser.Document(), "[permissions a=b b=");
	}

	@Test(expected = ParsingException.class)
	public void testUnclosedHeader3() {
		runParse(parser.Document(), "[permissions a=b b=c]\n" +
				"- a\n" +
				"-b \n" +
				"\n" +
				"[options");
	}

	@Test(expected = ParsingException.class)
	public void testEmptySection() {
		runParse(parser.Document(), "[permissions]");
	}

	@Test(expected = ParsingException.class)
	public void testUnmatchedMapping() {
		runParse(parser.Document(), "[permissions]\n" +
				"a=");
	}

	// String parsing tests

	@Test
	public void testSingleString() {
		String ret = runParse(parser.String(), "This is a plain string");
		assertEquals("This is a plain string", ret);

		ret = runParse(parser.String(), "A string with a \\u00A7 unicode escape");
		assertEquals("A string with a \u00A7 unicode escape", ret);

		ret  = runParse(parser.String(), "Strings with newlines\nare terminated at newlines");
		assertEquals("Strings with newlines", ret);
	}

	@Test
	public void testQuotedString() {
		String ret = runParse(parser.String(), "\"This is a \\\"quoted string\"");
		assertEquals("This is a \"quoted string", ret);

		ret = runParse(parser.String(), "\'A single-quoted string\'");
		assertEquals("A single-quoted string", ret);

		ret = runParse(parser.String(), "\"A quoted string with a newline.\nPretty cool, right?\"");
		assertEquals("A quoted string with a newline.\nPretty cool, right?", ret);
	}

	@Test
	public void testHeredoc() {
		String ret = runParse(parser.String(), "<<EOF\n" +
				"This is\n" +
				"a heredoc\n" +
				"EOF\n");
		assertEquals("This is\na heredoc", ret);

		ret = runParse(parser.String(), "<<EOF\n" +
				"EOF"); // Empty heredoc
		assertEquals("", ret);
	}

	private <T> T runParse(Rule rule, String input) {
		ParseRunner<T> runner = new ReportingParseRunner<>(rule);
		ParsingResult<T> res = runner.run(input);
		if (res.hasErrors()) {
			throw new ParsingException(ErrorUtils.printParseErrors(res));
		}
		return res.resultValue;
	}

}

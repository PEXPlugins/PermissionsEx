package ru.tehkode.permissions.backends.file.config;

import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.ParsingException;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * Tests for the PEXML writer.
 * Mostly focused on ensuring that roundtripping works fine.
 */
public class PEXMLWriterTest {
	@Test
	public void testSimple() throws IOException {
		final StringWriter raw = new StringWriter();
		final PEXMLWriter subject = new PEXMLWriter(raw, new WriterOptions());

		subject.beginHeader("test")
				.writeQualifier("a", "b")
				.endHeader()
				.writeListEntry("list1")
				.writeListEntry("list2");

		subject.beginHeader("test2")
				.endHeader()
				.writeComment("testComment")
				.writeMapping("key", "value");

		subject.close();

		final Node parsed = runParse(Parboiled.createParser(PEXMLParser.class).Document(), raw.toString());

		final Node expected = new Node("root", Node.Type.ROOT,
				new Node("test", Node.Type.SECTION,
						new Node("a", Node.Type.QUALIFIER,
								new Node("b", Node.Type.SCALAR)
						),
						new Node("list1", Node.Type.SCALAR),
						new Node("list2", Node.Type.SCALAR)
				),
				new Node("test2", Node.Type.SECTION,
						new Node("key", Node.Type.MAPPING,
								new Node("testComment", Node.Type.COMMENT),
								new Node("value", Node.Type.SCALAR)
						)
				)
		);

		assertEquals(expected, parsed);

	}

	@Test
	public void testStringWriting() throws IOException {
		final StringWriter writer = new StringWriter();
		PEXMLWriter pexml = new PEXMLWriter(writer, new WriterOptions());
		pexml.writeString("#startswithcomment");
		assertEquals("\"#startswithcomment\"", getAndReset(writer));

		pexml.writeString("This is a \"string\' with a lot of quotes");
		assertEquals("<<EOF_0\nThis is a \"string' with a lot of quotes\nEOF_0", getAndReset(writer));
	}

	private String getAndReset(StringWriter writer) {
		String ret = writer.toString();
		writer.getBuffer().delete(0, writer.getBuffer().length());
		return ret;
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

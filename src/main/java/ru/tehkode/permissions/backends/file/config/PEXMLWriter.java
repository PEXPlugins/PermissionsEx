package ru.tehkode.permissions.backends.file.config;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**
 * Writer for the PEXML format
 */
public class PEXMLWriter implements Closeable {
	private final Writer writer;
	private final WriterOptions options;
	private WriterState state = WriterState.GENERAL;
	private int heredocCounter = 0;

	private enum WriterState {
		GENERAL,
		HEADER
	}

	public PEXMLWriter(Writer writer, WriterOptions options) {
		this.writer = writer;
		this.options = options;
	}


	public PEXMLWriter writeComment(String comment) throws IOException {
		checkState(WriterState.GENERAL);
		for (String line : comment.split("[\r\n]")) {
			writer.write("# ");
			writer.write(line);
			writeNewline();
		}
		return this;
	}

	public PEXMLWriter beginHeader(String name) throws IOException {
		checkState(WriterState.GENERAL);
		setState(WriterState.HEADER);
		writeNewline();
		writer.write('[');
		writeString(name);
		return this;
	}

	public PEXMLWriter endHeader() throws IOException {
		checkState(WriterState.HEADER);
		writer.write("]");
		writeNewline();
		setState(WriterState.GENERAL);
		return this;
	}

	public PEXMLWriter writeQualifier(String key, String value) throws IOException {
		checkState(WriterState.HEADER);
		writer.write(' ');
		writeMapping0(key, value);
		return this;
	}

	public PEXMLWriter writeMapping(String key, String value) throws IOException {
		checkState(WriterState.GENERAL);
		writeMapping0(key, value);
		writeNewline();
		return this;
	}


	public PEXMLWriter writeListEntry(String value) throws IOException {
		checkState(WriterState.GENERAL);
		writer.write(options.getListFormat().getStartChar());
		writer.write(' ');
		writeString(value);
		writeNewline();
		return this;
	}

	private void writeMapping0(String key, String value) throws IOException {
		writeString(key);
		writer.write(options.getMappingFormat().getDivider());
		writeString(value);
	}

	private void checkState(WriterState state) throws IOException {
		if (this.state != state) {
			throw new IOException("Invalid state! Expected " + state + " but was " + this.state);
		}
	}

	private void setState(WriterState state) {
		this.state = state;
	}

	private void writeNewline() throws IOException {
		writer.write('\n');
	}

	protected PEXMLWriter writeString(String string) throws IOException {
		WriterOptions.StringType type = options.getStringType(string);

		switch (type) {
			case PLAIN:
				writer.write(escapedString(string, null));
				break;
			case SINGLE_QUOTED:
				writer.write("'");
				writer.write(escapedString(string, "'"));
				writer.write("'");
				break;
			case DOUBLE_QUOTED:
				writer.write("\"");
				writer.write(escapedString(string, "\""));
				writer.write("\"");
				break;
			case HEREDOC:
				String heredocKey = "EOF_" + heredocCounter++;
				writer.write("<<" + heredocKey);
				writeNewline();
				writer.write(string);
				writeNewline();
				writer.write(heredocKey);
				break;
			default:
				throw new IOException("Unknown string type: " + type);
		}
		return this;
	}

	private String escapedString(String input, String quoteChar) {
		if (quoteChar == null) {
			return WriterOptions.SPECIAL_CHARACTERS.matcher(input).replaceAll("\\\\$1");
		} else {
			return input.replace(quoteChar, "\\" + quoteChar);
		}
	}

	public void close() throws IOException {
		writer.close();
	}

	public void flush() throws IOException {
		writer.flush();
	}
}

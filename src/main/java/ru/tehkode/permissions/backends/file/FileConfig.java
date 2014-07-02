package ru.tehkode.permissions.backends.file;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import ru.tehkode.permissions.backends.file.config.Node;
import ru.tehkode.permissions.backends.file.config.PEXMLParser;
import ru.tehkode.permissions.backends.file.config.PEXMLWriter;
import ru.tehkode.permissions.backends.file.config.WriterOptions;
import ru.tehkode.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public class FileConfig {
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private final File file, tempFile, oldFile;
	private boolean saveSuppressed;
	private final ThreadLocal<PEXMLParser> parser;
	private final Object saveLock = new Object();

	public FileConfig(File file) {
		this.file = file;
		this.tempFile = new File(file.getPath() + ".tmp");
		this.oldFile = new File(file.getPath() + ".old");

		final PEXMLParser localParser = Parboiled.createParser(PEXMLParser.class);
		parser = new ThreadLocal<PEXMLParser>() {
			@Override
			protected PEXMLParser initialValue() {
				return localParser.newInstance();
			}
		};
	}

	private PEXMLParser getParser() {
		return parser.get();
	}

	public File getFile() {
		return file;
	}

	public FileMatcherList load() throws IOException {
		PEXMLParser parser = getParser();
		ParseRunner<Node> runner = new ReportingParseRunner<>(parser.Document());

		if (!getFile().exists()) {
			throw new FileNotFoundException(getFile().toString());
		}

		String data = StringUtils.readStream(new FileInputStream(getFile()));

		ParsingResult<Node> result = runner.run(data);

		if (result.hasErrors()) {
			throw new IOException(ErrorUtils.printParseErrors(result));
		}

		if (result.resultValue == null) {
			throw new IOException("No result value for parser!");
		}

		return new FileMatcherList(result.resultValue.getChildren(), this);
	}

	/**
	 * Saves a list of matcher groups to configuration.
	 *
	 * @param list The list of matchers to save.
	 * @throws IOException if saving is unsuccessful
	 */
	public void save(FileMatcherList list) throws IOException {
		if (saveSuppressed) {
			return;
		}

		Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), UTF8_CHARSET);

		PEXMLWriter configWriter = new PEXMLWriter(writer, new WriterOptions());

		list.save(configWriter);

		configWriter.close();

		synchronized (saveLock) {
			oldFile.delete();
			boolean fileExists = file.exists();
			if (!fileExists || file.renameTo(oldFile)) {
				if (!tempFile.renameTo(file)) {
					throw new IOException("Unable to overwrite config with temporary file! New config is at " + tempFile + ", old config at" + oldFile);
				} else {
					if (fileExists && !oldFile.delete()) {
						throw new IOException("Unable to delete old file " + oldFile);
					}
				}
			}
		}
	}

	public boolean isSaveSuppressed() {
		return saveSuppressed;
	}

	public void setSaveSuppressed(boolean saveSuppressed) {
		this.saveSuppressed = saveSuppressed;
	}
}

package ru.tehkode.permissions.backends.file;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import ru.tehkode.permissions.backends.file.config.Node;
import ru.tehkode.permissions.backends.file.config.PEXMLParser;
import ru.tehkode.permissions.backends.file.config.PEXMLWriter;
import ru.tehkode.permissions.backends.file.config.WriterOptions;
import ru.tehkode.permissions.backends.memory.ConfigInstance;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FileConfig {
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private final File file, tempFile, oldFile;
	private boolean saveSuppressed;
	private final ThreadLocal<PEXMLParser> parser;
	private final Object saveLock = new Object();
	private final FileBackend backend;

	public FileConfig(FileBackend backend, File file) {
		this.file = file;
		this.tempFile = new File(file.getPath() + ".tmp");
		this.oldFile = new File(file.getPath() + ".old");
		this.backend = backend;

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

	public FileConfigInstance load() throws IOException {
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

		List<Node> children;
		if (result.resultValue == null) {
			children = Collections.emptyList();
		} else {
			children = result.resultValue.getChildren();
		}

		return groupsFromNodes(children);
	}

	private FileConfigInstance groupsFromNodes(List<Node> children) throws IOException{
		final List<FileMatcherGroup> ret = new LinkedList<>();
		final List<String> globalComments = new ArrayList<>();
		for (Node node : children) {
			switch (node.getType()) {
				case SECTION:
					Multimap<Qualifier, String> qualifiers = HashMultimap.create();
					Map<String, String> entries = new HashMap<>();
					List<String> entriesList = new LinkedList<>();
					List<String> comments = new LinkedList<>();
					Multimap<String, String> entryComments = HashMultimap.create();

					for (Node child : node.getChildren()) {
						switch (child.getType()) {
							case QUALIFIER:
								for (Node value : child.getChildren()) {
									switch (value.getType()) {
										case SCALAR:
											qualifiers.put(Qualifier.fromString(child.getKey()), value.getKey());
											break;
										default:
											throw new IOException("Node " + value + " is not supported as a qualifier child");
									}
								}
								break;
							case MAPPING:
								for (Node value : child.getChildren()) {
									switch (value.getType()) {
										case SCALAR:
											entries.put(child.getKey(), value.getKey());
											break;
										case COMMENT:
											entryComments.put(child.getKey(), value.getKey());
											break;
										default:
											throw new IOException("Node " + value + " is not supported as a mapping child");
									}
								}
								break;
							case SCALAR:
								entriesList.add(child.getKey());
								break;
							case COMMENT:
								comments.add(child.getKey());
								break;
							default:
								throw new IOException("Node " + child + " is not supported as a child of a section");
						}
					}

					final FileMatcherGroup group;
					if (entries.isEmpty()) {
						group = new FileMatcherGroup(node.getKey(), backend, qualifiers, entriesList, comments, entryComments);
					} else {
						group = new FileMatcherGroup(node.getKey(), backend, qualifiers, entries, comments, entryComments);
					}
					ret.add(group);
					break;
				case COMMENT:
					globalComments.add(node.getKey());
					break;
				default:
					throw new IOException("Node " + node + " is not supported as a child of the root");
			}
		}
		return new FileConfigInstance(ret, globalComments);
	}

	private static class FileConfigInstance implements ConfigInstance<FileMatcherGroup> {
		private final List<String> comments;
		private Collection<FileMatcherGroup> groups;

		public FileConfigInstance(Collection<FileMatcherGroup> groups, List<String> comments) {
			this.groups = groups;
			this.comments = comments;
		}

		@Override
		public Collection<FileMatcherGroup> getGroups() {
			return groups;
		}

		@Override
		public void setGroups(Collection<FileMatcherGroup> groups) {
			this.groups = groups;
		}
	}

	/**
	 * Saves a list of matcher groups to configuration.
	 *
	 * @param list The list of matchers to save.
	 * @throws IOException if saving is unsuccessful
	 */
	public void save(ConfigInstance<FileMatcherGroup> list) throws IOException {
		if (saveSuppressed) {
			return;
		}

		Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), UTF8_CHARSET);

		PEXMLWriter configWriter = new PEXMLWriter(writer, new WriterOptions());

		writeGroups(list, configWriter);

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

	private void writeGroups(ConfigInstance<FileMatcherGroup> list, PEXMLWriter writer) throws IOException {
		for (FileMatcherGroup group : list.getGroups()) {
			List<String> comments = group.getComments();
			if (comments != null) {
				for (String comment : comments) {
					writer.writeComment(comment);
				}
			}
			writer.beginHeader(group.getName());
			for (Map.Entry<Qualifier, String> qual : group.getQualifiers().entries()) {
				writer.writeQualifier(qual.getKey().getName(), qual.getValue());
			}
			writer.endHeader();

			if (group.isMap()) {
				for (Map.Entry<String, String> ent : group.getEntries().entrySet()) {
					writeEntryComments(group, ent.getKey(), writer);
					writer.writeMapping(ent.getKey(), ent.getValue());
				}

			} else if (group.isList()) {
				for (String entry : group.getEntriesList()) {
					writeEntryComments(group, entry, writer);
					writer.writeListEntry(entry);
				}
			}
		}

		if (list instanceof FileConfigInstance) {
			FileConfigInstance inst = (FileConfigInstance) list;
			if (inst.comments != null) {
				for (String comment : inst.comments) {
					writer.writeComment(comment);
				}
			}
		}
	}

	private void writeEntryComments(FileMatcherGroup group, String entry, PEXMLWriter writer) throws IOException {
		if (group.getEntryComments() != null) {
			Collection<String> entryComments = group.getEntryComments().get(entry);
			if (entryComments != null && !entryComments.isEmpty()) {
				for (String comment : entryComments) {
					writer.writeComment(comment);
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

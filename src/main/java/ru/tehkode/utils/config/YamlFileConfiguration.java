package ru.tehkode.utils.config;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

public class YamlFileConfiguration extends SimpleHierarchicalFileConfiguration {

	public final static int DEFAULT_IDENT = 4;
	private final DumperOptions yamlOptions = new DumperOptions();
	private final Representer yamlRepresenter = new YamlRepresenter();
	private final Yaml yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions);
	private int ident = DEFAULT_IDENT;

	public YamlFileConfiguration() {
		initialize();
	}

	public YamlFileConfiguration(HierarchicalConfiguration c) {
		super(c);
		initialize();
	}

	public YamlFileConfiguration(String fileName) throws ConfigurationException {
		super(fileName);
		initialize();
	}

	public YamlFileConfiguration(File file) throws ConfigurationException {
		super(file);
		initialize();
	}

	public YamlFileConfiguration(URL url) throws ConfigurationException {
		super(url);
		initialize();
	}

	private void initialize() {
		yamlOptions.setIndent(this.ident);
		yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
	}

	@Override
	public void load(Reader in) throws ConfigurationException {
		try {
			this.loadHierarchy(this.getRootNode(), yaml.load(in));
		} catch (Throwable e) {
			throw new ConfigurationException("Failed to load configuration: " + e.getMessage(), e);
		}
	}

	@Override
	public void save(Writer out) throws ConfigurationException {
		try {
			yaml.dump(this.saveHierarchy(this.getRootNode()), out);
		} catch (Throwable e) {
			throw new ConfigurationException("Failed to save configuration: " + e.getMessage(), e);
		}
	}
}

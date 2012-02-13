package ru.tehkode.utils.config;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;


public class JSONFileConfiguration extends SimpleHierarchicalFileConfiguration {

	private final JSONParser parser = new JSONParser();
	
	public JSONFileConfiguration(HierarchicalConfiguration c) {
		super(c);
	}

	public JSONFileConfiguration(String fileName) throws ConfigurationException {
		super(fileName);
	}

	public JSONFileConfiguration(File file) throws ConfigurationException {
		super(file);
	}

	public JSONFileConfiguration(URL url) throws ConfigurationException {
		super(url);
	}

	@Override
	public void load(Reader in) throws ConfigurationException {
		try {
			this.loadHierarchy(this.getRootNode(), parser.parse(in));
			parser.reset();
		} catch (Throwable e) {
			throw new ConfigurationException("Failed to load configuration: " + e.getMessage(), e);
		}
	}

	@Override
	public void save(Writer out) throws ConfigurationException {
		try {
			JSONValue.writeJSONString(this.saveHierarchy(this.getRootNode()), out);
		} catch (Throwable e) {
			throw new ConfigurationException("Failed to save configuration: " + e.getMessage(), e);
		}
	}
	
	
	

}

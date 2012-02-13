package ru.tehkode.utils.config;

import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class ConfigurationFactory {

	public static HierarchicalConfiguration loadFile(File file) throws ConfigurationException {
		String fileName = file.getName();
		String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

		if ("yml".equals(fileExtension)) {
			return new YamlFileConfiguration(file);
		} else if ("json".equals(fileExtension)) {
			return new JSONFileConfiguration(file);
		} else if ("xml".equals(fileExtension)) {
			return new XMLConfiguration(file);
		}

		throw new IllegalArgumentException("File extensions \"" + fileExtension + "\" is not detected or not supported");
	}
}

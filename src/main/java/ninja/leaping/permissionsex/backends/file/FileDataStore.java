package ninja.leaping.permissionsex.backends.file;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.backends.DataStore;
import ninja.leaping.permissionsex.backends.DataStoreFactory;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.sponge.option.OptionSubjectData;

import java.io.File;
import java.io.IOException;


public class FileDataStore implements DataStore {
    private static final ObjectMapper<FileDataStore> MAPPER;

    static {
        try {
            MAPPER = ObjectMapper.mapperForClass(FileDataStore.class);
        } catch (ObjectMappingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final String identifier;
    @Setting("file")
    private String file;
    private ConfigurationLoader permissionsFileLoader;
    private ConfigurationNode permissionsConfig;

    public FileDataStore(String identifier) {
        this.identifier = identifier;
    }

    private void initialize() throws PermissionsLoadingException {
        File permissionsFile = new File(file); // TODO: Set proper configuration directory based on what the plugin provides
        if (file.endsWith(".yml")) {
            ConfigurationLoader<ConfigurationNode> yamlLoader = YAMLConfigurationLoader.builder().setFile(permissionsFile).build();
            file = file.replace(".yml", ".conf");
            permissionsFile = new File(file);
            permissionsFileLoader = HoconConfigurationLoader.builder().setFile(permissionsFile).build();
            try {
                permissionsConfig = yamlLoader.load();
                permissionsFileLoader.save(permissionsConfig);
            } catch (IOException e) {
                throw new PermissionsLoadingException("While loading legacy YML permissions from " + permissionsFile, e);
            }
        } else {
            permissionsFileLoader = HoconConfigurationLoader.builder().setFile(permissionsFile).build();
        }

        try {
            permissionsConfig = permissionsFileLoader.load();
        } catch (IOException e) {
            throw new PermissionsLoadingException("While loading permissions file from " + permissionsFile, e);
        }

    }

    private void save() throws PermissionsLoadingException {
        try {
            permissionsFileLoader.save(permissionsConfig);
        } catch (IOException e) {
            throw new PermissionsLoadingException("While saving permissions file to " + file, e);
        }
    }

    @Override
    public OptionSubjectData getData(String type, String identifier, Caching listener) {
        return null;
    }

    @Override
    public boolean isRegistered(String type, String identifier) {
        return false;
    }

    @Override
    public Iterable<OptionSubjectData> getAll(String type) {
        return null;
    }

    @Override
    public String getTypeName() {
        return null;
    }

    @Override
    public String serialize(ConfigurationNode node) throws PermissionsLoadingException {
        try {
            MAPPER.serializeObject(this, node);
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException("Error while serializing backend " + identifier, e);
        }
        return "file";
    }

    public static class Factory implements DataStoreFactory {
        @Override
        public DataStore createDataStore(String identifier, ConfigurationNode config) throws PermissionsLoadingException {
            FileDataStore store = new FileDataStore(identifier);
            try {
                MAPPER.populateObject(store, config);
            } catch (ObjectMappingException e) {
                throw new PermissionsLoadingException("Error while deserializing backend " + identifier, e);
            }
            return store;
        }
    }

}

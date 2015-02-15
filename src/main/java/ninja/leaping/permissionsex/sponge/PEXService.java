package ninja.leaping.permissionsex.sponge;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import ninja.leaping.permissionsex.backends.DataStore;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.ContextCalculator;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles core management stuff
 */
public class PEXService implements PermissionService {
    private final DataStore dataStore;
    private final List<ContextCalculator> contextCalculators = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, SubjectCollection> subjectCollections = new ConcurrentHashMap<>();
    private SubjectCollection users, groups;

    @Inject
    public PEXService(DataStore store) {
        this.dataStore = store;
    }


    @Override
    public SubjectCollection getUserSubjects() {
        return users;
    }

    @Override
    public SubjectCollection getGroupSubjects() {
        return groups;
    }

    @Override
    public SubjectData getDefaultData() {
        return null;
    }

    @Override
    public Optional<SubjectCollection> getSubjects(String identifier) {
        return Optional.fromNullable(subjectCollections.get(identifier));
    }

    @Override
    public Map<String, SubjectCollection> getKnownSubjects() {
        return ImmutableMap.copyOf(subjectCollections);
    }

    @Override
    public void registerContextCalculator(ContextCalculator calculator) {
        contextCalculators.add(calculator);
    }

    List<ContextCalculator> getContextCalculators() {
        return contextCalculators;
    }

    public void close() {

    }
}

package ninja.leaping.permissionsex.sponge;

import ninja.leaping.permissionsex.backends.DataStore;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Subject collection
 */
public class PEXSubjectCollection implements SubjectCollection {
    private final DataStore data;
    private final SubjectCollection parents;
    private final String type;

    public PEXSubjectCollection(DataStore data, SubjectCollection parents, String type) {
        this.data = data;
        this.parents = parents == null ? this : parents;
        this.type = type;
    }

    @Override
    public String getIdentifier() {
        return this.type;
    }

    @Override
    public Subject get(String identifier) {
        return new PermissionsExSubject(identifier, data.getData(type, identifier, null), null);
    }

    @Override
    public boolean hasRegistered(String identifier) {
        return data.isRegistered(type, identifier);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return Collections.emptyList(); //data.getAll(type);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(String permission) {
        return null;
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(Set<Context> contexts, String permission) {
        return null;
    }
}

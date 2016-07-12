package ninja.leaping.permissionsex.backend.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.Tristate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ninja.leaping.permissionsex.util.Util.updateImmutable;

/**
 * In-memory data segment, with {@link ninja.leaping.configurate.objectmapping.ObjectMapper} annotations.
 */
@ConfigSerializable
public class MemorySegment implements DataSegment {
    @Setting("contexts")
    private Map<String, String> rawContexts;
    private Set<Map.Entry<String, String>> contexts;
    @Nullable
    @Setting
    private Map<String, Tristate> permissions;
    @Nullable
    @Setting
    private Map<String, String> options;
    @Nullable
    @Setting
    private List<SubjectRef> parents;
    @Nullable
    @Setting("permissions-default")
    private Tristate defaultValue;
    @Nullable
    @Setting
    private Boolean inheritable;
    @Nullable
    @Setting
    private Integer weight;

    private MemorySegment(Set<Map.Entry<String, String>> contexts, @Nullable Integer weight, @Nullable Boolean inheritable, @Nullable Map<String, Tristate> permissions, @Nullable Map<String, String> options, @Nullable List<SubjectRef> parents, @Nullable Tristate defaultValue) {
        this.contexts = ImmutableSet.copyOf(contexts);
        this.weight = weight;
        this.inheritable = inheritable;
        this.permissions = permissions;
        this.options = options;
        this.parents = parents;
        this.defaultValue = defaultValue;
    }

    private MemorySegment() { // Objectmapper constructor
    }

    static MemorySegment fromSegment(DataSegment seg) {
        if (seg instanceof MemorySegment) {
            return (MemorySegment) seg;
        } else {
            return new MemorySegment(seg.getContexts(), seg.getWeight() == DataSegment.DEFAULT_WEIGHT ? null : seg.getWeight(), seg.isInheritable() == DataSegment.DEFAULT_INHERITABILITY ? null : seg.isInheritable(),
                    seg.getPermissions(), seg.getOptions(), seg.getParents(), seg.getPermissionDefault());
        }
    }

    MemorySegment(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable) {
        this.contexts = ImmutableSet.copyOf(contexts);
        this.weight = weight == 0 ? null : weight;
        this.inheritable = inheritable ? null : false;
    }

    @Override
    public Set<Map.Entry<String, String>> getContexts() {
        if (this.contexts == null && this.rawContexts != null) {
            this.contexts = ImmutableSet.copyOf(this.rawContexts.entrySet());
        } else if (this.contexts == null) {
            this.contexts = ImmutableSet.of();
        }
        return this.contexts;
    }

    @Override
    public DataSegment withContexts(Set<Map.Entry<String, String>> contexts) {
        return null;
    }

    @Override
    public int getWeight() {
        return this.weight == null ? DataSegment.DEFAULT_WEIGHT : this.weight;
    }

    @Override
    public DataSegment withWeight(int weight) {
        return new MemorySegment(contexts, weight == DataSegment.DEFAULT_WEIGHT ? null : weight, inheritable,
                permissions, options, parents, defaultValue);
    }

    @Override
    public boolean isInheritable() {
        return this.inheritable == null ? DataSegment.DEFAULT_INHERITABILITY : this.inheritable;
    }

    @Override
    public DataSegment withInheritability(boolean inheritable) {
        return new MemorySegment(contexts, weight, inheritable == DataSegment.DEFAULT_INHERITABILITY ? null : inheritable,
                permissions, options, parents, defaultValue);
    }

    @Override
    public Map<String, Tristate> getPermissions() {
        return this.permissions == null ? ImmutableMap.of() : this.permissions;
    }

    @Override
    public Map<String, String> getOptions() {
        return this.options == null ? ImmutableMap.of() : this.options;
    }

    @Override
    public List<SubjectRef> getParents() {
        return this.parents == null ? ImmutableList.of() : this.parents;
    }

    @Override
    public Tristate getPermissionDefault() {
        return this.defaultValue;
    }

    @Override
    public MemorySegment withOption(String key, String value) {
        return new MemorySegment(contexts, weight, inheritable,
                permissions, updateImmutable(options, key, value), parents, defaultValue);
    }

    @Override
    public MemorySegment withoutOption(String key) {
        if (options == null || !options.containsKey(key)) {
            return this;
        }

        Map<String, String> newOptions = new HashMap<>(options);
        newOptions.remove(key);
        return new MemorySegment(contexts, weight, inheritable,
                permissions, newOptions, parents, defaultValue);

    }

    @Override
    public MemorySegment withOptions(Map<String, String> values) {
        return new MemorySegment(contexts, weight, inheritable,
                permissions, values == null ? null : ImmutableMap.copyOf(values), parents, defaultValue);
    }

    @Override
    public MemorySegment withoutOptions() {
        return new MemorySegment(contexts, weight, inheritable,
                permissions, null, parents, defaultValue);
    }

    @Override
    public MemorySegment withPermission(String permission, Tristate value) {
        return new MemorySegment(contexts, weight, inheritable,
                updateImmutable(permissions, permission, value), options, parents, defaultValue);
    }

    @Override
    public MemorySegment withoutPermission(String permission) {
        if (permissions == null || !permissions.containsKey(permission)) {
            return this;
        }

        Map<String, Tristate> newPermissions = new HashMap<>(permissions);
        newPermissions.remove(permission);
        return new MemorySegment(contexts, weight, inheritable,
                newPermissions, options, parents, defaultValue);
    }

    @Override
    public MemorySegment withPermissions(Map<String, Tristate> values) {
        return new MemorySegment(contexts, weight, inheritable,
                ImmutableMap.copyOf(values), options, parents, defaultValue);
    }

    @Override
    public MemorySegment withoutPermissions() {
        return new MemorySegment(contexts, weight, inheritable,
                null, options, parents, defaultValue);
    }

    @Override
    public MemorySegment withDefaultValue(Tristate defaultValue) {
        return new MemorySegment(contexts, weight, inheritable,
                permissions, options, parents, defaultValue);
    }

    @Override
    public MemorySegment withAddedParent(SubjectRef parent) {
        ImmutableList.Builder<SubjectRef> parents = ImmutableList.builder();
        parents.add(parent);
        if (this.parents != null) {
            parents.addAll(this.parents);
        }
        return new MemorySegment(contexts, weight, inheritable,
                permissions, options, parents.build(), defaultValue);
    }

    @Override
    public MemorySegment withRemovedParent(SubjectRef parent) {
        if (this.parents == null || this.parents.isEmpty()) {
            return this;
        }

        final List<SubjectRef> newParents = new ArrayList<>(parents);
        newParents.remove(parent);
        return new MemorySegment(contexts, weight, inheritable,
                permissions, options, newParents, defaultValue);
    }

    @Override
    public MemorySegment withParents(List<SubjectRef> parents) {
        return new MemorySegment(contexts, weight, inheritable,
                permissions, options, parents == null ? null : ImmutableList.copyOf(parents), defaultValue);
    }

    @Override
    public MemorySegment withoutParents() {
        return new MemorySegment(contexts, weight, inheritable,
                permissions, options, null, defaultValue);
    }

    @Override
    public String toString() {
        return "DataEntry{" +
                "permissions=" + permissions +
                ", options=" + options +
                ", parents=" + parents +
                ", defaultValue=" + defaultValue +
                '}';
    }

    public boolean isEmpty() {
        return (this.permissions == null || this.permissions.isEmpty())
                && (this.options == null || this.options.isEmpty())
                && (this.parents == null || this.parents.isEmpty())
                && this.defaultValue == null;
    }
}

package ninja.leaping.permissionsex.sponge.option;

import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;

import java.util.Map;
import java.util.Set;

public interface OptionSubjectData extends SubjectData {
    public Map<Set<Context>, Map<String, String>> getAllOptions();

    public Map<String, String> getOptions(Set<Context> contexts);

    public boolean setOption(Set<Context> contexts, String key, String value);

    public boolean clearOptions(Set<Context> contexts);

    public boolean clearOptions();
}

package ninja.leaping.permissionsex.sponge.option;

import com.google.common.base.Optional;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import java.util.Set;

public interface OptionSubject extends Subject {
    @Override
    OptionSubjectData getData();

    @Override
    OptionSubjectData getTransientData();

    /**
     * Get the value of a given option in the given context
     *
     * @param key The key to get an option by
     * @return The value of the option, if any is present
     */
    public Optional<String> getOption(Set<Context> contexts, String key);

    /**
     * Get the value of a given option in the subject's current context
     *
     * @param key The key to get an option by
     * @return The value of the option, if any is present
     */
    public Optional<String> getOption(String key);
}

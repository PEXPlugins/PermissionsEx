package ninja.leaping.permissionsex.data;


import com.google.common.base.Preconditions;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Subject type information
 */
public class SubjectRef {
    private final String type, identifier;

    protected SubjectRef(String type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    public static SubjectRef of(String type, String identifier) {
        return new SubjectRef(checkNotNull(type, "type"), checkNotNull(identifier, "identifier"));
    }

    public String getType() {
        return this.type;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectRef)) return false;
        SubjectRef that = (SubjectRef) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identifier);
    }

    @Override
    public String toString() {
        return this.type + ":" + this.identifier;
    }
}

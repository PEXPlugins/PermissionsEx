package ninja.leaping.permissionsex.util;

import java.util.Comparator;

/**
 * Represents an object that can have weight. This weight should remain constant for any given object.
 *
 * In a {@link WeightedImmutableSet}, the objects with the highest weight will appear at the end of the list.
 */
public interface Weighted {
    Comparator<Weighted> COMPARATOR = (a, b) -> Integer.compare(a.getWeight(), b.getWeight());
    int getWeight();
}

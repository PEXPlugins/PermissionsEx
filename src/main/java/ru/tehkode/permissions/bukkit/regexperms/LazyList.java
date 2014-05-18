package ru.tehkode.permissions.bukkit.regexperms;


import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author devan_000
 */
public class LazyList
        implements Cloneable, Serializable {

    private static final String[] __EMTPY_STRING_ARRAY = new String[0];

    /* ------------------------------------------------------------ */
    private LazyList() {
    }

    /* ------------------------------------------------------------ */
    /**
     * Add an item to a LazyList
     *
     * @param list The list to add to or null if none yet created.
     * @param item The item to add.
     * @return The lazylist created or added to.
     */
    public static Object add(Object list, Object item) {
        if (list == null) {
            if (item instanceof List || item == null) {
                List l = new ArrayList();
                l.add(item);
                return l;
            }

            return item;
        }

        if (list instanceof List) {
            ((List) list).add(item);
            return list;
        }

        List l = new ArrayList();
        l.add(list);
        l.add(item);
        return l;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add an item to a LazyList
     *
     * @param list The list to add to or null if none yet created.
     * @param index The index to add the item at.
     * @param item The item to add.
     * @return The lazylist created or added to.
     */
    public static Object add(Object list, int index, Object item) {
        if (list == null) {
            if (index > 0 || item instanceof List || item == null) {
                List l = new ArrayList();
                l.add(index, item);
                return l;
            }
            return item;
        }

        if (list instanceof List) {
            ((List) list).add(index, item);
            return list;
        }

        List l = new ArrayList();
        l.add(list);
        l.add(index, item);
        return l;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add the contents of a Collection to a LazyList
     *
     * @param list The list to add to or null if none yet created.
     * @param collection The Collection whose contents should be added.
     * @return The lazylist created or added to.
     */
    public static Object addCollection(Object list, Collection collection) {
        Iterator i = collection.iterator();
        while (i.hasNext()) {
            list = LazyList.add(list, i.next());
        }
        return list;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add the contents of an array to a LazyList
     *
     * @param list The list to add to or null if none yet created.
     * @param collection The Collection whose contents should be added.
     * @return The lazylist created or added to.
     */
    public static Object addArray(Object list, Object[] array) {
        for (int i = 0; array != null && i < array.length; i++) {
            list = LazyList.add(list, array[i]);
        }
        return list;
    }

    /* ------------------------------------------------------------ */
    /**
     * Ensure the capcity of the underlying list.
     *
     */
    public static Object ensureSize(Object list, int initialSize) {
        if (list == null) {
            return new ArrayList(initialSize);
        }
        if (list instanceof ArrayList) {
            ArrayList ol = (ArrayList) list;
            if (ol.size() > initialSize) {
                return ol;
            }
            ArrayList nl = new ArrayList(initialSize);
            nl.addAll(ol);
            return nl;
        }
        List l = new ArrayList(initialSize);
        l.add(list);
        return l;
    }

    /* ------------------------------------------------------------ */
    public static Object remove(Object list, Object o) {
        if (list == null) {
            return null;
        }

        if (list instanceof List) {
            List l = (List) list;
            l.remove(o);
            if (l.size() == 0) {
                return null;
            }
            return list;
        }

        if (list.equals(o)) {
            return null;
        }
        return list;
    }

    /* ------------------------------------------------------------ */
    public static Object remove(Object list, int i) {
        if (list == null) {
            return null;
        }

        if (list instanceof List) {
            List l = (List) list;
            l.remove(i);
            if (l.size() == 0) {
                return null;
            }
            return list;
        }

        if (i == 0) {
            return null;
        }
        return list;
    }

    /* ------------------------------------------------------------ */
    /**
     * Get the real List from a LazyList.
     *
     * @param list A LazyList returned from LazyList.add(Object)
     * @return The List of added items, which may be an EMPTY_LIST or a
     * SingletonList.
     */
    public static List getList(Object list) {
        return getList(list, false);
    }


    /* ------------------------------------------------------------ */
    /**
     * Get the real List from a LazyList.
     *
     * @param list A LazyList returned from LazyList.add(Object) or null
     * @param nullForEmpty If true, null is returned instead of an empty list.
     * @return The List of added items, which may be null, an EMPTY_LIST or a
     * SingletonList.
     */
    public static List getList(Object list, boolean nullForEmpty) {
        if (list == null) {
            return nullForEmpty ? null : Collections.EMPTY_LIST;
        }
        if (list instanceof List) {
            return (List) list;
        }

        List l = new ArrayList(1);
        l.add(list);
        return l;
    }

    /* ------------------------------------------------------------ */
    public static String[] toStringArray(Object list) {
        if (list == null) {
            return __EMTPY_STRING_ARRAY;
        }

        if (list instanceof List) {
            List l = (List) list;
            String[] a = new String[l.size()];
            for (int i = l.size(); i-- > 0;) {
                Object o = l.get(i);
                if (o != null) {
                    a[i] = o.toString();
                }
            }
            return a;
        }

        return new String[]{list.toString()};
    }

    /* ------------------------------------------------------------ */
    public static Object toArray(Object list, Class aClass) {
        if (list == null) {
            return (Object[]) Array.newInstance(aClass, 0);
        }

        if (list instanceof List) {
            List l = (List) list;
            if (aClass.isPrimitive()) {
                Object a = Array.newInstance(aClass, l.size());
                for (int i = 0; i < l.size(); i++) {
                    Array.set(a, i, l.get(i));
                }
                return a;
            }
            return l.toArray((Object[]) Array.newInstance(aClass, l.size()));

        }

        Object a = Array.newInstance(aClass, 1);
        Array.set(a, 0, list);
        return a;
    }

    /* ------------------------------------------------------------ */
    /**
     * The size of a lazy List
     *
     * @param list A LazyList returned from LazyList.add(Object) or null
     * @return the size of the list.
     */
    public static int size(Object list) {
        if (list == null) {
            return 0;
        }
        if (list instanceof List) {
            return ((List) list).size();
        }
        return 1;
    }

    /* ------------------------------------------------------------ */
    /**
     * Get item from the list
     *
     * @param list A LazyList returned from LazyList.add(Object) or null
     * @param i int index
     * @return the item from the list.
     */
    public static Object get(Object list, int i) {
        if (list == null) {
            throw new IndexOutOfBoundsException();
        }

        if (list instanceof List) {
            return ((List) list).get(i);
        }

        if (i == 0) {
            return list;
        }

        throw new IndexOutOfBoundsException();
    }

    /* ------------------------------------------------------------ */
    public static boolean contains(Object list, Object item) {
        if (list == null) {
            return false;
        }

        if (list instanceof List) {
            return ((List) list).contains(item);
        }

        return list.equals(item);
    }


    /* ------------------------------------------------------------ */
    public static Object clone(Object list) {
        if (list == null) {
            return null;
        }
        if (list instanceof List) {
            return new ArrayList((List) list);
        }
        return list;
    }

    /* ------------------------------------------------------------ */
    public static String toString(Object list) {
        if (list == null) {
            return "[]";
        }
        if (list instanceof List) {
            return ((List) list).toString();
        }
        return "[" + list + "]";
    }

    /* ------------------------------------------------------------ */
    public static Iterator iterator(Object list) {
        if (list == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        if (list instanceof List) {
            return ((List) list).iterator();
        }
        return getList(list).iterator();
    }

    /* ------------------------------------------------------------ */
    public static ListIterator listIterator(Object list) {
        if (list == null) {
            return Collections.EMPTY_LIST.listIterator();
        }
        if (list instanceof List) {
            return ((List) list).listIterator();
        }
        return getList(list).listIterator();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param array Any array of object
     * @return A new <i>modifiable</i> list initialised with the elements from
     * <code>array</code>.
     */
    public static List array2List(Object[] array) {
        if (array == null || array.length == 0) {
            return new ArrayList();
        }
        return new ArrayList(Arrays.asList(array));
    }

    /* ------------------------------------------------------------ */
    /**
     * Add element to an array
     *
     * @param array The array to add to (or null)
     * @param item The item to add
     * @param type The type of the array (in case of null array)
     * @return new array with contents of array plus item
     */
    public static Object[] addToArray(Object[] array, Object item, Class type) {
        if (array == null) {
            if (type == null && item != null) {
                type = item.getClass();
            }
            Object[] na = (Object[]) Array.newInstance(type, 1);
            na[0] = item;
            return na;
        } else {
            Class c = array.getClass().getComponentType();
            Object[] na = (Object[]) Array.newInstance(c, Array.getLength(array) + 1);
            System.arraycopy(array, 0, na, 0, array.length);
            na[array.length] = item;
            return na;
        }
    }

    /* ------------------------------------------------------------ */
    public static Object[] removeFromArray(Object[] array, Object item) {
        if (item == null || array == null) {
            return array;
        }
        for (int i = array.length; i-- > 0;) {
            if (item.equals(array[i])) {
                Class c = array == null ? item.getClass() : array.getClass().getComponentType();
                Object[] na = (Object[]) Array.newInstance(c, Array.getLength(array) - 1);
                if (i > 0) {
                    System.arraycopy(array, 0, na, 0, i);
                }
                if (i + 1 < array.length) {
                    System.arraycopy(array, i + 1, na, i, array.length - (i + 1));
                }
                return na;
            }
        }
        return array;
    }

}

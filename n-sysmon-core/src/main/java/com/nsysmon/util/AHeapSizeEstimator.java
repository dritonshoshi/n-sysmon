package com.nsysmon.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estimates the retained heap size of an object graph.
 * <p>
 * This is deliberately an estimate, not an exact measurement: it assumes a 64 bit JVM with compressed oops, uses
 *  fixed per-entry overheads for JDK collections and does not instrument the JVM. It is accurate enough to identify
 *  which data sink consumes the most memory.
 * <p>
 * JDK internals are never accessed reflectively (Java 17 modules forbid that without {@code --add-opens}). JDK objects
 *  are accounted for with a shallow size, collections and maps are traversed through their public API. Application
 *  classes (e.g. {@code com.nsysmon} and {@code com.ajjpj.afoundation}) are traversed reflectively, which also captures
 *  the pre-allocated backing array of an {@code ARingBuffer}.
 *
 * @author arno
 */
public final class AHeapSizeEstimator {
    private static final int REFERENCE_SIZE = 4;
    private static final int OBJECT_HEADER_SIZE = 12;
    private static final int ARRAY_HEADER_SIZE = 16;
    private static final int JDK_OBJECT_SIZE = 24;
    private static final int COLLECTION_OVERHEAD = 16;
    private static final int MAP_OVERHEAD = 32;
    private static final int MAP_ENTRY_OVERHEAD = 40;
    private static final int NODE_OVERHEAD = 32;

    private static final Field[] NO_FIELDS = new Field[0];
    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    private final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();

    /**
     * @return the estimated number of bytes retained by the graph rooted at {@code root}.
     */
    public long estimate(Object root) {
        visited.clear();
        return estimateObject(root);
    }

    /**
     * Counts the elements of an iterable. For an {@code ARingBuffer} this uses its iterator, which allocates a
     *  temporary copy of the backing array - acceptable for an explicitly requested diagnostics report.
     */
    public static long count(Iterable<?> data) {
        if (data == null) {
            return -1;
        }

        long result = 0;
        for (Object ignored : data) {
            result++;
        }
        return result;
    }

    private long estimateObject(Object object) {
        if (object == null || visited.put(object, Boolean.TRUE) != null) {
            return 0;
        }

        if (object instanceof String string) {
            return estimateString(string);
        }
        if (object instanceof Class<?> || object instanceof ClassLoader) {
            return 0;
        }

        final Class<?> clazz = object.getClass();
        if (clazz.isArray()) {
            return estimateArray(object, clazz);
        }
        if (object instanceof Map<?, ?> map) {
            return estimateMap(map);
        }
        if (object instanceof Collection<?> collection) {
            return estimateCollection(collection);
        }
        if (object instanceof Map.Entry<?, ?> entry) {
            return estimateEntry(entry);
        }
        if (isJdkClass(clazz)) {
            return JDK_OBJECT_SIZE;
        }
        return estimateReflective(object, clazz);
    }

    private long estimateArray(Object array, Class<?> arrayClass) {
        final int length = Array.getLength(array);
        final Class<?> componentType = arrayClass.getComponentType();

        if (componentType.isPrimitive()) {
            return align(ARRAY_HEADER_SIZE + (long) length * primitiveSize(componentType));
        }

        long result = align(ARRAY_HEADER_SIZE + (long) length * REFERENCE_SIZE);
        for (int i = 0; i < length; i++) {
            result += estimateObject(Array.get(array, i));
        }
        return result;
    }

    private long estimateCollection(Collection<?> collection) {
        long result = align(OBJECT_HEADER_SIZE + REFERENCE_SIZE) + COLLECTION_OVERHEAD;

        for (Object element : collection) {
            result += perElementOverhead(collection) + estimateObject(element);
        }
        return result;
    }

    private long estimateMap(Map<?, ?> map) {
        long result = align(OBJECT_HEADER_SIZE + REFERENCE_SIZE) + MAP_OVERHEAD;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result += MAP_ENTRY_OVERHEAD;
            result += estimateObject(entry.getKey());
            result += estimateObject(entry.getValue());
        }
        return result;
    }

    private long estimateEntry(Map.Entry<?, ?> entry) {
        return MAP_ENTRY_OVERHEAD + estimateObject(entry.getKey()) + estimateObject(entry.getValue());
    }

    private long estimateReflective(Object object, Class<?> clazz) {
        long result = align(OBJECT_HEADER_SIZE);

        for (Field field : fieldsOf(clazz)) {
            if (field.getType().isPrimitive()) {
                result += primitiveSize(field.getType());
            } else {
                result += REFERENCE_SIZE;
                result += estimateObject(readField(field, object));
            }
        }
        return result;
    }

    private static long perElementOverhead(Collection<?> collection) {
        if (collection instanceof Queue<?> || collection instanceof Set<?>) {
            return NODE_OVERHEAD;
        }
        if (collection instanceof List<?>) {
            return REFERENCE_SIZE * 2L;
        }
        return COLLECTION_OVERHEAD;
    }

    private static long estimateString(String string) {
        // Java 9+ stores a byte[] per String. The coder is not accessible, so assume two bytes per character.
        return align(OBJECT_HEADER_SIZE + REFERENCE_SIZE) + align(ARRAY_HEADER_SIZE + 2L * string.length());
    }

    private static Object readField(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Field[] fieldsOf(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, AHeapSizeEstimator::collectFields);
    }

    private static Field[] collectFields(Class<?> clazz) {
        final List<Field> result = new ArrayList<>();

        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    result.add(field);
                } catch (RuntimeException e) {
                    // the class is not open to reflection (e.g. a named module) - use a shallow estimate instead
                    return NO_FIELDS;
                }
            }
        }
        return result.toArray(new Field[0]);
    }

    private static boolean isJdkClass(Class<?> clazz) {
        final String name = clazz.getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
                || name.startsWith("sun.") || name.startsWith("com.sun.");
    }

    private static int primitiveSize(Class<?> type) {
        if (type == boolean.class || type == byte.class) {
            return 1;
        }
        if (type == char.class || type == short.class) {
            return 2;
        }
        if (type == int.class || type == float.class) {
            return 4;
        }
        if (type == long.class || type == double.class) {
            return 8;
        }
        return REFERENCE_SIZE;
    }

    private static long align(long size) {
        return (size + 7) & ~7L;
    }
}

package com.nsysmon.measure.environment.impl;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class AJarFileEnvironmentMeasurerTest {

    @TempDir
    private Path tempDir;

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private AJarFileEnvironmentMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new AJarFileEnvironmentMeasurer();
    }

    private static Path writeJar(Path dir, String name, String version) throws IOException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (version != null) {
            manifest.getMainAttributes().putValue("Implementation-Version", version);
        }
        final Path jar = dir.resolve(name);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            out.putNextEntry(new JarEntry("content.txt"));
            out.write("x".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private static Path writePlainManifest(Path dir) throws IOException {
        final Path metaInf = Files.createDirectories(dir.resolve("META-INF"));
        Files.writeString(metaInf.resolve("MANIFEST.MF"), "Manifest-Version: 1.0\r\n\r\n");
        return dir;
    }

    static List<String> nameOf(AEnvironmentData data) {
        return List.copyOf(data.getName().asJavaUtilList());
    }

    private List<String> names() {
        return collected.stream().map(d -> String.join("/", nameOf(d))).toList();
    }

    private List<String> valuesOf(List<String> name) {
        return collected.stream().filter(d -> nameOf(d).equals(name)).map(AEnvironmentData::getValue).toList();
    }

    private void collectJars(ClassLoader classLoader, Set<URL> alreadyVisited) throws Exception {
        final Method method = AJarFileEnvironmentMeasurer.class.getDeclaredMethod(
                "collectJars", ClassLoader.class, Set.class, AEnvironmentMeasurer.EnvironmentCollector.class);
        method.setAccessible(true);
        method.invoke(measurer, classLoader, alreadyVisited, collector);
    }

    private void collectJars(ClassLoader classLoader) throws Exception {
        collectJars(classLoader, new HashSet<>());
    }

    private static AOption<String> extractJarName(String url) throws Exception {
        final Method method = AJarFileEnvironmentMeasurer.class.getDeclaredMethod("extractJarName", URL.class);
        method.setAccessible(true);
        return (AOption<String>) method.invoke(new AJarFileEnvironmentMeasurer(), new URL(url));
    }

    private static URLClassLoader classLoader(URL[] urls, ClassLoader parent) {
        return new URLClassLoader(urls, parent);
    }

    @Test
    public void versionIsTakenFromTheImplementationVersionOfTheManifest() throws Exception {
        final Path jar = writeJar(tempDir, "versioned.jar", "1.2.3");
        final ClassLoader cl = classLoader(new URL[]{jar.toUri().toURL()}, null);

        collectJars(cl);

        assertEquals(List.of("1.2.3"), valuesOf(List.of(
                AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION, cl.getClass().getName(), "versioned.jar")));
    }

    @Test
    public void missingImplementationVersionIsReportedAsUnknown() throws Exception {
        final Path jar = writeJar(tempDir, "unversioned.jar", null);
        final ClassLoader cl = classLoader(new URL[]{jar.toUri().toURL()}, null);

        collectJars(cl);

        assertEquals(List.of("???"), valuesOf(List.of(
                AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION, cl.getClass().getName(), "unversioned.jar")));
    }

    @Test
    public void manifestsOutsideOfJarFilesAreIgnored() throws Exception {
        final Path exploded = writePlainManifest(Files.createDirectories(tempDir.resolve("exploded")));
        final ClassLoader cl = classLoader(new URL[]{exploded.toUri().toURL()}, null);

        collectJars(cl);

        assertEquals(List.of(), names().stream().filter(n -> n.contains("exploded")).toList());
        assertEquals(List.of("1 JARs"),
                valuesOf(List.of(AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION, cl.getClass().getName())));
    }

    @Test
    public void allManifestsOfTheClassLoaderAreCounted() throws Exception {
        final Path jar = writeJar(tempDir, "a.jar", "1.0");
        final Path exploded = writePlainManifest(Files.createDirectories(tempDir.resolve("exploded")));
        final ClassLoader cl = classLoader(new URL[]{jar.toUri().toURL(), exploded.toUri().toURL()}, null);

        collectJars(cl);

        assertEquals(List.of("2 JARs"),
                valuesOf(List.of(AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION, cl.getClass().getName())));
    }

    @Test
    public void parentClassLoadersAreVisitedFirst() throws Exception {
        final Path parentJar = writeJar(tempDir, "parent.jar", "9.9");
        final Path childJar = writeJar(tempDir, "child.jar", "8.8");
        final URLClassLoader parent = classLoader(new URL[]{parentJar.toUri().toURL()}, null);
        final URLClassLoader child = classLoader(new URL[]{childJar.toUri().toURL()}, parent);

        collectJars(child);

        assertEquals(List.of("9.9"), valuesOf(List.of(
                AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION, parent.getClass().getName(), "parent.jar")));
        assertEquals(List.of("8.8"), valuesOf(List.of(
                AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION, child.getClass().getName(), "child.jar")));
    }

    @Test
    public void alreadyVisitedManifestsAreCollectedOnlyOnce() throws Exception {
        final Path jar = writeJar(tempDir, "once.jar", "1.0");
        final ClassLoader cl = classLoader(new URL[]{jar.toUri().toURL()}, null);
        final Set<URL> alreadyVisited = new HashSet<>();

        collectJars(cl, alreadyVisited);
        collected.clear();
        collectJars(cl, alreadyVisited);

        assertEquals(List.of(), names().stream().filter(n -> n.contains("once.jar")).toList());
        assertEquals(List.of("0 JARs"),
                valuesOf(List.of(AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION, cl.getClass().getName())));
    }

    @Test
    public void jarNameIsExtractedFromTheManifestUrl() throws Exception {
        assertEquals("my.jar", extractJarName("jar:file:/opt/lib/my.jar!/META-INF/MANIFEST.MF").get());
        assertEquals("my.jar", extractJarName("jar:file:/opt/lib/my.jar!/sub/dir/META-INF/MANIFEST.MF").get());
    }

    @Test
    public void urlsWithoutAJarMarkerHaveNoJarName() throws Exception {
        assertTrue(extractJarName("file:/opt/classes/META-INF/MANIFEST.MF").isEmpty());
    }

    @Test
    public void everyCollectedEntryStartsWithTheJarVersionKey() throws Exception {
        final Path jar = writeJar(tempDir, "a.jar", "1.0");
        final ClassLoader cl = classLoader(new URL[]{jar.toUri().toURL()}, null);

        collectJars(cl);

        assertFalse(collected.isEmpty());
        assertTrue(Arrays.stream(collected.toArray(new AEnvironmentData[0])).allMatch(d ->
                        AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION.equals(nameOf(d).get(0))),
                names().toString());
    }

    @Test
    public void allJarsOfTheContextClassLoaderAreCollected() throws Exception {
        measurer.contributeMeasurements(collector);

        assertFalse(collected.isEmpty());
        assertTrue(names().stream().allMatch(n -> n.startsWith(AJarFileEnvironmentMeasurer.KEY_PREFIX_JAR_VERSION + "/")),
                names().toString());
    }

    @Test
    public void oneJarCountIsReportedPerClassLoaderLevel() throws Exception {
        measurer.contributeMeasurements(collector);

        final List<AEnvironmentData> counts = collected.stream().filter(d -> nameOf(d).size() == 2).toList();
        assertFalse(counts.isEmpty(), names().toString());
        final Map<String, Long> numJarsPerClassLoader = new HashMap<>();
        for (AEnvironmentData count : counts) {
            numJarsPerClassLoader.put(nameOf(count).get(1), Long.parseLong(count.getValue().replace(" JARs", "")));
        }
        final Map<String, Long> collectedPerClassLoader = collected.stream().filter(d -> nameOf(d).size() == 3)
                .collect(Collectors.groupingBy(d -> nameOf(d).get(1), Collectors.counting()));
        assertTrue(numJarsPerClassLoader.keySet().containsAll(collectedPerClassLoader.keySet()),
                collectedPerClassLoader.keySet().toString());
        collectedPerClassLoader.forEach((classLoader, numJars) ->
                assertEquals(numJars, numJarsPerClassLoader.get(classLoader), classLoader));
        assertTrue(numJarsPerClassLoader.size() >= collectedPerClassLoader.size());
    }

    @Test
    public void shutdownLeavesAlreadyCollectedDataUntouched() throws Exception {
        final Path jar = writeJar(tempDir, "a.jar", "1.0");
        final ClassLoader cl = classLoader(new URL[]{jar.toUri().toURL()}, null);
        collectJars(cl);
        final int numCollected = collected.size();

        measurer.shutdown();

        assertEquals(numCollected, collected.size());
    }
}

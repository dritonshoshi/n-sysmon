package com.nsysmon.measure.environment.impl;

import com.ajjpj.afoundation.proc.CliCommand;
import com.nsysmon.NSysMon;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This measurer collects information about mounted file systems from several sources.
 *
 * @author arno
 */
public class AFileSystemsEnvironmentMeasurer implements AEnvironmentMeasurer {
    public static final String KEY_FILESYSTEMS = "file systems";
    public static final String JVM_PARAMETERS = "JVM Parameters";

    @Override
    public void contributeMeasurements(EnvironmentCollector data) throws Exception {
        contributeMtab(data);
        contributeDf(data);
        contributeJdkParameters(data);
    }

    private static void contributeJdkParameters(EnvironmentCollector data) throws Exception {

        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMxBean.getInputArguments();

        for (String arg : jvmArgs) {
            String[] split = new String[2];
            if (arg.indexOf(':') != -1) {
                split = arg.split(":", 2);
            } else if (arg.indexOf('=') != -1) {
                split = arg.split("=", 2);
            }
            data.add(split[1], JVM_PARAMETERS, split[0]);
        }
    }

    private static void contributeDf(EnvironmentCollector data) throws Exception {
        if (NSysMon.isWindows()) {
            return;
        }

        for (String line : new CliCommand("df", "-P").getOutput()) {
            if (!line.startsWith("/dev/")) {
                continue;
            }

            final String[] split = line.split("\\s+");
            if (split.length != 6) {
                continue;
            }

            final String device = split[0];
            final String size = split[1];
            final String used = split[2];
            final String available = split[3];
            final String usedPercent = split[4];
            final String mountPoint = split[5];

            add(data, device, "Size Total (1k Blocks)", size);
            add(data, device, "Size Used (1k Blocks)", used);
            add(data, device, "Size Available (1k Blocks)", available);
            add(data, device, "Size Used (%)", usedPercent);
            add(data, device, "Mount Point", mountPoint);
        }
    }

    public static Map<String, String> getMountPoints() throws Exception {
        final Map<String, String> result = new HashMap<>();

        for (String line : new CliCommand("df", "-P").getOutput()) {
            if (!line.startsWith("/dev/")) {
                continue;
            }

            final String[] split = line.split("\\s+");
            if (split.length != 6) {
                continue;
            }

            result.put(split[0].substring("/dev/".length()), split[5]);
        }

        return result;
    }

    private void contributeMtab(EnvironmentCollector data) throws IOException {
        if (NSysMon.isWindows()) {
            return;
        }

        final BufferedReader br = new BufferedReader(new FileReader(new File("/etc/mtab"))); //TODO refactor to use AFile
        try {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (!line.startsWith("/dev/")) {
                    continue;
                }

                final String[] split = line.split(" ");
                if (split.length < 4) {
                    continue; // should not happen, but you never know
                }

                final String device = split[0];
                final String mountPoint = split[1];
                final String fsType = split[2];
                final String flags = split[3];

                add(data, device, "Type", fsType);
                add(data, device, "Flags", flags);

                data.add(mountPoint, ACpuEnvironmentMeasurer.KEY_HW, KEY_FILESYSTEMS, device);
            }
        } finally {
            br.close();
        }
    }

    private static void add(EnvironmentCollector data, String device, String key, String value) {
        data.add(value, ACpuEnvironmentMeasurer.KEY_HW, KEY_FILESYSTEMS, device, key);
    }

    @Override
    public void shutdown() throws Exception {
    }
}

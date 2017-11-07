# N-Sysmon
N-Sysmon is a toolset for monitoring and exploring performance aspects of Java enterprise applications. N-Sysmon is a fork of A-Sysmon.

# Documentation

## How do you get started with N-Sysmon?
1. N-Sysmon is available via [Maven] (www.maven.org).
```
  <dependency>
            <groupId>com.github.dritonshoshi</groupId>
            <artifactId>n-sysmon-core</artifactId>
            <version>${project.version}</version>
  </dependency>
```
2. Add it to your web.xml
```
<servlet>
        <servlet-name>nsysmon</servlet-name>
        <servlet-class>com.nsysmon.servlet.NSysMonServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>nsysmon</servlet-name>  
        <url-pattern>/nsysmon/*</url-pattern>   <-- this means that n-sysmon will be available under *htttp(s)://hostip:port/nsysmon*
    </servlet-mapping>
    
```
  
3. Add your measurepoints in your application code

```
    final ASimpleMeasurement parMeasurement = NSysMon.get().start("Your measure id", false);
           NSysMon.get().measure("Your measure method", m -> {
                doQuery(); 
                return;
            }); 
    parMeasurement.finish();
```
Now you should be able to see the measuring points under *htttp(s)://hostip:port/nsysmon* 

4. Configuration

Take a look in *nsysmon.properties* : 

```
application-package = com.yourpackage
application-name = Your N-SysMon Application
application-version = 1.0

drilldown-measurement-initially-started = true

max-measurements-per-hierarchy = 500000
data-sink-timeout-nanos = 1000000000

timedscalar-measurers = com.nsysmon.measure.scalar.SystemLoadOneMinuteMeasurer, \
                        com.nsysmon.measure.scalar.CpuTotalAndUserMeasurer, \
                        com.nsysmon.measure.scalar.ACpuUtilizationMeasurer, \
                        com.nsysmon.measure.scalar.ThreadingMeasurer

```

# License
N-Sysmon is Open Source and available under the Apache 2 License.

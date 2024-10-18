package com.nsysmon.servlet;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import org.apache.http.entity.ContentType;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This servlet_ class serves static resources and dispatches RESTful service calls
 *
 * @author arno
 */
public abstract class AbstractNSysMonServlet extends HttpServlet {
    public static final String NSYSMON_MARKER_SEGMENT = "/_$_nsysmon_$_/";
    public static final String NSYSMON_MARKER_STATIC = NSYSMON_MARKER_SEGMENT + "static/";
    public static final String NSYSMON_MARKER_REST = NSYSMON_MARKER_SEGMENT + "rest/";

    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final String uri = req.getRequestURI();

            if(uri.contains(NSYSMON_MARKER_STATIC)) {
                serveStaticResource(substringAfter(uri, NSYSMON_MARKER_STATIC), resp);
                return;
            }

            if(uri.contains(NSYSMON_MARKER_REST)) {
                resp.setContentType(String.valueOf(ContentType.APPLICATION_JSON));
                try {
                    final String[] restPart = substringAfter(uri, NSYSMON_MARKER_REST).split("/");
                    if(!handleRestCall(new ArrayList<>(Arrays.asList(restPart)), resp)) {
                        throw new IllegalArgumentException("unsupported REST call: " + uri);
                    }
                    return;
                }
                catch(Exception exc) {
                    try {
                        // special status code to indicate an exception that is reported as a JSON message
                        resp.setStatus(599);
                        writeExceptionToJson(new AJsonSerHelperForNSysmon(resp.getOutputStream()), exc);
                    }
                    catch(Exception e2) {
                        // throw the original exception if there is a problem in the special handling code
                        throw new ServletException(exc);
                    }
                }
            }

            if(uri.contains(NSYSMON_MARKER_SEGMENT)) {
                final String[] dynamicPart = substringAfter(uri, NSYSMON_MARKER_SEGMENT).split("/");
                if(! handleDynamic(new ArrayList<>(Arrays.asList(dynamicPart)), resp)) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }

            if(! uri.endsWith("/")) {
                resp.sendRedirect(uri + "/");
                return;
            }

            serveStaticResource(getDefaultHtmlName(), resp);
        }
        catch (RuntimeException | ServletException | IOException exc) {
            throw exc;
        } catch(Exception exc) {
            throw new ServletException(exc);
        }
    }

    private void writeExceptionToJson(AJsonSerHelperForNSysmon json, Exception exc) throws IOException {
        json.startObject();

        if(exc.getMessage() != null) {
            json.writeKey("msg");
            json.writeStringLiteral(exc.getMessage());
        }

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        exc.printStackTrace(pw);
        pw.close();

        json.writeKey("details");
        json.startArray();

        final BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
        String line;
        while ((line = br.readLine()) != null) {
            json.writeStringLiteral(line);
        }

        json.endArray();

        json.endObject();
    }

    protected abstract String getDefaultHtmlName();
    protected abstract boolean handleRestCall(List<String> pathSegments, HttpServletResponse resp) throws Exception;
    protected boolean handleDynamic(List<String> pathSegments, HttpServletResponse resp) throws IOException {
        return false;
    }

    private static String substringAfter(String s, String sub) {
        final int idx = s.indexOf(sub);
        return s.substring(idx + sub.length());
    }

    private void serveStaticResource(String resName, HttpServletResponse resp) throws IOException {
        if(resName.contains("..") || resName.startsWith("/") || resName.contains("//")) {
            throw new IllegalArgumentException("rejected resource request '"  + resName + "' for security reasons");
        }

        if(getContentType(resName) != null) {
            resp.setContentType(getContentType(resName));
        }

        final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("nsysmon-res/" + resName);
        if(in == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        resp.addHeader("Cache-Control", "max-age=36000");

        final OutputStream out = resp.getOutputStream();

        final byte[] buf = new byte[4096];
        int numRead;
        while((numRead = in.read(buf)) > 0) {
            out.write(buf, 0, numRead);
        }
    }

    protected String getContentType(String resName) {
        if(resName.contains(".html")) {
            return "text/html";
        } else if(resName.contains(".css")) {
            return "text/css";
        } else if(resName.contains(".js")) {
            return "text/javascript";
        }

        return null;
    }
}

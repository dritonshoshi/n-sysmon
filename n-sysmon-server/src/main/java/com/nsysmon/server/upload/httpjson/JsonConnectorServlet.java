package com.nsysmon.server.upload.httpjson;

import com.nsysmon.server.init.InitServletContextListener;
import com.nsysmon.server.upload.preprocess.InputProcessor;
import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.server.data.json.EnvironmentNode;
import com.nsysmon.server.data.json.RootNode;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.data.json.TraceRootNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @author arno
 */
public class JsonConnectorServlet extends HttpServlet {
    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        System.out.print("received: ");
//        int ch;
//        while ((ch = req.getReader().read()) != -1) {
//            System.out.print((char) ch);
//        }
//        System.out.println();

        try {
            final ObjectMapper om = new ObjectMapper();
            final RootNode root = om.readValue(req.getInputStream(), RootNode.class);

//            System.out.println("received " + root);

            final InputProcessor processor = getProcessor();
            final InstanceIdentifier instanceIdentifier = new InstanceIdentifier(root.getSender(), root.getSenderInstance());

            processor.updateSystemClockDiff(instanceIdentifier, root.getSenderTimestamp());

            for(EnvironmentNode envNode: root.getEnvironment()) {
                processor.addEnvironmentEntry(instanceIdentifier, envNode);
            }
            for(ScalarNode scalarNode: root.getScalars()) {
                processor.addScalarEntry(instanceIdentifier, scalarNode);
            }
            for(TraceRootNode traceNode: root.getTraces()) {
                processor.addTraceEntry(instanceIdentifier, traceNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        //TODO error handling --> log unparsable request
    }

    protected InputProcessor getProcessor() {
        return InitServletContextListener.getInjector().getInstance(InputProcessor.class);
    }
}

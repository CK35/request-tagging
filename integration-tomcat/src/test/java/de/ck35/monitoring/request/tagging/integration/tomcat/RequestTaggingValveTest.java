package de.ck35.monitoring.request.tagging.integration.tomcat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Tomcat;
import org.junit.Ignore;
import org.junit.Test;

public class RequestTaggingValveTest {

    @Test
    @Ignore
    public void testValve() {
        Catalina catalina = new Catalina();
        catalina.setConfigFile(getClass().getResource("/server.xml").getFile());
        catalina.start();
        catalina.stop();
    }
    
    @Test
    @Ignore
    public void test() throws Exception {
        String basedir = Paths.get("target").toAbsolutePath().toString();
        
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(basedir);

        Connector connector = new Connector();
        connector.setPort(8080);
        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);
        tomcat.getHost().setAutoDeploy(false);
        tomcat.getEngine().setBackgroundProcessorDelay(-1);
        
        StandardContext context = new StandardContext();
        context.setName("context");
        context.setDocBase(basedir);
        tomcat.getHost().addChild(context);

        Wrapper servlet = context.createWrapper();
        servlet.setName("servlet");
        servlet.setParentClassLoader(RequestTaggingValveTest.class.getClassLoader());
        servlet.setServletClass(TestServlet.class.getName());
        servlet.setLoadOnStartup(1);
        context.addChild(servlet);
        context.addValve(new RequestTaggingValve());
        context.addServletMapping("/", "servlet");
        
        tomcat.start();
        
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("http://localhost:8080/").openConnection().getInputStream(), StandardCharsets.UTF_8))) {
            reader.lines().forEach(System.out::println);
        }

        tomcat.getServer().stop();
    }

}
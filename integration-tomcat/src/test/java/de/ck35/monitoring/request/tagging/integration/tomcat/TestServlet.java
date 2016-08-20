package de.ck35.monitoring.request.tagging.integration.tomcat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.ck35.monitoring.request.tagging.RequestTagging;

public class TestServlet extends HttpServlet {

    public TestServlet() {
        System.out.println("-----------------------------------------------");
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        RequestTagging.get().withResourceName("TestGet").withMetaData("operation", "GET");
    }
   
}
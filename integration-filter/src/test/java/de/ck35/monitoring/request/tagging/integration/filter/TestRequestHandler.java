package de.ck35.monitoring.request.tagging.integration.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpRequestHandler;

import de.ck35.monitoring.request.tagging.RequestTagging;
import de.ck35.monitoring.request.tagging.RequestTagging.Status;

public class TestRequestHandler implements HttpRequestHandler {
    

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Optional<Status> requestTagging = RequestTagging.getOptional();
        if(requestTagging.isPresent()) {
            response.setStatus(200);
        } else {
            response.setStatus(500);
        }
    }

}

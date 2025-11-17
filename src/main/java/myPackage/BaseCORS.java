package myPackage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public abstract class BaseCORS extends HttpServlet {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app"
    );

    // Method to add CORS headers
    protected void addCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "86400"); // 24 hours cache
    }

    // Handle preflight OPTIONS request
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        addCORSHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(request, response);
        doGetCors(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(request, response);
        doPostCors(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(request, response);
        doPutCors(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(request, response);
        doDeleteCors(request, response);
    }

    // Abstract methods for extending servlets to implement actual logic
    protected abstract void doGetCors(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
    protected abstract void doPostCors(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
    protected abstract void doPutCors(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
    protected abstract void doDeleteCors(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

}

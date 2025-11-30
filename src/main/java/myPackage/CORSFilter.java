package myPackage;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// You can configure the filter for all URLs ("/*") or a package prefix
@WebFilter("/*")
public class CORSFilter implements Filter {
    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:5173",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev",
        "https://wellness-management-system.vercel.app"
    };
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String origin = request.getHeader("Origin");
        boolean isAllowed = false;
        if (origin != null) {
            for (String allowed : ALLOWED_ORIGINS) {
                if (allowed.equals(origin)) {
                    isAllowed = true;
                    break;
                }
            }
        }
        if (isAllowed) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", 
            "Origin, X-Requested-With, Content-Type, Accept, Authorization, ngrok-skip-browser-warning, Ngrok-Skip-Browser-Warning");
        response.setHeader("Access-Control-Max-Age", "3600");
        // Respond immediately to OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        chain.doFilter(req, res); // Continue with other filters/servlets
    }
    // init() and destroy() can be left empty for basic usage
}
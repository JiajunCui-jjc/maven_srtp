package servlet;

import monitor.ExchangeRateMonitor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Serves the CNY->GBP rate dashboard at /exchange_rate
 * and exposes a JSON endpoint at /exchange_rate?format=json
 */
public class ExchangeRateServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String format = req.getParameter("format");

        if ("json".equalsIgnoreCase(format)) {
            // Lightweight JSON for programmatic consumers / auto-refresh
            ExchangeRateMonitor.RateEntry latest =
                    ExchangeRateMonitor.getInstance().getLatest();

            resp.setContentType("application/json;charset=UTF-8");
            if (latest == null) {
                resp.getWriter().write("{\"status\":\"pending\"}");
            } else {
                long nextMs = ExchangeRateMonitor.getInstance().getMillisUntilNext();
                resp.getWriter().write(String.format(
                        "{\"rate\":%.6f,\"time\":\"%s\",\"alert\":%s,\"nextMs\":%d}",
                        latest.rate,
                        latest.getFormattedTime(),
                        latest.alert == null ? "null" : "\"" + latest.alert + "\"",
                        nextMs));
            }
            return;
        }

        // Forward to JSP dashboard
        req.setAttribute("monitor", ExchangeRateMonitor.getInstance());
        req.getRequestDispatcher("/jsp/exchange_rate.jsp")
           .forward(req, resp);
    }
}

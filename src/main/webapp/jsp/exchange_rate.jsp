<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="monitor.ExchangeRateMonitor" %>
<%@ page import="monitor.ExchangeRateMonitor.RateEntry" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.concurrent.TimeUnit" %>
<%
    ExchangeRateMonitor monitor = (ExchangeRateMonitor) request.getAttribute("monitor");
    RateEntry latest = monitor.getLatest();
    List<RateEntry> history = monitor.getHistory();
    long nextMs = monitor.getMillisUntilNext();

    long hoursLeft  = TimeUnit.MILLISECONDS.toHours(nextMs);
    long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(nextMs) % 60;
    long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(nextMs) % 60;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CNY to GBP Rate Monitor</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/static/bootstrap/css/bootstrap.min.css">
    <style>
        body { background: #f4f6f9; }
        .card-rate { background: #fff; border-radius: 12px;
                     box-shadow: 0 2px 12px rgba(0,0,0,.08); }
        .rate-value { font-size: 3.5rem; font-weight: 700; color: #2c3e50; }
        .badge-alert { font-size: .85rem; }
        .countdown { font-size: 1.1rem; color: #7f8c8d; }
        .history-table th { background: #34495e; color: #fff; }
        .alert-row { background: #fff3cd !important; }
        .trend-up   { color: #27ae60; font-weight: bold; }
        .trend-down { color: #e74c3c; font-weight: bold; }
    </style>
</head>
<body>

<%@ include file="nav.jsp" %>

<div class="container mt-4">

    <h2 class="mb-4">
        <span class="glyphicon glyphicon-stats" aria-hidden="true"></span>
        &nbsp;CNY &rarr; GBP Exchange Rate Monitor
    </h2>

    <!-- Current rate card -->
    <div class="row mb-4">
        <div class="col-md-6">
            <div class="card-rate p-4">
                <p class="text-muted mb-1">Current Rate &nbsp;
                    <small class="text-muted">(1 Chinese Yuan)</small>
                </p>
                <% if (latest != null) { %>
                    <div class="rate-value">
                        &pound; <%= String.format("%.6f", latest.rate) %>
                    </div>
                    <p class="text-muted mt-2">
                        Last checked: <strong><%= latest.getFormattedTime() %></strong>
                    </p>
                    <% if (latest.alert != null) { %>
                        <div class="alert alert-warning badge-alert" role="alert">
                            <strong>Alert:</strong> <%= latest.alert %>
                        </div>
                    <% } %>
                <% } else { %>
                    <div class="rate-value text-muted">Fetching&hellip;</div>
                    <p class="text-muted mt-2">First check is in progress.</p>
                <% } %>
            </div>
        </div>

        <!-- Next check countdown -->
        <div class="col-md-6">
            <div class="card-rate p-4 text-center">
                <p class="text-muted mb-2">Next scheduled check in</p>
                <div class="rate-value" id="countdown">
                    <%= String.format("%02d:%02d:%02d", hoursLeft, minutesLeft, secondsLeft) %>
                </div>
                <p class="countdown mt-2">Checks every
                    <strong><%= ExchangeRateMonitor.INTERVAL_HOURS %> hours</strong>
                    automatically.</p>
                <form method="get" action="${pageContext.request.contextPath}/exchange_rate" class="mt-3">
                    <button type="submit" class="btn btn-default">
                        Refresh Dashboard
                    </button>
                </form>
            </div>
        </div>
    </div>

    <!-- History table -->
    <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">Rate History</h3>
        </div>
        <div class="panel-body" style="padding:0">
            <% if (history.isEmpty()) { %>
                <p class="text-center text-muted p-3">No data yet – first fetch is running.</p>
            <% } else { %>
                <table class="table table-striped table-hover history-table mb-0">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Timestamp</th>
                            <th>1 CNY = ? GBP</th>
                            <th>Change</th>
                            <th>Alert</th>
                        </tr>
                    </thead>
                    <tbody>
                    <%
                        // Render newest first
                        for (int i = history.size() - 1; i >= 0; i--) {
                            RateEntry e = history.get(i);
                            double prev = (i > 0) ? history.get(i - 1).rate : e.rate;
                            double changePct = (i > 0) ? (e.rate - prev) / prev * 100.0 : 0.0;
                            String trendClass = changePct > 0 ? "trend-up"
                                             : changePct < 0 ? "trend-down" : "";
                            String rowClass   = e.alert != null ? "alert-row" : "";
                    %>
                        <tr class="<%= rowClass %>">
                            <td><%= i + 1 %></td>
                            <td><%= e.getFormattedTime() %></td>
                            <td>&pound;&nbsp;<%= String.format("%.6f", e.rate) %></td>
                            <td class="<%= trendClass %>">
                                <%= i > 0 ? String.format("%+.3f%%", changePct) : "—" %>
                            </td>
                            <td>
                                <% if (e.alert != null) { %>
                                    <span class="label label-warning"><%= e.alert %></span>
                                <% } else { %>
                                    <span class="text-muted">—</span>
                                <% } %>
                            </td>
                        </tr>
                    <%  } %>
                    </tbody>
                </table>
            <% } %>
        </div>
    </div>

</div><!-- /container -->

<script src="${pageContext.request.contextPath}/static/bootstrap/js/bootstrap.min.js"></script>
<script>
// Live countdown timer (client-side)
(function () {
    var remaining = <%= nextMs %>;
    var el = document.getElementById('countdown');
    if (!el || remaining <= 0) return;

    var interval = setInterval(function () {
        remaining -= 1000;
        if (remaining <= 0) {
            el.textContent = '00:00:00';
            clearInterval(interval);
            // Auto-refresh the page so the new rate is displayed
            window.location.reload();
            return;
        }
        var h = Math.floor(remaining / 3600000);
        var m = Math.floor((remaining % 3600000) / 60000);
        var s = Math.floor((remaining % 60000) / 1000);
        el.textContent =
            pad(h) + ':' + pad(m) + ':' + pad(s);
    }, 1000);

    function pad(n) { return n < 10 ? '0' + n : '' + n; }
})();
</script>
</body>
</html>

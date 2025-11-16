<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="db.jspf" %>
<%@ include file="patch/patchme.jsp" %>

<!DOCTYPE html>
<html>
<head>
  <title>Services - DevSpark IT Solutions</title>
  <link rel="stylesheet" href="theme.css">
  <style>
    body { background: #f4f6f8; font-family: system-ui; color: #333; margin: 0; }
    header { background: white; box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 20px; text-align: center; }
    .container { max-width: 1000px; margin: 40px auto; padding: 0 20px; }
    .service { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); margin-bottom: 20px; }
    h2 { color: #222; margin-bottom: 10px; }
    footer { text-align: center; color: #777; font-size: 0.9em; padding: 20px; }
  </style>
</head>
<body>
<header>
  <h1>Our Services</h1>
  <p>Explore the IT solutions we deliver</p>
</header>

<div class="container">
  <%
    PreparedStatement ps = conn.prepareStatement("SELECT * FROM get_all_services_dynamic()");
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
  %>
  <div class="service">
    <h2><%= rs.getString("name") %></h2>
    <p><%= rs.getString("description") %></p>
  </div>
  <%
    }
    rs.close(); ps.close();
  %>
</div>

<footer>
  <a href="index.jsp">← Back to Home</a>
</footer>
</body>
</html>
<%
  conn.close();
%>
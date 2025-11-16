<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="db.jspf" %>
<!DOCTYPE html>
<html>
<head>
  <title>Contact Us - DevSpark IT Solutions</title>
  <link rel="stylesheet" href="theme.css">
  <style>
    body { background: #f4f6f8; font-family: system-ui; margin: 0; color: #333; }
    header { background: white; box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 20px; text-align: center; }
    .container { max-width: 700px; margin: 40px auto; padding: 0 20px; }
    .card { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
    h2 { margin-top: 0; }
    a { color: #007bff; text-decoration: none; }
    a:hover { text-decoration: underline; }
    footer { text-align: center; margin-top: 40px; color: #777; font-size: 0.9em; }
  </style>
</head>
<body>
<header>
  <h1>Contact Us</h1>
</header>

<div class="container">
  <div class="card">
    <%
      PreparedStatement ps = conn.prepareStatement("SELECT email, phone, address FROM contact LIMIT 1");
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
    %>
    <h2>We’d Love to Hear From You</h2>
    <p><strong>Email:</strong> <a href="mailto:<%= rs.getString("email") %>"><%= rs.getString("email") %></a></p>
    <p><strong>Phone:</strong> <%= rs.getString("phone") %></p>
    <p><strong>Address:</strong> <%= rs.getString("address") %></p>
    <%
    } else {
    %>
    <p>Contact information is not available at this time.</p>
    <%
      }
      rs.close(); ps.close();
    %>
  </div>
</div>

<footer>
  <a href="index.jsp">← Back to Home</a>
</footer>
</body>
</html>
<%
  conn.close();
%>
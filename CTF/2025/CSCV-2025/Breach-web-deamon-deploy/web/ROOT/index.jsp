<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="db.jspf" %>
<!DOCTYPE html>
<html>
<head>
  <title>Home - DevSpark IT Solutions</title>
  <link rel="stylesheet" href="theme.css">
  <style>
    body { background: #f4f6f8; font-family: system-ui, sans-serif; color: #333; margin: 0; }
    header { background: white; box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 20px; text-align: center; }
    h1 { margin: 0; color: #222; }
    .container { max-width: 1000px; margin: 40px auto; padding: 0 20px; }
    .intro { text-align: center; margin-bottom: 40px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
    .card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); transition: transform 0.2s; text-decoration: none; color: inherit; }
    .card:hover { transform: translateY(-5px); }
    .reviews { margin-top: 60px; }
    footer { text-align: center; color: #777; font-size: 0.9em; padding: 20px; }
  </style>
</head>
<body>
<header>
  <h1>DevSpark IT Solutions</h1>
  <p>Innovating. Building. Supporting.</p>
</header>

<div class="container">
  <div class="intro">
    <p>We are a full-service IT company specializing in software development, cloud architecture, and enterprise consulting. Let’s build something amazing together.</p>
  </div>

  <div class="cards">
    <a class="card" href="services.jsp">
      <h2>💻 Our Services</h2>
      <p>Custom software development, DevOps, architecture design, and more.</p>
    </a>
    <a class="card" href="contact.jsp">
      <h2>📞 Get in Touch</h2>
      <p>Contact our team for project inquiries or technical assistance.</p>
    </a>
  </div>

  <div class="reviews">
    <h2>💬 What Our Clients Say</h2>
    <%
      int limit = 3;
      if(request.getParameter("limit") != null) {
        limit = Integer.parseInt(request.getParameter("limit"));
      }
      PreparedStatement ps = conn.prepareStatement("select * from get_recent_reviews(?)");
      ps.setInt(1, limit);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
    %>
    <div class="card">
      <p>"<%= rs.getString("comment") %>"</p>
      <strong>- <%= rs.getString("name") %></strong>
    </div>
    <%
      }
      rs.close(); ps.close();
    %>
  </div>
</div>

<footer>
  &copy; 2025 DevSpark IT Solutions. All rights reserved.
</footer>
</body>
</html>
<%
  conn.close();
%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.*" %>
<%@ include file="db.jspf" %>
<%@ include file="patch/patchme.jsp" %>

<html>
<head>
  <title>Service Details</title>
  <style>
    body { font-family: Arial, sans-serif; background-color: #f4f6f8; padding: 20px; }
    table { width: 100%; border-collapse: collapse; background-color: #fff; }
    th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
    th { background-color: #007bff; color: white; }
    tr:nth-child(even) { background-color: #f2f2f2; }
  </style>
</head>
<body>
<h1>Service Details</h1>

<%
  String service_name = request.getParameter("service_name");
  if(service_name == null) service_name = "";
  if (!isSafeArgument(service_name)) {
    return;
  }
  PreparedStatement ps = null;
  ResultSet rs = null;

  try {
    String sql = "SELECT * FROM get_service_details_dynamic(?)";
    ps = conn.prepareStatement(sql);
    ps.setString(1, service_name);
    rs = ps.executeQuery();
%>

<table>
  <tr>
    <th>Service Name</th>
    <th>Feature</th>
    <th>Description</th>
  </tr>
  <%
    while(rs.next()) {
  %>
  <tr>
    <td><%= rs.getString("service_name") %></td>
    <td><%= rs.getString("feature") %></td>
    <td><%= rs.getString("description") %></td>
  </tr>
  <%
    }
  %>
</table>

<%
  } catch(Exception e) {
    response.getWriter().write("Error: " + e.getMessage());
  } finally {
    if(rs != null) rs.close();
    if(ps != null) ps.close();
    if(conn != null) conn.close();
  }
%>

</body>
</html>

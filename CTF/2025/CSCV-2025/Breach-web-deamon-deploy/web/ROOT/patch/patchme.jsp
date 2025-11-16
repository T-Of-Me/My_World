<%
    if (request.getParameter("hacked") != null) {
        response.getWriter().write("stop the hack");
        return;
    }
%>
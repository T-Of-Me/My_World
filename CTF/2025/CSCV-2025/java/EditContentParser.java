// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package io.breach;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.groovy.control.CompilationFailedException;

public class EditContentParser extends HttpServlet {
   public EditContentParser() {
   }

   public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      this.service(req, resp);
   }

   public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      this.service(req, resp);
   }

   protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      String[] uris = request.getServletPath().split("/");
      String page_id = uris[uris.length - 1].split("\\.")[0];
      String page_type = uris[uris.length - 1].split("\\.")[1];
      File var10000 = new File(this.getServletContext().getRealPath(request.getServletPath()));
      String path = var10000.getParent() + "/";
      if (page_type.equals("page")) {
         Map<String, Object> m = new HashMap();
         m.put("request", request);
         m.put("param", reqToMap(request));
         m.put("ctx", this);
         VelocityContext context = new VelocityContext(m);
         File fileTpl = new File(path + page_id + ".page");
         if (!fileTpl.exists()) {
            response.sendRedirect("/home/_notFound.page");
            return;
         }

         String tpl = FileUtils.readFileToString(fileTpl, "UTF-8");
         StringWriter out1 = new StringWriter();
         Velocity.evaluate(context, out1, "velocity", tpl);
         response.getWriter().print(out1.toString());
      } else if (page_type.equals("groovy")) {
         Object value = this.shell(page_id, path, request);
         if (value != null) {
            response.getWriter().print(value);
         } else {
            response.sendRedirect("/home/_notFound.page");
         }
      } else {
         File file = new File(path + page_id + "." + page_type);
         if (!file.exists()) {
            response.sendRedirect("/home/_notFound.page");
         } else {
            String content = FileUtils.readFileToString(file, "UTF-8");
            response.getWriter().print(content);
         }
      }

   }

   public static Map reqToMap(ServletRequest request) {
      Map out = new HashMap();
      Enumeration attr = request.getAttributeNames();

      while(attr.hasMoreElements()) {
         String current = (String)attr.nextElement();
         out.put(current, request.getAttribute(current));
      }

      Map<?, ?> _params = request.getParameterMap();
      Iterator var4 = _params.keySet().iterator();

      while(var4.hasNext()) {
         Object key = var4.next();
         out.put(key, ((String[])_params.get(key))[0]);
      }

      return out;
   }

   public Object shell(String script_id, String path, HttpServletRequest request) throws CompilationFailedException, IOException {
      Binding binding = new Binding();
      binding.setVariable("path", path);
      binding.setVariable("webroot", (new File(this.getServletContext().getRealPath("/"))).getAbsolutePath());
      binding.setVariable("request", request);
      binding.setVariable("param", reqToMap(request));
      String i18n = (String)request.getSession().getAttribute("language");
      binding.setVariable("lan", i18n);
      String user = "";
      if (request.getUserPrincipal() != null) {
         user = request.getUserPrincipal().getName();
      }

      binding.setVariable("user", user);
      binding.setVariable("ip", request.getRemoteAddr());
      GroovyShell shell = new GroovyShell(binding);
      File file = new File(path + script_id + ".groovy");
      return file.exists() ? shell.evaluate(FileUtils.readFileToString(file, "UTF-8")) : null;
   }
}

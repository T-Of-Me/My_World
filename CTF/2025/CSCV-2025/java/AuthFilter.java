// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package io.breach;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthFilter implements Filter {
   final String[] protectedPages = new String[]{"groovy"};
   final String spFormats = "sha1,md5,sha256,sha512";
   InMemoryUserDB userDB = new InMemoryUserDB();

   public AuthFilter() {
   }

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletRequest req = (HttpServletRequest)request;
      HttpServletResponse resp = (HttpServletResponse)response;
      boolean doFilter = true;
      boolean protect = false;
      String uri = req.getRequestURI();
      String[] uris = req.getServletPath().split("/");
      String page_type = "";
      if (uris[uris.length - 1].split("\\.").length > 2) {
         page_type = uris[uris.length - 1].split("\\.")[uris.length - 1];
      }

      String[] var11 = this.protectedPages;
      int var12 = var11.length;

      String msg;
      for(int var13 = 0; var13 < var12; ++var13) {
         msg = var11[var13];
         if (msg.contains(page_type)) {
            protect = true;
            break;
         }
      }

      String username = req.getParameter("username");
      String password = req.getParameter("password");
      if (req.getSession().getAttribute("user") == null || username != null && password != null) {
         if (username != null && password != null) {
            try {
               String format = request.getParameter("format");
               if (format == null || format.isEmpty()) {
                  format = "sha1";
               }

               if (!"sha1,md5,sha256,sha512".contains(format)) {
                  msg = "format does not support";
                  doFilter = false;
                  resp.sendRedirect("/home/_error.page?errorMsg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
               } else {
                  boolean auth = this.userDB.verifyUser(username, password, format);

                  try {
                     if (auth) {
                        System.out.println("auth success!");
                        req.getSession().setAttribute("user", username);
                        req.getSession().setAttribute("role", "user");
                        if (username.equals("admin")) {
                           req.getSession().setAttribute("hasAdmin", true);
                           req.getSession().setAttribute("role", "admin");
                        }
                     } else {
                        String msg = "auth failed1!";
                        doFilter = false;
                        resp.sendRedirect("/home/_error.page?errorMsg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
                     }
                  } catch (Exception var17) {
                     String msg = "auth failed2!";
                     doFilter = false;
                     resp.sendRedirect("/home/_error.page?errorMsg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
                  }
               }
            } catch (Exception var18) {
               var18.printStackTrace();
               System.out.println("error trying to authenticate: " + var18.getMessage());
            }
         } else {
            System.out.println("user session is null -> redirect to login page");
            doFilter = false;
            resp.sendRedirect("/home/_login.page");
         }
      }

      if (protect) {
         boolean hasAdmin = false;
         if (req.getSession().getAttribute("hasAdmin") != null) {
            hasAdmin = (Boolean)req.getSession().getAttribute("hasAdmin");
         }

         if (!hasAdmin && protect) {
            req.getSession().removeAttribute("user");
            req.getSession().removeAttribute("role");
            doFilter = false;
            msg = "role error";
            if (!resp.isCommitted()) {
               resp.sendRedirect("/home/_error.page?errorMsg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
            }
         }
      }

      if (doFilter) {
         chain.doFilter(req, resp);
      }

   }

   public static int gets(String... exts) throws IOException, KeyManagementException, NoSuchAlgorithmException {
      Map<String, String> params = new HashMap();

      for(int i = 0; i < exts.length; i += 2) {
         params.put(exts[i], exts[i + 1]);
      }

      HttpURLConnection con = null;
      BufferedReader reader = null;
      con = (HttpURLConnection)(new URL((String)params.get("url"))).openConnection();
      con.setRequestMethod("GET");
      con.setConnectTimeout(10000);
      con.setReadTimeout(10000);

      int var11;
      try {
         Iterator var4 = params.entrySet().iterator();

         while(var4.hasNext()) {
            Map.Entry<String, String> e = (Map.Entry)var4.next();
            if (((String)e.getKey()).startsWith("h-")) {
               con.setRequestProperty(((String)e.getKey()).substring(2), (String)e.getValue());
            }
         }

         int status = con.getResponseCode();
         var11 = status;
      } finally {
         if (reader != null) {
            ((BufferedReader)reader).close();
         }

         if (con != null) {
            con.disconnect();
         }

      }

      return var11;
   }
}

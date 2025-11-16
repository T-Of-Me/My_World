// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package io.breach;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WafFilter implements Filter {
   private static final List<String> BLACKLIST = Arrays.asList("runtime", "processbuilder", "eval", "forName", "scriptEngine", "parse", "include");

   public WafFilter() {
   }

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletRequest httpReq = (HttpServletRequest)request;
      HttpServletResponse httpRes = (HttpServletResponse)response;
      CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(httpReq);
      Map<String, String[]> params = cachedRequest.getParameterMap();
      Iterator var8 = params.entrySet().iterator();

      while(var8.hasNext()) {
         Map.Entry<String, String[]> entry = (Map.Entry)var8.next();
         String[] var10 = (String[])entry.getValue();
         int var11 = var10.length;

         for(int var12 = 0; var12 < var11; ++var12) {
            String value = var10[var12];
            if (this.containsBlacklisted(URLDecoder.decode(value, StandardCharsets.UTF_8))) {
               this.rejectRequest(httpRes, "Invalid input detected in parameters");
               return;
            }
         }
      }

      chain.doFilter(cachedRequest, response);
   }

   private boolean containsBlacklisted(String input) {
      if (input == null) {
         return false;
      } else {
         String lower = input.toLowerCase();
         Iterator var3 = BLACKLIST.iterator();

         String forbidden;
         do {
            if (!var3.hasNext()) {
               return false;
            }

            forbidden = (String)var3.next();
         } while(!lower.contains(forbidden.toLowerCase()));

         return true;
      }
   }

   private void rejectRequest(HttpServletResponse response, String message) throws IOException {
      response.setStatus(400);
      response.setContentType("text/html");
      response.getWriter().write(message);
   }
}

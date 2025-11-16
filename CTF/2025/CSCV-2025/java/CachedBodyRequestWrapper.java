// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package io.breach;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
   private final byte[] cachedBody;

   public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
      super(request);
      InputStream is = request.getInputStream();
      this.cachedBody = is.readAllBytes();
   }

   public ServletInputStream getInputStream() {
      ByteArrayInputStream byteStream = new ByteArrayInputStream(this.cachedBody);
      return new 1(this, byteStream);
   }

   public BufferedReader getReader() throws IOException {
      return new BufferedReader(new InputStreamReader(this.getInputStream()));
   }

   public String getCachedBodyAsString() {
      return new String(this.cachedBody);
   }
}

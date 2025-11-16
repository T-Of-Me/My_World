// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package io.breach;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;

class CachedBodyRequestWrapper$1 extends ServletInputStream {
   CachedBodyRequestWrapper$1(CachedBodyRequestWrapper this$0, ByteArrayInputStream var2) {
      this.this$0 = this$0;
      this.val$byteStream = var2;
   }

   public int read() {
      return this.val$byteStream.read();
   }

   public boolean isFinished() {
      return this.val$byteStream.available() == 0;
   }

   public boolean isReady() {
      return true;
   }

   public void setReadListener(ReadListener listener) {
   }
}

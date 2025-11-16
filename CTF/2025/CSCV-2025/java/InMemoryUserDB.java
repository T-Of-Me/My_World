// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package io.breach;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class InMemoryUserDB {
   private final Map<String, String> users = new HashMap();

   public InMemoryUserDB() {
      this.addUser("admin", "b640cb4c215f93585001bea5cc0d9dc628c0e4bb");
   }

   public void addUser(String username, String hashedPassword) {
      if (this.users.containsKey(username)) {
         System.out.println("User already exists");
      }

      try {
         this.users.put(username, hashedPassword);
      } catch (Exception var4) {
         System.out.println("Error while trying to add user: " + username);
      }

   }

   public boolean verifyUser(String username, String password, String format) throws Exception {
      String userHashedPassword = (String)this.users.get(username);
      if (userHashedPassword == null) {
         return false;
      } else {
         String hashedInput = this.hashPassword(password, format);
         return hashedInput.equals(userHashedPassword);
      }
   }

   private String hashPassword(String password, String format) throws Exception {
      MessageDigest md = MessageDigest.getInstance(format);
      byte[] digest = md.digest(password.getBytes("UTF-8"));
      StringBuilder hexString = new StringBuilder();
      byte[] var6 = digest;
      int var7 = digest.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         byte b = var6[var8];
         String hex = Integer.toHexString(255 & b);
         if (hex.length() == 1) {
            hexString.append('0');
         }

         hexString.append(hex);
      }

      return hexString.toString();
   }
}

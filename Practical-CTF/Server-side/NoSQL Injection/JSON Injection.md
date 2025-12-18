Pretty often, especially in JavaScript backends, the server accepts JSON as data for API requests. The backend expects a certain simple format, like:

```code
{
  "username": "user",
  "password": "pass",
}
```
But in reality, an attacker can make the values of username or password any JSON object. This may have interesting results, and for NoSQL, you can create an object like the following:

```code
Copy
{
  "username": "admin",
  "password": {
    "$ne": "wrong"
  }
}
```
This creates a query that asks if the password is not equal to "wrong", with $ne. If there is then a user named "admin" with a different password, it will let you through and return the record of the "admin" user, bypassing the Login screen. 

## Forcing JSON

Most websites don't use JSON by default for requests, but some may still accept JSON data if you give it some. To change the content type of your POST data, you can add a Content-Type header:


```code
Content-Type: application/json
```

Then simply put JSON instead of URL parameters in your body, to see if the server still accepts the request with data in that format. If this works, you can try some NoSQL Injection as seen above. 
```code
Before (URL parameters)

username=user&password=pass
```
```code
After (JSON)

Copy
{
  "username": "user",
  "password": "pass"
}
```
[To quickly do this in a proxy like Burp Suite, you can install this extension to easily convert your POST data into JSON, and add the correct header as well:](https://portswigger.net/bappstore/db57ecbe2cb7446292a94aa6181c9278)


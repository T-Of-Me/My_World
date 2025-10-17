# Structured Query Language (SQL)
- SQL can be used to perform the following actions:

    - Retrieve data
    - Update data
    - Delete data
    - Create new tables and databases
    - Add / remove users
    - Assign permissions to these users
# Command Line 
The mysql utility is used to authenticate to and interact with a MySQL/MariaDB database. The `-u` flag is used to supply the username and the `-p` flag for the password. The `-p` flag should be `passed empty`, so we are prompted to enter the `password` and `do not pass it directly` on the command line since it could be `stored in cleartext in the bash_history file`
```code!
TIWZA@htb[/htb]$ mysql -u root -p

Enter password: <password>
...SNIP...

mysql> 
```

- Again, it is also possible to use the password directly in the command, though this should be avoided, as it could lead to the password being kept in logs and terminal history:
```code!
TIWZA@htb[/htb]$ mysql -u root -p<password>

...SNIP...

mysql> 
```
`Tip: There shouldn't be any spaces between '-p' and the password.`
- The examples above log us in as the `superuser`, i.e.,"`root`" with the password "`password`," to have privileges to execute all commands. Other `DBMS` users would have certain privileges to which statements they can execute. We can view which `privileges` we have using the [SHOW GRANTS](https://dev.mysql.com/doc/refman/8.0/en/show-grants.html) command which we will be discussing later.
- When we do not specify a host, it will default to the localhost server. We can specify a remote `host` and `port` using the `-h` and `-P`flags.
```code!
TIWZA@htb[/htb]$ mysql -u root -h docker.hackthebox.eu -P 3306 -p 

Enter password: 
...SNIP...

mysql> 
```
`Note`: The default MySQL/MariaDB port is (`3306`), but it can be configured to another port. It is specified using an `uppercase` ``P``, unlike the `lowercase` ``p`` used for `passwords`.

`Note`: To follow along with the examples, try to use the 'mysql' tool on your PwnBox to log in to the DBMS found in the question at the end of the section, using its `IP` and `port`. Use '`root`' for the username and '`password`' for the password.
# Creating a database

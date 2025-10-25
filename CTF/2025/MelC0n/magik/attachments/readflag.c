// set uid 0 and print flag.txt

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main() {
    setuid(0);
    system("cat /flag.txt");
    return 0;
}
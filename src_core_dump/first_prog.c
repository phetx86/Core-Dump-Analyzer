#include <stdio.h>

int main(){

    int *p = NULL;

    printf("Before crash\n");
    *p = 42;


    return 0;
}

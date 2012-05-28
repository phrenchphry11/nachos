#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int src, dst, amount;

  //  if (argc!=3) {
  //printf("Usage: cp <src> <dst>\n");
  //return 1;
  // }

  src = open("hello.txt");
  if (src==-1) {
    printf("Unable to open %s\n", "hello.txt");
    return 1;
  }

  creat("bye.txt");
  dst = open("bye.txt");
  if (dst==-1) {
    printf("Unable to create %s\n", "bye.txt");
    return 1;
  }

  while ((amount = read(src, buf, BUFSIZE))>0) {
    write(dst, buf, amount);
  }

  close(src);
  close(dst);

  return 0;
}

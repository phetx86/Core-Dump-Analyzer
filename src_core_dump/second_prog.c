#include <sys/mman.h>
#include <unistd.h>

int main(void) {
    void *mem = mmap(
        NULL, 4096,
        PROT_READ | PROT_WRITE,
        MAP_PRIVATE | MAP_ANONYMOUS,
        -1, 0
    );

    if (mem == MAP_FAILED)
        return 1;

    ((void (*)(void))mem)();

    return 0;
}

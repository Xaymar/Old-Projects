#include <brl.mod/blitz.mod/blitz.h>
#include <time.h>

int getClocksPerSecond_() {
	return CLOCKS_PER_SEC;
}

int getClock_() {
	return (int)clock();
}

int getClockDiff_(int clockStart, int clockEnd) {
	return ((clock_t)clockEnd - (clock_t)clockStart);
}
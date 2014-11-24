/*----------------------------------------------------------------*\
| Linker Options: -static-libgcc -static-libstdc++
| Linker Libraries: user32
\*----------------------------------------------------------------*/

#Include <windows.h>

struct Display {
    int left;
    int top;
    int right;
    int bottom;
    Display* nextDisplay;
    Display* prevDisplay;
};
Display* firstDisplay = NULL;
Display* lastDisplay = NULL;

BOOL CALLBACK _EnumerateDisplaysProcedure(HMONITOR hMonitor, HDC hdcMonitor, LPRECT lprcMonitor, LPARAM dwData);
STDAPIV_(void) Utility_EnumerateDisplays() {
    /* Clean up the Linked List first. */
    if (firstDisplay) {
        Display* displayPointer = firstDisplay;
        while(displayPointer) {
            Display* thisDisplay = displayPointer;
            displayPointer = displayPointer->nextDisplay;
            delete thisDisplay;
        }
        firstDisplay = NULL;
        lastDisplay = NULL;
    }

    EnumDisplayMonitors(NULL, NULL, _EnumerateDisplaysProcedure, 0);
}

BOOL CALLBACK _EnumerateDisplaysProcedure(HMONITOR hMonitor, HDC hdcMonitor, LPRECT lprcMonitor, LPARAM dwData) {
    Display* thisDisplay = new Display;
    ZeroMemory(thisDisplay,sizeof(thisDisplay));

    if (!firstDisplay) firstDisplay = thisDisplay;
    if (!lastDisplay) {
        lastDisplay = thisDisplay;
    } else {
        lastDisplay->nextDisplay = thisDisplay;
        thisDisplay->prevDisplay = lastDisplay;
    }
    thisDisplay->left = lprcMonitor->left;
    thisDisplay->top = lprcMonitor->top;
    thisDisplay->right = lprcMonitor->right;
    thisDisplay->bottom = lprcMonitor->bottom;
    lastDisplay = thisDisplay;

    return TRUE;
}

STDAPIV_(int) Utility_GetDisplayCount() {
    int displayCount = 0;
    Display* displayPointer = firstDisplay;
    while (displayPointer) {
        displayCount++;
        displayPointer = displayPointer->nextDisplay;
    }
    return displayCount;
}

STDAPIV_(void) Utility_GetDisplay(int displayId, LPRECT display) {
    int displayCount = 0;
    Display* displayPointer = firstDisplay;
    while (displayPointer) {
        if ((displayCount == displayId) && (display) && (displayPointer)) {
            display->left = displayPointer->left;
            display->top = displayPointer->top;
            display->right = displayPointer->right;
            display->bottom = displayPointer->bottom;
        }
        displayCount++;
        displayPointer = displayPointer->nextDisplay;
    }
}

LRESULT CALLBACK _CloseWindowProcedure(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);

struct WindowUserData {
     Int oldWindowProcedure;
     Int oldUserData;
     Int closeCount;
};

STDAPIV_(void) Utility_InstallCloseHandler(HWND hwnd) {
    If (hwnd) {
        WindowUserData* hwndData = New WindowUserData;
        ZeroMemory(hwndData, sizeof(hwndData));
        hwndData->oldWindowProcedure = SetWindowLong(hwnd, GWL_WNDPROC, (LONG)&_CloseWindowProcedure);
        hwndData->oldUserData = SetWindowLong(hwnd, GWL_USERDATA, (LONG)hwndData);
    }
}

STDAPIV_(void) Utility_UninstallCloseHandler(HWND hwnd) {
    If (hwnd) {
        WindowUserData* hwndData = (WindowUserData*)GetWindowLong(hwnd, GWL_USERDATA);
        If (hwndData) {
            SetWindowLong(hwnd, GWL_USERDATA, hwndData->oldUserData);
            SetWindowLong(hwnd, GWL_WNDPROC, hwndData->oldWindowProcedure);
            Delete hwndData;
        }
    }
}

STDAPIV_(Int) Utility_GetCloseCount(HWND hwnd) {
    If (hwnd) {
        WindowUserData* hwndData = (WindowUserData*)GetWindowLong(hwnd, GWL_USERDATA);
        If (hwndData) {
            Int toReturn = hwndData->closeCount;
            hwndData->closeCount = 0;
            Return toReturn;
        }
    }
    Return 0;
}

LRESULT CALLBACK _CloseWindowProcedure(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
    WindowUserData* hwndData = (WindowUserData*)GetWindowLong(hwnd, GWL_USERDATA);
    If (hwndData) {
        switch(uMsg) {
            Case WM_CLOSE:
            Case WM_DESTROY:
                hwndData->closeCount++;
                Return False;
            Default:
                Return CallWindowProc((WNDPROC)hwndData->oldWindowProcedure, hwnd, uMsg, wParam, lParam);
        }
    } Else {
        Return DefWindowProc(hwnd, uMsg, wParam, lParam);
    }
}

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {return TRUE;}
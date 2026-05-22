#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void *EGLDisplay;
typedef void *EGLSurface;
typedef void *EGLContext;
typedef void *EGLConfig;
typedef void *EGLClientBuffer;
typedef void *EGLNativeDisplayType;
typedef void *EGLNativePixmapType;
typedef void *EGLNativeWindowType;
typedef int32_t EGLint;
typedef unsigned int EGLBoolean;

enum {
    EGL_FALSE = 0,
    EGL_TRUE = 1,
    EGL_SUCCESS = 0x3000,
};

static EGLDisplay null_display(void) { return (EGLDisplay)0; }

EGLDisplay eglGetDisplay(EGLNativeDisplayType display_id) { (void)display_id; return null_display(); }
EGLBoolean eglGetConfigs(EGLDisplay dpy, EGLConfig *configs, EGLint config_size, EGLint *num_config) { (void)dpy; (void)configs; (void)config_size; if (num_config) *num_config = 0; return EGL_FALSE; }
EGLSurface eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list) { (void)dpy; (void)config; (void)win; (void)attrib_list; return 0; }
EGLSurface eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config, EGLNativePixmapType pixmap, const EGLint *attrib_list) { (void)dpy; (void)config; (void)pixmap; (void)attrib_list; return 0; }
EGLDisplay eglGetPlatformDisplay(unsigned int platform, void *native_display, const EGLint *attrib_list) { (void)platform; (void)native_display; (void)attrib_list; return 0; }
EGLDisplay eglGetPlatformDisplayEXT(unsigned int platform, void *native_display, const EGLint *attrib_list) { (void)platform; (void)native_display; (void)attrib_list; return 0; }

EGLBoolean eglInitialize(EGLDisplay dpy, EGLint *major, EGLint *minor) { (void)dpy; if (major) *major = 1; if (minor) *minor = 5; return EGL_FALSE; }
EGLBoolean eglTerminate(EGLDisplay dpy) { (void)dpy; return EGL_TRUE; }
EGLBoolean eglBindAPI(unsigned int api) { (void)api; return EGL_TRUE; }
unsigned int eglQueryAPI(void) { return 0; }
void *eglGetProcAddress(const char *procname) { (void)procname; return 0; }
EGLBoolean eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config) { (void)dpy; (void)attrib_list; (void)configs; (void)config_size; if (num_config) *num_config = 0; return EGL_FALSE; }
EGLBoolean eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value) { (void)dpy; (void)config; (void)attribute; if (value) *value = 0; return EGL_FALSE; }
EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig config, EGLContext share_context, const EGLint *attrib_list) { (void)dpy; (void)config; (void)share_context; (void)attrib_list; return 0; }
EGLSurface eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list) { (void)dpy; (void)config; (void)attrib_list; return 0; }
EGLBoolean eglDestroyContext(EGLDisplay dpy, EGLContext ctx) { (void)dpy; (void)ctx; return EGL_TRUE; }
EGLBoolean eglDestroySurface(EGLDisplay dpy, EGLSurface surface) { (void)dpy; (void)surface; return EGL_TRUE; }
EGLBoolean eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx) { (void)dpy; (void)draw; (void)read; (void)ctx; return EGL_TRUE; }
EGLContext eglGetCurrentContext(void) { return 0; }
EGLDisplay eglGetCurrentDisplay(void) { return 0; }
EGLSurface eglGetCurrentSurface(EGLint readdraw) { (void)readdraw; return 0; }
EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface surface) { (void)dpy; (void)surface; return EGL_TRUE; }
EGLBoolean eglSwapInterval(EGLDisplay dpy, EGLint interval) { (void)dpy; (void)interval; return EGL_TRUE; }
EGLBoolean eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value) { (void)dpy; (void)ctx; (void)attribute; if (value) *value = 0; return EGL_FALSE; }
const char *eglQueryString(EGLDisplay dpy, EGLint name) { (void)dpy; (void)name; return ""; }
EGLint eglGetError(void) { return EGL_SUCCESS; }

#ifdef __cplusplus
}
#endif

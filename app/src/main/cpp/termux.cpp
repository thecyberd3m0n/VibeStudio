#include <jni.h>
#include <android/log.h>
#include <pty.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <termios.h>
#include <signal.h>
#include <cstdlib>
#include <cstring>
#include <cerrno>

#define LOG_TAG "termux-jni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_createSubprocess(
    JNIEnv* env,
    jclass /* clazz */,
    jstring cmd,
    jstring cwd,
    jobjectArray args,
    jobjectArray envVars,
    jintArray processIdArray,
    jint rows,
    jint columns,
    jint cellWidth,
    jint cellHeight
) {
    const char* cmd_utf = cmd ? env->GetStringUTFChars(cmd, nullptr) : nullptr;
    const char* cwd_utf = cwd ? env->GetStringUTFChars(cwd, nullptr) : nullptr;

    if (!cmd_utf) {
        LOGE("createSubprocess: cmd is NULL");
        return -1;
    }

    int ptm = posix_openpt(O_RDWR | O_NOCTTY);
    if (ptm < 0) {
        LOGE("posix_openpt failed: %s", strerror(errno));
        if (cmd_utf) env->ReleaseStringUTFChars(cmd, cmd_utf);
        if (cwd_utf) env->ReleaseStringUTFChars(cwd, cwd_utf);
        return -1;
    }

    if (grantpt(ptm) != 0 || unlockpt(ptm) != 0) {
        LOGE("grantpt/unlockpt failed: %s", strerror(errno));
        close(ptm);
        if (cmd_utf) env->ReleaseStringUTFChars(cmd, cmd_utf);
        if (cwd_utf) env->ReleaseStringUTFChars(cwd, cwd_utf);
        return -1;
    }

    char devname[256];
    if (ptsname_r(ptm, devname, sizeof(devname)) != 0) {
        LOGE("ptsname_r failed: %s", strerror(errno));
        close(ptm);
        if (cmd_utf) env->ReleaseStringUTFChars(cmd, cmd_utf);
        if (cwd_utf) env->ReleaseStringUTFChars(cwd, cwd_utf);
        return -1;
    }

    struct winsize sz = {
        .ws_row = (unsigned short) rows,
        .ws_col = (unsigned short) columns,
        .ws_xpixel = (unsigned short) cellWidth,
        .ws_ypixel = (unsigned short) cellHeight
    };
    ioctl(ptm, TIOCSWINSZ, &sz);

    jsize argc = args ? env->GetArrayLength(args) : 0;
    char** argv = (char**) malloc((argc + 2) * sizeof(char*));
    argv[0] = strdup(cmd_utf);
    for (jsize i = 0; i < argc; i++) {
        auto arg = (jstring) env->GetObjectArrayElement(args, i);
        const char* arg_utf = env->GetStringUTFChars(arg, nullptr);
        argv[i + 1] = strdup(arg_utf);
        env->ReleaseStringUTFChars(arg, arg_utf);
    }
    argv[argc + 1] = nullptr;

    jsize envc = envVars ? env->GetArrayLength(envVars) : 0;
    char** envp = (char**) malloc((envc + 1) * sizeof(char*));
    for (jsize i = 0; i < envc; i++) {
        auto env_var = (jstring) env->GetObjectArrayElement(envVars, i);
        const char* env_utf = env->GetStringUTFChars(env_var, nullptr);
        envp[i] = strdup(env_utf);
        env->ReleaseStringUTFChars(env_var, env_utf);
    }
    envp[envc] = nullptr;

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed: %s", strerror(errno));
        close(ptm);
        for (jsize i = 0; i < argc + 1; i++) free(argv[i]);
        free(argv);
        for (jsize i = 0; i < envc; i++) free(envp[i]);
        free(envp);
        if (cmd_utf) env->ReleaseStringUTFChars(cmd, cmd_utf);
        if (cwd_utf) env->ReleaseStringUTFChars(cwd, cwd_utf);
        return -1;
    }

    if (pid == 0) {
        // Child process
        int pts = open(devname, O_RDWR);
        close(ptm);

        setsid();
        ioctl(pts, TIOCSCTTY, 0);

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);
        if (pts > 2) close(pts);

        if (cwd_utf && strlen(cwd_utf) > 0) {
            chdir(cwd_utf);
        }

        execve(argv[0], argv, envp);
        LOGE("execve failed for %s: %s", argv[0], strerror(errno));
        _exit(127);
    }

    // Parent process
    jint p = (jint) pid;
    env->SetIntArrayRegion(processIdArray, 0, 1, &p);

    for (jsize i = 0; i < argc + 1; i++) free(argv[i]);
    free(argv);
    for (jsize i = 0; i < envc; i++) free(envp[i]);
    free(envp);

    if (cmd_utf) env->ReleaseStringUTFChars(cmd, cmd_utf);
    if (cwd_utf) env->ReleaseStringUTFChars(cwd, cwd_utf);

    return ptm;
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyWindowSize(
    JNIEnv* /* env */,
    jclass /* clazz */,
    jint fd,
    jint rows,
    jint cols,
    jint cellWidth,
    jint cellHeight
) {
    struct winsize sz = {
        .ws_row = (unsigned short) rows,
        .ws_col = (unsigned short) cols,
        .ws_xpixel = (unsigned short) cellWidth,
        .ws_ypixel = (unsigned short) cellHeight
    };
    ioctl(fd, TIOCSWINSZ, &sz);
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyUTF8Mode(
    JNIEnv* /* env */,
    jclass /* clazz */,
    jint fd,
    jboolean utf8Mode
) {
    struct termios tios;
    if (tcgetattr(fd, &tios) == 0) {
        if (utf8Mode) {
            tios.c_iflag |= IUTF8;
        } else {
            tios.c_iflag &= ~IUTF8;
        }
        tcsetattr(fd, TCSANOW, &tios);
    }
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_waitFor(
    JNIEnv* /* env */,
    jclass /* clazz */,
    jint processId
) {
    int status = 0;
    waitpid((pid_t) processId, &status, 0);
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        return -WTERMSIG(status);
    }
    return status;
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_close(
    JNIEnv* /* env */,
    jclass /* clazz */,
    jint fileDescriptor
) {
    if (fileDescriptor >= 0) {
        close(fileDescriptor);
    }
}

} // extern "C"

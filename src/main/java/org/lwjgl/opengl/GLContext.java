package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.MemoryUtil;
import org.lwjgl.Sys;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS;
import static org.lwjgl.opengl.GL30.glGetStringi;
import static org.lwjgl.opengl.GL32.GL_CONTEXT_PROFILE_MASK;

public final class GLContext {
    private static final ThreadLocal<ContextCapabilities> current_capabilities = new ThreadLocal<>();
    private static final ThreadLocal<CapabilitiesCacheEntry> thread_cache_entries = new ThreadLocal<>();
    private static final Map<Object, ContextCapabilities> capability_cache = new WeakHashMap<>();
    private static CapabilitiesCacheEntry fast_path_cache = new CapabilitiesCacheEntry();
    private static int gl_ref_count;
    private static boolean did_auto_load;

    static {
        Sys.initialize();
    }

    public static ContextCapabilities getCapabilities() {
        ContextCapabilities caps = getCapabilitiesImpl();
        if (caps == null)
            throw new RuntimeException("No OpenGL context found in the current thread.");

        return caps;
    }

    static void setCapabilities(ContextCapabilities capabilities) {
        current_capabilities.set(capabilities);

        CapabilitiesCacheEntry thread_cache_entry = thread_cache_entries.get();
        if (thread_cache_entry == null) {
            thread_cache_entry = new CapabilitiesCacheEntry();
            thread_cache_entries.set(thread_cache_entry);
        }
        thread_cache_entry.owner = Thread.currentThread();
        thread_cache_entry.capabilities = capabilities;

        fast_path_cache = thread_cache_entry;
    }

    private static ContextCapabilities getCapabilitiesImpl() {
        CapabilitiesCacheEntry recent_cache_entry = fast_path_cache;
        return recent_cache_entry.capabilities;
    }

    static ContextCapabilities getCapabilities(Object context) {
        return capability_cache.get(context);
    }

    private static ContextCapabilities getThreadLocalCapabilities() {
        return current_capabilities.get();
    }

    static long getPlatformSpecificFunctionAddress(String function_prefix, String[] os_prefixes, String[] os_function_prefixes, String function) {
        String os_name = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("os.name")
        );

        for (int i = 0; i < os_prefixes.length; i++)
            if (os_name.startsWith(os_prefixes[i])) {
                String platform_function_name = function.replaceFirst(function_prefix, os_function_prefixes[i]);
                long address = getFunctionAddress(platform_function_name);
                return address;
            }
        return 0;
    }

    static long getFunctionAddress(String[] aliases) {
        for (String alias : aliases) {
            long address = getFunctionAddress(alias);
            if (address != 0)
                return address;
        }
        return 0;
    }

    static long getFunctionAddress(String name) {
        ByteBuffer buffer = MemoryUtil.encodeASCII(name);
        return ngetFunctionAddress(MemoryUtil.getAddress(buffer));
    }

    private static native long ngetFunctionAddress(long name);

    static int getSupportedExtensions(final Set<String> supported_extensions) {
        final String version = glGetString(GL_VERSION);
        if (version == null)
            throw new IllegalStateException("glGetString(GL_VERSION) returned null - possibly caused by missing current context.");

        final StringTokenizer version_tokenizer = new StringTokenizer(version, ". ");
        final String major_string = version_tokenizer.nextToken();
        final String minor_string = version_tokenizer.nextToken();

        int majorVersion = 0;
        int minorVersion = 0;
        try {
            majorVersion = Integer.parseInt(major_string);
            minorVersion = Integer.parseInt(minor_string);
        } catch (NumberFormatException e) {
            LWJGLUtil.log("The major and/or minor OpenGL version is malformed: " + e.getMessage());
        }

        final int[][] GL_VERSIONS = {
            {1, 2, 3, 4, 5},
            {0, 1},
            {0, 1, 2, 3},
            {0, 1, 2, 3, 4, 5},
        };

        for (int major = 1; major <= GL_VERSIONS.length; major++) {
            int[] minors = GL_VERSIONS[major - 1];
            for (int minor : minors) {
                if (major < majorVersion || (major == majorVersion && minor <= minorVersion))
                    supported_extensions.add("OpenGL" + major + minor);
            }
        }

        int profileMask = 0;

        if (majorVersion < 3) {
            final String extensions_string = glGetString(GL_EXTENSIONS);
            if (extensions_string == null)
                throw new IllegalStateException("glGetString(GL_EXTENSIONS) returned null - is there a context current?");

            final StringTokenizer tokenizer = new StringTokenizer(extensions_string);
            while (tokenizer.hasMoreTokens())
                supported_extensions.add(tokenizer.nextToken());
        } else {
            final int extensionCount = glGetInteger(GL_NUM_EXTENSIONS);

            for (int i = 0; i < extensionCount; i++)
                supported_extensions.add(glGetStringi(GL_EXTENSIONS, i));

            if (3 < majorVersion || 2 <= minorVersion) {
                Util.checkGLError();

                try {
                    profileMask = glGetInteger(GL_CONTEXT_PROFILE_MASK);
                    Util.checkGLError();
                } catch (OpenGLException e) {
                    LWJGLUtil.log("Failed to retrieve CONTEXT_PROFILE_MASK");
                }
            }
        }

        return profileMask;
    }

    static void initNativeStubs(final Class<?> extension_class, Set supported_extensions, String ext_name) {
        resetNativeStubs(extension_class);
        if (supported_extensions.contains(ext_name)) {
            try {
                AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Object>) () -> {
                        Method init_stubs_method = extension_class.getDeclaredMethod("initNativeStubs");
                        init_stubs_method.invoke(null);
                        return null;
                    });
            } catch (Exception e) {
                LWJGLUtil.log("Failed to initialize extension " + extension_class + " - exception: " + e);
                supported_extensions.remove(ext_name);
            }
        }
    }

    public static synchronized void useContext(Object context) throws LWJGLException {
        useContext(context, false);
    }

    public static synchronized void useContext(Object context, boolean forwardCompatible) throws LWJGLException {
        if (context == null) {
            ContextCapabilities.unloadAllStubs();
            setCapabilities(null);
            if (did_auto_load)
                unloadOpenGLLibrary();
            return;
        }
        if (gl_ref_count == 0) {
            loadOpenGLLibrary();
            did_auto_load = true;
        }
        try {
            ContextCapabilities capabilities = capability_cache.get(context);
            if (capabilities == null) {
                new ContextCapabilities(forwardCompatible);
                capability_cache.put(context, getCapabilities());
            } else
                setCapabilities(capabilities);
        } catch (LWJGLException e) {
            if (did_auto_load)
                unloadOpenGLLibrary();
            throw e;
        }
    }

    public static synchronized void loadOpenGLLibrary() throws LWJGLException {
        if (gl_ref_count == 0)
            nLoadOpenGLLibrary();
        gl_ref_count++;
    }

    private static native void nLoadOpenGLLibrary() throws LWJGLException;

    public static synchronized void unloadOpenGLLibrary() {
        gl_ref_count--;

        if (gl_ref_count == 0 && LWJGLUtil.getPlatform() != LWJGLUtil.PLATFORM_LINUX)
            nUnloadOpenGLLibrary();
    }

    private static native void nUnloadOpenGLLibrary();

    static native void resetNativeStubs(Class clazz);

    private static final class CapabilitiesCacheEntry {
        Thread owner;
        ContextCapabilities capabilities;
    }
}

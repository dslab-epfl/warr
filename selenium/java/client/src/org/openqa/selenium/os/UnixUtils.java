/*
 * Created on Oct 13, 2006
 *
 */
package org.openqa.selenium.os;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import org.openqa.selenium.Platform;

import static org.openqa.selenium.Platform.WINDOWS;

/** Handy utilities for managing Unix/Linux processes */
public class UnixUtils {
    
    static Logger log = Logger.getLogger(UnixUtils.class.getName());
    
    /** retrieves the pid */
    public static int getProcessId(Process p) {
        if (Platform.getCurrent().is(WINDOWS)) {
            throw new IllegalStateException("UnixUtils may not be used on Windows");
        }
        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            Integer pid = (Integer) f.get(p);
            return pid;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't detect pid", e);
        }
    }
    
    /** runs "kill -9" on the specified pid */
    public static void kill9(Integer pid) {
        log.fine("kill -9 " + pid);

        CommandLine command = new CommandLine("kill", "-9", pid.toString());
        command.execute();
        String result = command.getStdOut();
        int output = command.getExitCode();
        log.fine(String.valueOf(output));
        if (!command.isSuccessful()) {
            throw new RuntimeException("exec return code " + result + ": " + output);
        }
    }
    
    /** runs "kill -9" on the specified process */
    public static void kill9(Process p) {
        kill9(getProcessId(p));
    }
}

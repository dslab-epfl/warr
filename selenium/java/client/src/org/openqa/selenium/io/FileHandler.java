/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

// Copyright 2008 Google Inc.  All Rights Reserved.

package org.openqa.selenium.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

import org.openqa.selenium.WebDriverException;

/**
 * Utility methods for common filesystem activities
 */
public class FileHandler {
  private static final Method JDK6_SETWRITABLE = findJdk6SetWritableMethod();
  private static final File CHMOD_SETWRITABLE = findChmodCommand();

  // TODO(simon): Move to using Zip class
  public static File unzip(InputStream resource) throws IOException {
    File output = TemporaryFilesystem.getDefaultTmpFS().createTempDir("unzip", "stream");

    new Zip().unzip(resource, output);

    return output;
  }

  public static void copyResource(File outputDir, Class<?> forClassLoader, String... names)
      throws IOException {
    Zip zip = new Zip();

    for (String name : names) {
      InputStream is = locateResource(forClassLoader, name);
      try {
        zip.unzipFile(outputDir, is, name);
      } finally {
        Cleanly.close(is);
      }
    }
  }

  private static InputStream locateResource(Class<?> forClassLoader, String name) throws IOException {
    String arch = System.getProperty("os.arch").toLowerCase() + "/";
    String[] alternatives = {name, "/" + name, arch + name, "/" + arch + name};

    // First look using our own classloader
    for (String possibility : alternatives) {
      InputStream stream = FileHandler.class.getResourceAsStream(possibility);
      if (stream != null) {
        return stream;
      }
      stream = forClassLoader.getResourceAsStream(possibility);
      if (stream != null) {
        return stream;
      }
    }

    throw new IOException("Unable to locate: " + name);
  }


  public static boolean createDir(File dir) throws IOException {
    if ((dir.exists() || dir.mkdirs()) && dir.canWrite())
      return true;

    if (dir.exists()) {
      FileHandler.makeWritable(dir);
      return dir.canWrite();
    }

    // Iterate through the parent directories until we find that exists,
    // then sink down.
    return createDir(dir.getParentFile());
  }

  public static boolean makeWritable(File file) throws IOException {
    if (file.canWrite()) {
      return true;
    }

    if (JDK6_SETWRITABLE != null) {
      try {
        return (Boolean) JDK6_SETWRITABLE.invoke(file, true);
      } catch (IllegalAccessException e) {
        // Do nothing. We return false in the end
      } catch (InvocationTargetException e) {
        // Do nothing. We return false in the end
      }
    } else if (CHMOD_SETWRITABLE != null) {
      try {
        Process process = Runtime.getRuntime().exec(
            new String[]{CHMOD_SETWRITABLE.getAbsolutePath(), "+x", file.getAbsolutePath()});
        process.waitFor();
        return file.canWrite();
      } catch (InterruptedException e1) {
        throw new WebDriverException(e1);
      }
    }
    return false;
  }

  public static boolean isZipped(String fileName) {
    return fileName.endsWith(".zip") || fileName.endsWith(".xpi");
  }

  public static boolean delete(File toDelete) {
    boolean deleted = true;

    if (toDelete.isDirectory()) {
      for (File child : toDelete.listFiles()) {
        deleted &= child.canWrite() && delete(child);
      }
    }

    return deleted && toDelete.canWrite() && toDelete.delete();
  }

  public static void copy(File from, File to) throws IOException {
    copy(from, to, new NoFilter());
  }

  public static void copy(File source, File dest, String suffix) throws IOException {
    copy(source, dest, suffix == null ? new NoFilter() : new FileSuffixFilter(suffix));
  }

  private static void copy(File source, File dest, Filter onlyCopy) throws IOException {
    if (!source.exists()) {
      return;
    }

    if (source.isDirectory()) {
      copyDir(source, dest, onlyCopy);
    } else {
      copyFile(source, dest, onlyCopy);
    }
  }

  private static void copyDir(File from, File to, Filter onlyCopy) throws IOException {
    if (!onlyCopy.isRequired(from)) {
      return;
    }

    // Create the target directory.
    createDir(to);

    // List children.
    String[] children = from.list();
    for (String child : children) {
      if (!".parentlock".equals(child) && !"parent.lock".equals(child)) {
        copy(new File(from, child), new File(to, child), onlyCopy);
      }
    }
  }

  private static void copyFile(File from, File to, Filter onlyCopy) throws IOException{
    if (!onlyCopy.isRequired(from)) {
      return;
    }

    FileChannel out = null;
    FileChannel in = null;
    try {
      in = new FileInputStream(from).getChannel();
      out = new FileOutputStream(to).getChannel();
      final long length = in.size();

      final long copied = in.transferTo(0, in.size(), out);
      if (copied != length) {
        throw new IOException("Could not transfer all bytes.");
      }
    } finally {
      Cleanly.close(out);
      Cleanly.close(in);
    }
  }

  /**
   * File.setWritable appears in Java 6. If we find the method,
   * we can use it
   */
  private static Method findJdk6SetWritableMethod() {
    try {
      return File.class.getMethod("setWritable", Boolean.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  /**
   * In JDK5 and earlier, we have to use a chmod command from the path.
   */
  private static File findChmodCommand() {

    // Search the path for chmod
    String allPaths = System.getenv("PATH");
    String[] paths = allPaths.split(File.pathSeparator);
    for (String path : paths) {
      File chmod = new File(path, "chmod");
      if (chmod.exists()) {
        return chmod;
      }
    }
    return null;
  }

  /**
   * Used by file operations to determine whether or not to make use of a file.
   */
  public interface Filter {
    /**
     * @param file File to be considered.
     * @return Whether or not to make use of the file in this oprtation.
     */
    boolean isRequired(File file);
  }

  private static class FileSuffixFilter implements Filter {
    private final String suffix;

    public FileSuffixFilter(String suffix) {
      this.suffix = suffix;
    }

    public boolean isRequired(File file) {
      return file.isDirectory() || file.getAbsolutePath().endsWith(suffix);
    }
  }

  private static class NoFilter implements Filter {
    public boolean isRequired(File file) {
      return true;
    }
  }

  public static String readAsString(File toRead) throws IOException {
    Reader reader = null;
    try {
      reader = new BufferedReader(new FileReader(toRead));
      StringBuilder builder = new StringBuilder();
    
      char[] buffer = new char[4096];
      int read;
      while ((read = reader.read(buffer)) != -1) {
        char[] target = new char[read];
        System.arraycopy(buffer, 0, target, 0, read);
        builder.append(target);
      }
    
      return builder.toString();
    } finally {
      Cleanly.close(reader);
    }
  }
}

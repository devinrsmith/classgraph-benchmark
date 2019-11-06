package com.devinrsmith.classgraph;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class StartupSpeed {
  private final ClassLoader loader;
  private final String name;
  private final boolean useClassgraph;
  private final Hasher hasher;
  private long count;

  public StartupSpeed(ClassLoader loader, String name, boolean useClassgraph) {
    this.loader = Objects.requireNonNull(loader);
    this.name = Objects.requireNonNull(name);
    this.useClassgraph = useClassgraph;
    hasher = Hashing.murmur3_32().newHasher();
  }

  void run(long enterMain) throws IOException {
    final long start = System.nanoTime();
    if (useClassgraph) {
      processResourceClassgraph();
    } else {
      processResourceClassloader();
    }
    final long stop = System.nanoTime();
    System.out.println(
        String.format(
            "%s %s %d %s %d %d",
            useClassgraph ? "classgraph" : "classloader",
            name,
            count,
            hasher.hash(),
            TimeUnit.NANOSECONDS.toMillis(stop - start),
            TimeUnit.NANOSECONDS.toMillis(stop - enterMain)));
  }

  private void processResourceClassloader() throws IOException {
    final Enumeration<URL> urls = loader.getResources(name);
    while (urls.hasMoreElements()) {
      final URL url = urls.nextElement();
      try (final InputStream in = url.openStream()) {
        doFastOperation(in);
      }
    }
  }

  private void processResourceClassgraph() throws IOException {
    try (final ScanResult scan =
        new ClassGraph()
            // .enableClassInfo()
            .overrideClassLoaders(loader)
            .disableNestedJarScanning()
            .disableModuleScanning()
            .whitelistClasspathElementsContainingResourcePath(name)
            // .whitelistPaths(name)
            .scan()) {
      for (final Resource resource : scan.getResourcesWithPath(name)) {
        try (final InputStream in = resource.open()) {
          doFastOperation(in);
        }
      }
    }
  }

  private void doFastOperation(InputStream in) throws IOException {
    ++count;
    int read;
    while ((read = in.read()) != -1) {
      hasher.putByte((byte) read);
    }
  }

  public static void main(String[] args) throws IOException {
    final long enterMain = System.nanoTime();
    final boolean useClassgraph = "classgraph".equals(args[0]);
    final List<URL> urls = new ArrayList<>();
    final Iterator<Path> it = Files.list(Paths.get(args[1])).iterator();
    while (it.hasNext()) {
      urls.add(it.next().toUri().toURL());
    }
    // hmm - I don't think this classloader is fully isolated from our runtime classpath?
    final ClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]));
    final String resource = args[2];
    new StartupSpeed(loader, resource, useClassgraph).run(enterMain);
  }
}

package net.mauhiz.jspc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.lang3.SystemUtils;
import org.codehaus.plexus.util.FileUtils;

public class JspcMojoClassLoader extends URLClassLoader {

	/**
	* Figure out where the tools.jar file lives.
	 * @throws IOException 
	*/
	public static URL findToolsJar() throws IOException {
		final File javaHome = FileUtils.resolveFile(new File(File.pathSeparator), System.getProperty("java.home"));

		final File file;
		if (SystemUtils.IS_OS_MAC_OSX) {
			file = FileUtils.resolveFile(javaHome, "../Classes/classes.jar");
		} else {
			file = FileUtils.resolveFile(javaHome, "../lib/tools.jar");
		}

		if (!file.exists()) {
			throw new IOException("Could not find tools.jar at '" + file + "' under java.home: " + javaHome);
		}

		return file.toURI().toURL();

	}

	public JspcMojoClassLoader(final ClassLoader parent, final URL... urLs) {
		super(urLs, parent);
	}

	public void addToolsJar() throws IOException {
		final URL toolJar = findToolsJar();
		addURL(toolJar);
	}

	@Override
	public synchronized URL findResource(final String name) {
		final URL url = super.findResource(name);
		return url == null ? getParent().getResource(name) : url;
	}

	@Override
	public synchronized Class<?> loadClass(final String name) throws ClassNotFoundException {
		final Class<?> c = findLoadedClass(name);
		return c == null ? super.loadClass(name) : c;
	}
}
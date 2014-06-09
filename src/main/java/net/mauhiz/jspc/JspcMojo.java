package net.mauhiz.jspc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jasper.JspC;
import org.apache.jasper.JspCompilationContext;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "jspc", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, executionStrategy = "once-per-session", threadSafe = false)
public class JspcMojo extends AbstractMojo {

	static class PrivilegedLoader implements PrivilegedAction<JspcMojoClassLoader> {
		private final URL[] urls;

		PrivilegedLoader(final URL... urls) {
			this.urls = urls;
		}

		@Override
		public JspcMojoClassLoader run() {
			return new JspcMojoClassLoader(Thread.currentThread().getContextClassLoader(), urls);
		}
	}

	class SpecialJspc extends JspC {

		@Override
		protected void initClassLoader(final JspCompilationContext clctxt) throws IOException {
			super.initClassLoader(clctxt);

			// this is because we don't output classes to target/classes, which means the JasperLoader forgets about them after generation.
			// the trailing slash also matters. Rotten world.
			final URL[] urLs = ArrayUtils.add(loader.getURLs(), new URL(FileSystems.getDefault().getPath(work).toUri()
					.toURL()
					+ "/"));
			final JspcMojoClassLoader cl = AccessController.doPrivileged(new PrivilegedLoader(urLs));
			loader = cl;
			context.setClassLoader(cl);
		}
	}

	@Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
	private List<String> compileClasspathElements;

	@Parameter(defaultValue = "false")
	private boolean verbose;

	@Parameter(property = "webapp", defaultValue = "${project.basedir}/src/main/webapp", required = true)
	private String webapp;

	@Parameter(property = "webXmlFragment", defaultValue = "${project.build.directory}/jspweb.xml", required = true, readonly = true)
	private String webXmlFragment;

	@Parameter(property = "work", defaultValue = "${project.build.directory}/${project.build.finalName}/WEB-INF/work", required = true)
	private String work;

	@Override
	public void execute() throws MojoFailureException, MojoExecutionException {
		if (compileClasspathElements.isEmpty()) {
			throw new MojoExecutionException("Classpath is empty!");
		}

		// this stuff seems necessary to avoid tags not found
		// my guess is that the synchronized method in the classloader is the point
		try {
			final JspC jspc = new SpecialJspc();
			init(jspc);

			jspc.execute();
		} catch (final Exception be) {
			throw new MojoFailureException("Could not generate all tags", be);
		}
	}

	private void init(final JspC jspc) {
		jspc.setAddWebXmlMappings(false);
		jspc.setClassDebugInfo(true);
		jspc.setClassPath(StringUtils.join(compileClasspathElements, File.pathSeparator));
		jspc.setCompile(true);
		jspc.setCompilerSourceVM("1.7");
		jspc.setCompilerTargetVM("1.7");
		jspc.setErrorOnUseBeanInvalidClassAttribute(true);
		jspc.setFailOnError(true);
		jspc.setGenStringAsCharArray(true);
		jspc.setJavaEncoding("UTF-8");
		jspc.setListErrors(true);
		jspc.setPoolingEnabled(true);
		jspc.setPackage("org.apache.jsp");
		jspc.setOutputDir(work);
		jspc.setTrimSpaces(true);
		jspc.setUriroot(webapp);
		jspc.setValidateTld(false);
		jspc.setVerbose(verbose ? 1 : 0);
		jspc.setWebXmlFragment(webXmlFragment);
		jspc.setWebXmlEncoding("UTF-8");
	}
}

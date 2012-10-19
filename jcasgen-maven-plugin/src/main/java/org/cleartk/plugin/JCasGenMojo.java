package org.cleartk.plugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.jcasgen.IError;
import org.apache.uima.tools.jcasgen.Jg;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLInputSource;
import org.codehaus.plexus.util.DirectoryScanner;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Applies JCasGen to create Java files from XML type system descriptions.
 * 
 * Note that by default this runs at the process-resources phase because it requires the XML
 * descriptor files to already be at the appropriate places on the classpath, and the
 * generate-resources phase runs before resources are copied.
 * 
 * @goal generate
 * @phase process-resources
 * @requiresDependencyResolution compile
 */
public class JCasGenMojo extends AbstractMojo {

  /**
   * The path to the XML type system description.
   * 
   * @parameter
   * @required
   * @readonly
   */
  private String typeSystem;

  /**
   * The directory where the generated sources will be written.
   * 
   * @parameter default-value="${project.build.directory}/generated-sources/jcasgen"
   * @required
   * @readonly
   */
  private File outputDirectory;

  /**
   * The Maven Project.
   * 
   * @parameter property="project"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * The Plexus build context, used to see if files have changed.
   * 
   * @component
   */
  private BuildContext buildContext;

  public void execute() throws MojoExecutionException, MojoFailureException {

    // the type system is relative to the base directory
    File typeSystemFile = new File(this.project.getBasedir(), this.typeSystem);

    // assemble the classpath
    List<String> elements;
    try {
      elements = this.project.getCompileClasspathElements();
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    StringBuilder classpathBuilder = new StringBuilder();
    for (String element : elements) {
      if (classpathBuilder.length() > 0) {
        classpathBuilder.append(File.pathSeparatorChar);
      }
      classpathBuilder.append(element);
    }
    String classpath = classpathBuilder.toString();

    // skip JCasGen if there are no changes in the type system file or the files it references
    if (!this.buildContext.hasDelta(this.typeSystem) && !this.hasDelta(typeSystemFile, classpath)) {
      return;
    }

    // run JCasGen to generate the Java sources
    JCasGenErrors error = new JCasGenErrors();
    Jg jCasGen = new Jg();
    jCasGen.error = error;
    String[] args = new String[] {
        "-jcasgeninput",
        typeSystemFile.toString(),
        "-jcasgenoutput",
        this.outputDirectory.getAbsolutePath(),
        "=jcasgenclasspath",
        classpath };
    try {
      jCasGen.main1(args);
    } catch (JCasGenException e) {
      throw new MojoExecutionException(e.getMessage(), e.getCause());
    }

    // signal that the output directory has changed
    this.buildContext.refresh(this.outputDirectory);

    // add the generated sources to the build
    this.project.addCompileSourceRoot(this.outputDirectory.getPath());
  }

  static class JCasGenErrors implements IError {

    private static Level logLevels[] = new Level[3];
    static {
      logLevels[IError.INFO] = Level.INFO;
      logLevels[IError.WARN] = Level.WARNING;
      logLevels[IError.ERROR] = Level.SEVERE;
    }

    @Override
    public void newError(int severity, String message, Exception exception) {
      Logger log = UIMAFramework.getLogger();
      log.log(logLevels[severity], "JCasGen: " + message, exception);
      if (severity >= IError.ERROR) {
        throw new JCasGenException(exception);
      }
    }
  }

  static class JCasGenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JCasGenException(Throwable cause) {
      super(cause);
    }
  }

  private boolean hasDelta(File typeSystemFile, String classpath) throws MojoExecutionException {
    // load the type system
    XMLInputSource in;
    try {
      in = new XMLInputSource(typeSystemFile.toURI().toURL());
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    TypeSystemDescription typeSystemDescription;
    try {
      typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(in);
    } catch (InvalidXMLException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    // resolve the type system imports using the classpath
    ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();
    try {
      resourceManager.setExtensionClassPath(classpath, true);
      resourceManager.setDataPath(classpath);
    } catch (MalformedURLException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    try {
      typeSystemDescription.resolveImports(resourceManager);
    } catch (InvalidXMLException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    File buildOutputDirectory = new File(this.project.getBuild().getOutputDirectory());

    // map each resource from its target location to its source location
    Map<File, File> targetToSource = new HashMap<File, File>();
    for (Resource resource : this.project.getResources()) {
      File resourceDir = new File(resource.getDirectory());
      if (resourceDir.exists()) {

        // scan for the resource files
        List<String> includes = resource.getIncludes();
        if (includes.isEmpty()) {
          includes = Arrays.asList("**");
        }
        List<String> excludes = resource.getExcludes();
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(resourceDir);
        scanner.setIncludes(includes.toArray(new String[includes.size()]));
        scanner.setExcludes(excludes.toArray(new String[excludes.size()]));
        scanner.scan();

        // map each of the resources from its target location to its source location
        String targetPath = resource.getTargetPath();
        for (String filePath : scanner.getIncludedFiles()) {
          File sourceFile = new File(resourceDir, filePath);
          File baseDirectory = targetPath != null
              ? new File(buildOutputDirectory, targetPath)
              : buildOutputDirectory;
          File targetFile = new File(baseDirectory, filePath);
          targetToSource.put(targetFile, sourceFile);
        }
      }
    }

    // search through the type system description for source files that have changed
    for (TypeDescription type : typeSystemDescription.getTypes()) {
      URL typeSystemURL = type.getSourceUrl();
      if (typeSystemURL != null) {
        File targetFile;
        try {
          targetFile = new File(typeSystemURL.toURI());
        } catch (IllegalArgumentException e) {
          // the URL is not a file, so assume it has changed
          return true;
        } catch (URISyntaxException e) {
          // the URL is not a file, so assume it has changed
          return true;
        }
        File sourceFile = targetToSource.get(targetFile);
        if (sourceFile == null) {
          // no resource corresponding to this file, so assume the file has changed
          return true;
        }
        // the file was found, only return true if the file has actually changed
        if (this.buildContext.hasDelta(sourceFile)) {
          return true;
        }
      }
    }
    return false;
  }
}

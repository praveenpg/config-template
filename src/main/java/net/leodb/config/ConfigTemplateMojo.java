package net.leodb.config;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mojo(name = "generateConfig", defaultPhase = LifecyclePhase.PACKAGE)
public class ConfigTemplateMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "build.env", defaultValue = "dev")
    String env;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final File baseDir = project.getBasedir();
        final List<File> mappingYamls = Arrays.stream(Objects.requireNonNull(baseDir.listFiles()))
                .filter(file -> (file.getName().equals("mapping.yaml") || file.getName().equals("mapping.yml"))).collect(Collectors.toList());
        final File dataDir = Arrays.stream(Objects.requireNonNull(baseDir.listFiles())).filter(file -> file.getName().equals("data")).findFirst().orElseThrow(() -> new IllegalStateException("no data directory found"));
        final List<File> envYamlFiles = Arrays.stream(Objects.requireNonNull(dataDir.listFiles()))
                .filter(file -> (file.getName().equals(env + ".yaml") || file.getName().equals(env + ".yml"))).collect(Collectors.toList());
        final List<File> localYamlFiles = Arrays.stream(Objects.requireNonNull(dataDir.listFiles()))
                .filter(file -> (file.getName().equals("local.yaml") || file.getName().equals("local.yml"))).collect(Collectors.toList());
        final List<File> defaultYamlFiles = Arrays.stream(Objects.requireNonNull(dataDir.listFiles()))
                .filter(file -> (file.getName().equals("default.yaml") || file.getName().equals("default.yml"))).collect(Collectors.toList());
        final File mappingYaml;
        final File envYamlFile;
        final File localYamlFile;
        final File defaultYamlFile;

        getLog().info("Env = " + env);

        getLog().info("Parsing mapping");
        if(mappingYamls.size() == 0) {
            throw new IllegalStateException("No mapping yaml file found");
        } else if(mappingYamls.size() > 1) {
            throw new IllegalStateException("Multiple mapping yaml files found");
        }

        getLog().debug("Parsing envYamlFiles");
        if(envYamlFiles.size() == 0) {
            throw new IllegalStateException("No env yaml file found");
        } else if(envYamlFiles.size() > 1) {
            throw new IllegalStateException(String.format("Multiple %s yaml files found", env));
        }

        getLog().debug("Parsing localYamlFiles");
        if(localYamlFiles == null || localYamlFiles.size() == 0) {
            localYamlFile = null;
        } else if(localYamlFiles.size() > 1) {
            throw new IllegalStateException("More than one local yaml files found");
        } else {
            localYamlFile = localYamlFiles.get(0);
        }

        getLog().debug("Parsing defaultYamlFiles");
        if(defaultYamlFiles == null || defaultYamlFiles.size() == 0) {
            getLog().debug("Parsing defaultYamlFiles - 1");
            defaultYamlFile = null;
        } else if(defaultYamlFiles.size() > 1) {
            getLog().debug("Parsing defaultYamlFiles - 2");
            throw new IllegalStateException("More than one local yaml files found");
        } else {
            getLog().debug("Parsing defaultYamlFiles - 3");
            defaultYamlFile = defaultYamlFiles.get(0);
        }

        getLog().info("Completed data yamls");

        envYamlFile = envYamlFiles.get(0);

        mappingYaml = mappingYamls.get(0);
        new MappingFileParser(baseDir, mappingYaml, envYamlFile, localYamlFile, defaultYamlFile, getLog(), env).generateConfigs();
    }
}

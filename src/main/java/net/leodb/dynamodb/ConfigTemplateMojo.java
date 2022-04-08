package net.leodb.dynamodb;

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

@Mojo(name = "generateConfig", defaultPhase = LifecyclePhase.COMPILE)
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

        if(mappingYamls.size() == 0) {
            throw new IllegalStateException("No mapping yaml file found");
        } else if(mappingYamls.size() > 1) {
            throw new IllegalStateException("Multiple mapping yaml files found");
        }

        if(envYamlFiles.size() == 0) {
            throw new IllegalStateException("No env yaml file found");
        } else if(envYamlFiles.size() > 1) {
            throw new IllegalStateException(String.format("Multiple %s yaml files found", env));
        }

        if(localYamlFiles.size() == 0) {
            localYamlFile = null;
        } else if(localYamlFiles.size() > 1) {
            throw new IllegalStateException("More than one local yaml files found");
        } else {
            localYamlFile = localYamlFiles.get(0);
        }

        if(defaultYamlFiles.size() == 0) {
            defaultYamlFile = null;
        } else if(defaultYamlFiles.size() > 1) {
            throw new IllegalStateException("More than one local yaml files found");
        } else {
            defaultYamlFile = localYamlFiles.get(0);
        }

        envYamlFile = envYamlFiles.get(0);

        mappingYaml = mappingYamls.get(0);
        new MappingFileParser(baseDir, mappingYaml, envYamlFile, localYamlFile, defaultYamlFile, getLog()).generateConfigs();
    }
}

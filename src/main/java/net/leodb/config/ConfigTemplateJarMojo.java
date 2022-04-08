package net.leodb.config;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo( name = "jar", defaultPhase = LifecyclePhase.PACKAGE)
public class ConfigTemplateJarMojo extends ConfigTemplateMojo{
}

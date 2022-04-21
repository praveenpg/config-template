//file:noinspection GroovyAssignabilityCheck
package net.leodb.config

import groovy.text.SimpleTemplateEngine
import org.apache.commons.lang3.StringUtils
import org.apache.maven.plugin.logging.Log
import org.yaml.snakeyaml.Yaml

class MappingFileParser {
    private final File baseDirectory;
    private final File mappingYaml;
    private final Log log;
    private final Set<String> set = new HashSet<>()
    private final File envFile
    private final Map envMap;
    private final SimpleTemplateEngine templateEngine
    String buildEnv

    MappingFileParser(File baseDirectory, File mappingYaml, File envFile, File localFile, File defaultYamlFile, Log log, String buildEnv) {
        this.baseDirectory = baseDirectory
        this.mappingYaml = mappingYaml
        this.log = log;
        this.envFile = envFile
        this.templateEngine = new SimpleTemplateEngine();
        this.envMap = new HashMap()
        this.buildEnv = buildEnv;

        log.debug("populating default")
        populateDataYaml(defaultYamlFile, envMap, buildEnv)

        log.debug("populating env")
        populateDataYaml(envFile, envMap, buildEnv)

        log.debug("populating local")
        populateDataYaml(localFile, envMap, "dev")
    }

    private static void populateDataYaml(File file, Map envMap, String buildEnv) {
        if (file != null && file.exists()) {
            final String fileContent = file.getText().trim()

            if (!StringUtils.isEmpty(fileContent)) {
                Map map = new HashMap<>(new Yaml().load(fileContent))

                if (map != null && !map.isEmpty()) {
                    envMap.putAll(map)
                }
            }
        }
    }

    void generateConfigs() {
        final Yaml parser = new Yaml();
        final Map map = parser.load(mappingYaml.getText())
        final Set<String> keys = map.keySet();

        keys.each { key -> handleMapping(key, map.get(key)) }
    }

    private String getReplacedValue(String key) {
        SimpleTemplateEngine templateEngine = new SimpleTemplateEngine()
        final HashMap map = new HashMap()

        map.put("env", buildEnv)

        String value = templateEngine.createTemplate(key).make(map)

        return value;
    }

    void handleMapping(String key, Object value) {
        if (value instanceof String) {
            parseTemplate(key, value, false)
        } else if(value instanceof List){
            log.debug("List: " + value)
            value.each {listVal -> parseTemplate(listVal, key, true)}
        }
    }

    private void parseTemplate(String key, String value, boolean appendContent) {
        String baseDirectoryPath = baseDirectory.getPath() + "/" + getReplacedValue(key);
        log.debug("Base Directory Path: " + baseDirectoryPath)
        File templateFile = new File(baseDirectoryPath);
        String finalDestPath = baseDirectory.getPath() + "/" + getReplacedValue((String) value);
        String finalDestPathDirPath = finalDestPath.substring(0, finalDestPath.lastIndexOf("/"));
        File finalDestPathDirPathDir = new File(finalDestPathDirPath)

        log.debug("template file: " + templateFile)

        if (!finalDestPathDirPathDir.exists()) {
            finalDestPathDirPathDir.mkdirs();
        }

        File destFile = new File(finalDestPath)

        if (!destFile.exists()) {
            destFile.createNewFile();
            destFile.append("")
            parseTemplate(key, value, appendContent)

            return
        } else {
            if (!appendContent) {
                clearDestFile(destFile)
            } else {
                if(!set.contains(value)) {
                    clearDestFile(destFile)

                    set.add(value)
                }
            }
        }
        String templateText = templateEngine.createTemplate(templateFile.getText()).make(envMap);

        log.debug("template text: " + templateText)

        destFile.append(templateText)
    }

    private static void clearDestFile(File destFile) {
        destFile.delete();
        destFile.createNewFile();
        destFile.append("")
    }
}

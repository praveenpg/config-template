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

    MappingFileParser(File baseDirectory, File mappingYaml, File envFile, File localFile, File defaultYamlFile, Log log) {
        this.baseDirectory = baseDirectory
        this.mappingYaml = mappingYaml
        this.log = log;
        this.envFile = envFile
        this.templateEngine = new SimpleTemplateEngine();
        this.envMap = new HashMap()

        log.debug("populating default")
        populateDataYaml(defaultYamlFile, envMap)

        log.debug("populating env")
        populateDataYaml(envFile, envMap)

        log.debug("populating local")
        populateDataYaml(localFile, envMap)
    }

    private static void populateDataYaml(File file, Map envMap) {
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

    void handleMapping(String key, Object value) {
        if (value instanceof String) {
            parseTemplate(key, value, false)
        } else if(value instanceof List){
            log.info("List: " + value)
            value.each {listVal -> parseTemplate(listVal, key, true)}
        }
    }

    private void parseTemplate(String key, String value, boolean appendContent) {
        File templateFile = new File(baseDirectory.getPath() + "/" + key);
        String finalDestPath = baseDirectory.getPath() + "/" + (String) value;
        String finalDestPathDirPath = finalDestPath.substring(0, finalDestPath.lastIndexOf("/"));
        File finalDestPathDirPathDir = new File(finalDestPathDirPath)

        log.info("template file: " + templateFile)

        if (!finalDestPathDirPathDir.exists()) {
            finalDestPathDirPathDir.mkdirs();
        }

        File destFile = new File(finalDestPath)

        if (!destFile.exists()) {
            destFile.createNewFile()
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

        log.info("template text: " + templateText)

        destFile.append(templateText)
    }

    private static void clearDestFile(File destFile) {
        destFile.delete();
        destFile.createNewFile();
        destFile.append("")
    }
}

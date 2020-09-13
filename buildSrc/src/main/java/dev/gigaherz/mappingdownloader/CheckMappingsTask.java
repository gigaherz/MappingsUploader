package dev.gigaherz.mappingdownloader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import com.google.common.collect.BiMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CheckMappingsTask extends DefaultTask
{
    public static final Logger LOGGER = LogManager.getLogger(MergeMappingsTask.class);

    public static final Set<String> JAVA_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while")));

    private File checkDir;

    public void checkDir(File file) {
        setCheckDir(file);
    }
    public void setCheckDir(File file) {
        checkDir = file;
    }
    public File getCheckDir() {
        return checkDir;
    }

    @InputFiles
    public List<File> getInputFiles() {
        File fieldsFile = new File(checkDir, "fields.csv");
        File methodsFile = new File(checkDir, "methods.csv");
        File paramsFile = new File(checkDir, "params.csv");
        return Arrays.asList(fieldsFile, methodsFile, paramsFile);
    }

    @TaskAction
    public void checkFieldMappings() throws IOException
    {
        boolean errors = checkMappings("fields.csv", false);
        errors = errors || checkMappings("methods.csv", false);
        errors = errors || checkMappings("params.csv", true);
        if (errors)
            throw new RuntimeException("Errors found during check.");
    }

    public boolean checkMappings(String name, boolean isParams) throws IOException
    {
        boolean errors = false;
        File path = new File(checkDir, name);
        Multimap<Integer, String> paramNames = ArrayListMultimap.create();
        if (path.exists())
        {
            try (
                    CSVParser refIn = CSVParser.parse(path, StandardCharsets.UTF_8, Utils.CUSTOM_CSV_FORMAT);
            )
            {
                for(CSVRecord r : refIn)
                {
                    String srgName = r.get(0);
                    String mcpName = r.get(1);
                    if (JAVA_KEYWORDS.contains(mcpName))
                    {
                        LOGGER.error(String.format("Record contains a java keyword: %s -> %s", srgName, mcpName));
                        errors = true;
                    }
                    if (isParams)
                    {
                        int id = Utils.getId(srgName);
                        if (paramNames.containsEntry(id, mcpName))
                        {
                            LOGGER.error(String.format("Record contains a duplicate param name: %s -> %s", srgName, mcpName));
                            errors = true;
                        }
                        paramNames.put(id, mcpName);
                    }
                }
            }
        }
        return errors;
    }
}

package dev.gigaherz.mappingdownloader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

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

    private File joinedTsrg;

    public void joinedTsrg(File file) {
        setJoinedTsrg(file);
    }
    public void setJoinedTsrg(File file) {
        joinedTsrg = file;
    }
    public File getJoinedTsrg() {
        return joinedTsrg;
    }

    private File inheritanceJson;

    public void inheritanceJson(File file) {
        setInheritanceJson(file);
    }
    public void setInheritanceJson(File file) {
        inheritanceJson = file;
    }
    public File getInheritanceJson() {
        return inheritanceJson;
    }

    @InputFiles
    public List<File> getInputFiles() {
        File fieldsFile = new File(checkDir, "fields.csv");
        File methodsFile = new File(checkDir, "methods.csv");
        File paramsFile = new File(checkDir, "params.csv");
        return Arrays.asList(fieldsFile, methodsFile, paramsFile);
    }

    @TaskAction
    public void checkMappings() throws IOException
    {
        MappingData obfToSrg;

        try (BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(joinedTsrg))))
        {
            obfToSrg = new MappingData(rd);
        }

        InheritanceTree inhObf;
        try(Reader r = new InputStreamReader(new FileInputStream(inheritanceJson)))
        {
            inhObf = new InheritanceTree(r);
        }

        InheritanceTree inhSrg = inhObf.remap(obfToSrg);

        Map<String, String> methodToClass = new HashMap<>();
        Map<String, String> fieldToClass = new HashMap<>();
        for(InheritanceTree.InhClass classData : inhSrg.classes.values())
        {
            for (String s : classData.methods.keySet())
            {
                methodToClass.put(s, classData.name);
            }
        }
        for(MappingData.TsrgClass classData : obfToSrg.classes.values())
        {
            for (String s : classData.fields.values())
            {
                fieldToClass.put(s, classData.name);
            }
            for (String s : classData.methods.values())
            {
                methodToClass.put(s, classData.name);
            }
        }

        MappingData srgToMapped = new MappingData(obfToSrg);

        boolean errors = checkMappings("fields.csv", false, (srg, mcp) -> srgToMapped.mapField(fieldToClass, srg, mcp));
        errors = errors || checkMappings("methods.csv", false, (srg, mcp) -> srgToMapped.mapMethod(methodToClass, srg, mcp));
        errors = errors || checkMappings("params.csv", true, (srg, mcp) -> srgToMapped.mapParam(srg, mcp));

        if (!errors)
        {
            Set<String> seen = new HashSet<>();
            for(MappingData.TsrgClass classData : srgToMapped.classes.values())
            {
                seen.clear();
                for (Map.Entry<String,String> s : classData.fields.entrySet())
                {
                    if (seen.contains(s.getValue()))
                    {
                        LOGGER.error(String.format("Record contains a duplicate field name: %s -> %s", s.getKey(), s.getValue()));
                        errors=true;
                        continue;
                    }
                    if (checkParents(s.getKey(), s.getValue(), fieldToClass, inhSrg, srgToMapped))
                    {
                        LOGGER.error(String.format("Record contains a field name potentially clashing with a parent class: %s -> %s", s.getKey(), s.getValue()));
                        errors=true;
                    }
                }
                /*seen.clear();
                for (String s : classData.methods.values())
                {
                }*/
            }
        }

        if (errors)
        {
            throw new RuntimeException("Errors found during check.");
        }
    }

    private boolean checkParents(String srg, String mcp, Map<String, String> fieldToClass, InheritanceTree inhSrg, MappingData srgToMcp)
    {
        String cls = fieldToClass.get(srg);
        if (cls == null)
        {
            //LOGGER.warn(String.format("The field name does not appear to exist in the joined.tsrg. Is it from an old version? %s", srg));
            return false;
        }
        InheritanceTree.InhClass c = inhSrg.classes.get(cls);
        while(c != null && c.superName != null)
        {
            c = inhSrg.classes.get(c.superName);
            if (c == null)
                break;
            MappingData.TsrgClass c2 = srgToMcp.classes.get(c.name);
            if (c2 == null)
                continue;
            if (c2.fields.containsValue(mcp))
            {
                return true;
            }
        }
        return false;
    }

    public boolean checkMappings(String name, boolean isParams, BiConsumer<String, String> addMapping) throws IOException
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
                        continue;
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
                    addMapping.accept(srgName,mcpName);
                }
            }
        }
        return errors;
    }
}

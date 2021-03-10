package dev.gigaherz.mappingdownloader;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class MergeMappingsTask extends DefaultTask
{
    public static final Logger LOGGER = LogManager.getLogger(MergeMappingsTask.class);

    private File destinationDir;
    private List<File> sourceDirs = new ArrayList<>();
    private boolean officialMappingOverlayMode = false;

    public void dirs(File... files)
    {
        Collections.addAll(sourceDirs, files);
    }
    public List<File> getSourceDirs()
    {
        return sourceDirs;
    }
    public void setSourceDirs(List<File> sourceDirs)
    {
        this.sourceDirs = sourceDirs;
    }

    public void destinationDir(File file) {
        setDestinationDir(file);
    }
    public void setDestinationDir(File file) {
        destinationDir = file;
    }
    public File getDestinationDir() {
        return destinationDir;
    }

    public boolean getOfficialMappingOverlayMode() {
        return officialMappingOverlayMode;
    }
    public void setOfficialMappingOverlayMode(boolean mode) {
        officialMappingOverlayMode = mode;
    }

    @InputFiles
    public List<File> getInputFiles() {
        List<File> files = new ArrayList<>();
        for(File file : sourceDirs)
        {
            files.add(new File(destinationDir, "fields.csv"));
            files.add(new File(destinationDir, "methods.csv"));
            files.add(new File(destinationDir, "params.csv"));
        }
        return files;
    }

    @OutputFiles
    public List<File> getOutputFiles() {
        File fieldsFile = new File(destinationDir, "fields.csv");
        File methodsFile = new File(destinationDir, "methods.csv");
        File paramsFile = new File(destinationDir, "params.csv");
        return Arrays.asList(fieldsFile, methodsFile, paramsFile);
    }

    @TaskAction
    public void mergeFieldMappings() throws IOException
    {
        if (!destinationDir.exists())
            destinationDir.mkdirs();

        LOGGER.info("Merging fields CSVs...");
        mergeAndSort("fields.csv", false);
    }

    @TaskAction
    public void mergeMethodMappings() throws IOException
    {
        if (!destinationDir.exists())
            destinationDir.mkdirs();

        LOGGER.info("Merging methods CSVs...");
        mergeAndSort("methods.csv", false);
    }

    @TaskAction
    public void mergeParamMappings() throws IOException
    {
        if (!destinationDir.exists())
            destinationDir.mkdirs();

        LOGGER.info("Merging params CSVs...");
        mergeAndSort("params.csv", true);
    }

    private void mergeAndSort(String file, boolean isParam) throws IOException
    {
        Map<String, CSVRecord> recordMap = new HashMap<>();
        List<String> headers = null;
        for(File sourceDir : sourceDirs)
        {
            File path = new File(sourceDir, file);
            if (path.exists())
            {
                try (
                        CSVParser refIn = CSVParser.parse(path, StandardCharsets.UTF_8, Utils.CUSTOM_CSV_FORMAT);
                )
                {
                    List<CSVRecord> recordsRef = refIn.getRecords();
                    addToMap(recordMap, recordsRef);

                    if (headers == null) headers = refIn.getHeaderNames();
                }
            }
        }

        try (
                CSVPrinter fileOut = new CSVPrinter(new FileWriter(new File(destinationDir, file)), Utils.CUSTOM_CSV_FORMAT);
        )
        {
            if (headers != null)
                fileOut.printRecord(headers);

            List<CSVRecord> merged = recordMap.values().stream().sorted(Utils::compareById).collect(Collectors.toList());
            for (CSVRecord record : merged)
            {
                if (officialMappingOverlayMode) // only params and javadocs
                {
                    if (isParam)
                        fileOut.printRecord(record.get(0), record.get(1), record.get(2));
                    else if (!Utils.isNullOrEmpty(record.get(3)))
                        fileOut.printRecord(record.get(0), record.get(0), record.get(2), record.get(3));
                }
                else
                {
                    if (isParam)
                        fileOut.printRecord(record.get(0), record.get(1), record.get(2));
                    else
                        fileOut.printRecord(record.get(0), record.get(1), record.get(2), record.get(3));
                }
            }
        }
    }

    private void addToMap(Map<String, CSVRecord> recordMap, List<CSVRecord> list)
    {
        list.forEach(row -> {
            if (Utils.isNullOrEmpty(row.get(1)))
            {
                recordMap.remove(row.get(0));
            }
            else if (!row.get(0).equals(row.get(1)) || (row.size() >= 4 && !Utils.isNullOrEmpty(row.get(3))))
            {
                String srg = row.get(0);
                CSVRecord existing = recordMap.get(srg);
                if (existing != null)
                {
                    if (existing.get(1).equals(row.get(1)))
                    {
                        LOGGER.warn(String.format("Pointless override: %s -> %s", row.get(0), row.get(1)));
                    }
                }
                recordMap.put(row.get(0), row);
            }
        });
    }
}

package dev.gigaherz.mappingdownloader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
import java.util.Arrays;
import java.util.List;

public class ConvertToCsvTask extends DefaultTask
{
    public static final Logger LOGGER = LogManager.getLogger(ConvertToCsvTask.class);

    public boolean includeUnverified = false;
    public File destinationDir;
    public File inputFile;

    public void inputFile(File file)
    {
        this.inputFile = file;
    }

    public void setDestinationDir(File file)
    {
        destinationDir = file;
    }

    public void destinationDir(File file)
    {
        setDestinationDir(file);
    }

    public File getDestinationDir()
    {
        return destinationDir;
    }

    @InputFiles
    public List<File> getInputFiles()
    {
        return Arrays.asList(inputFile);
    }

    @OutputFiles
    public List<File> getOutputFiles()
    {
        File fieldsFile = new File(destinationDir, "fields.csv");
        File methodsFile = new File(destinationDir, "methods.csv");
        File paramsFile = new File(destinationDir, "params.csv");
        return Arrays.asList(fieldsFile, methodsFile, paramsFile);
    }

    @TaskAction
    public void convertToMcp() throws IOException
    {
        if (!destinationDir.exists())
            destinationDir.mkdirs();

        boolean anyErrors = false;

        File fieldsFile = new File(destinationDir, "fields.csv");
        File methodsFile = new File(destinationDir, "methods.csv");
        File paramsFile = new File(destinationDir, "params.csv");
        try (
                CSVParser parser = CSVParser.parse(inputFile, StandardCharsets.UTF_8, Utils.CUSTOM_CSV_FORMAT);
        )
        {
            List<CSVRecord> records = parser.getRecords();
            try (CSVPrinter fields = new CSVPrinter(new FileWriter(fieldsFile), Utils.CUSTOM_CSV_FORMAT))
            {
                fields.printRecord("searge", "name", "side", "desc");
                for (CSVRecord record : records)
                {
                    if (record.size() != 6)
                        LOGGER.warn(String.format("Warning at entry, incorrect number of entries at: %s", record));
                    boolean include = ("TRUE".equals(record.get(0)) || includeUnverified)
                            && !isNullOrEmpty(record.get(2)) && !isNullOrEmpty(record.get(3));
                    if (include)
                    {
                        String className = record.get(1);
                        String srgName = record.get(2);
                        String mcpName = record.get(3);
                        String side = record.get(4);
                        String comment = record.get(5);
                        if (Utils.isNullOrEmpty(side)) side = "2";

                        if (srgName.contains(" "))
                        {
                            LOGGER.error("SRG name contains spaces: " + srgName);
                            anyErrors = true;
                            continue;
                        }

                        if (!Utils.LEGAL_NAME.matcher(mcpName).matches())
                        {
                            LOGGER.error("MCP name invalid: " + mcpName);
                            anyErrors = true;
                            continue;
                        }
                        if (side.contains(" "))
                        {
                            LOGGER.error("Side contains spaces: " + side);
                            anyErrors = true;
                            side = "2";
                        }
                        if (srgName.startsWith("field_"))
                        {
                            fields.printRecord(srgName, mcpName, side, comment);
                        }
                    }
                }
            }
            try (CSVPrinter methods = new CSVPrinter(new FileWriter(methodsFile), Utils.CUSTOM_CSV_FORMAT))
            {
                methods.printRecord("searge", "name", "side", "desc");
                for (CSVRecord record : records)
                {
                    if (record.size() != 6)
                        LOGGER.warn(String.format("Warning at entry, incorrect number of entries at: %s", record));
                    boolean include = ("TRUE".equals(record.get(0)) || includeUnverified)
                            && !isNullOrEmpty(record.get(2)) && !isNullOrEmpty(record.get(3));
                    if (include)
                    {
                        String className = record.get(1);
                        String srgName = record.get(2);
                        String mcpName = record.get(3);
                        String side = record.get(4);
                        String comment = record.get(5);
                        if (Utils.isNullOrEmpty(side)) side = "2";

                        if (srgName.contains(" "))
                        {
                            LOGGER.error("SRG name contains spaces: " + srgName);
                            anyErrors = true;
                            continue;
                        }

                        if (!Utils.LEGAL_NAME.matcher(mcpName).matches())
                        {
                            LOGGER.error("MCP name invalid: " + mcpName);
                            anyErrors = true;
                            continue;
                        }
                        if (side.contains(" "))
                        {
                            LOGGER.error("Side contains spaces: " + side);
                            anyErrors = true;
                            side = "2";
                        }
                        if (srgName.startsWith("func_"))
                        {
                            methods.printRecord(srgName, mcpName, side, comment);
                        }
                    }
                }
            }
            try (CSVPrinter params = new CSVPrinter(new FileWriter(paramsFile), Utils.CUSTOM_CSV_FORMAT))
            {
                params.printRecord("param", "name", "side");
                for (CSVRecord record : records)
                {
                    if (record.size() != 6)
                        LOGGER.warn(String.format("Warning at entry, incorrect number of entries at: %s", record));
                    boolean include = ("TRUE".equals(record.get(0)) || includeUnverified)
                            && !isNullOrEmpty(record.get(2)) && !isNullOrEmpty(record.get(3));
                    if (include)
                    {
                        String className = record.get(1);
                        String srgName = record.get(2);
                        String mcpName = record.get(3);
                        String side = record.get(4);
                        String comment = record.get(5);
                        if (Utils.isNullOrEmpty(side)) side = "2";

                        if (srgName.contains(" "))
                        {
                            LOGGER.error("SRG name contains spaces: " + srgName);
                            anyErrors = true;
                            continue;
                        }

                        if (!Utils.LEGAL_NAME.matcher(mcpName).matches())
                        {
                            LOGGER.error("MCP name invalid: " + mcpName);
                            anyErrors = true;
                            continue;
                        }

                        if (side.contains(" "))
                        {
                            LOGGER.error("Side contains spaces: " + side);
                            anyErrors = true;
                            side = "2";
                        }

                        if (srgName.startsWith("p_"))
                        {
                            params.printRecord(srgName, mcpName, side);
                        }
                    }
                }
            }
        }

        if (anyErrors)
            throw new RuntimeException("Finished with errors!");
    }

    private static boolean isNullOrEmpty(String s)
    {
        return s == null || s.length() == 0;
    }
}

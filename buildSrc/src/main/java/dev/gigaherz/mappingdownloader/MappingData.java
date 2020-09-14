package dev.gigaherz.mappingdownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MappingData
{
    public final Map<String, TsrgClass> classes = new HashMap<>();

    public MappingData()
    {
    }

    // Ensures we have all classes, fields and methods present
    public MappingData(MappingData previousMapping)
    {
        for(MappingData.TsrgClass classData : previousMapping.classes.values())
        {
            TsrgClass newClass;
            classes.put(classData.name, newClass = new TsrgClass(classData.name));

            for (String s : classData.fields.values())
            {
                newClass.fields.put(s, s);
            }
            for (String s : classData.methods.values())
            {
                newClass.fields.put(s, s);
            }
        }
    }

    public MappingData(BufferedReader r)
    {
        try
        {
            TsrgClass currentClass = null;
            String line;
            while ((line = r.readLine()) != null)
            {
                if (line.startsWith("\t"))
                {
                    String[] args = line.substring(1).split(" ");
                    if(args.length == 2) // field
                    {
                        currentClass.fields.put(args[0], args[1]);
                    }
                    else // method
                    {
                        currentClass.methods.put(String.format("%s %s",args[0], args[1]), args[2]);
                    }
                }
                else
                {
                    String[] args = line.split(" ");
                    classes.put(args[0], currentClass = new TsrgClass(args[1]));
                }
            }
        }
        catch(IOException e)
        {

        }
    }

    public void mapField(Map<String, String> fieldToClass, String srg, String mcp)
    {
        String cls = fieldToClass.get(srg);
        if (cls == null)
            return;
        TsrgClass clsData = classes.computeIfAbsent(cls, TsrgClass::new);
        clsData.fields.put(srg, mcp);
    }

    public void mapMethod(Map<String, String> methodToClass, String srg, String mcp)
    {
        String cls = methodToClass.get(srg);
        if (cls == null)
            return;
        TsrgClass clsData = classes.computeIfAbsent(cls, TsrgClass::new);
        clsData.methods.put(srg, mcp);
    }

    public void mapParam(String srg, String mcp)
    {
        // TODO
    }

    public class TsrgClass
    {
        public final String name;
        public final Map<String, String> fields = new HashMap<>();
        public final Map<String, String> methods = new HashMap<>();

        TsrgClass(String name)
        {
            this.name = name;
        }
    }
}


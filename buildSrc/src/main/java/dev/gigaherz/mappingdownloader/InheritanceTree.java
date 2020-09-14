package dev.gigaherz.mappingdownloader;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

public class InheritanceTree
{
    public static final Gson INSTANCE = (new GsonBuilder())
            .create();

    public final Map<String, InhClass> classes;

    public InheritanceTree()
    {
        classes = new HashMap<>();
    }

    public InheritanceTree(Reader r)
    {
        classes = INSTANCE.fromJson(r, new TypeToken<HashMap<String, InhClass>>(){}.getType());
    }

    public InheritanceTree remap(MappingData mapping)
    {
        InheritanceTree target = new InheritanceTree();
        for(Map.Entry<String, InhClass> en : classes.entrySet())
        {
            target.mapClass(en.getKey(), en.getValue(), mapping);
        }
        return target;
    }

    private void mapClass(String classObf, InhClass classData, MappingData tsrg)
    {
        MappingData.TsrgClass classSrg = tsrg.classes.get(classObf);
        InhClass mappedClass = new InhClass();
        mappedClass.name = classSrg.name;
        mappedClass.access = classData.access;
        mappedClass.superName = getClassNameOrDefault(tsrg, classData.superName);
        mappedClass.interfaces = mappedClass.interfaces.stream().map(obf -> getClassNameOrDefault(tsrg, obf)).collect(Collectors.toList());
        for(Map.Entry<String,InhMethod> en : classData.methods.entrySet())
        {
            String obfSignature = en.getKey();
            String methodSrg;
            if (obfSignature.startsWith("<init>"))
                methodSrg=remapSignature("<init>", obfSignature);
            else
                methodSrg=classSrg.methods.getOrDefault(obfSignature, obfSignature);
            InhMethod methodData = en.getValue();
            InhMethod mappedMethod = new InhMethod();
            mappedMethod.access = methodData.access;
            mappedMethod.override = getClassNameOrDefault(tsrg, methodData.override);
            mappedClass.methods.put(methodSrg, mappedMethod);
        }
        classes.put(classSrg.name, mappedClass);
    }

    private String remapSignature(String methodName, String obfSignature)
    {
        // TODO
        return obfSignature;
    }

    private String getClassNameOrDefault(MappingData tsrg, String classObf)
    {
        if (tsrg.classes.containsKey(classObf))
            return tsrg.classes.get(classObf).name;
        return classObf;
    }

    public static class InhClass
    {
        public String name;
        public int access;
        public String superName;
        public List<String> interfaces = new ArrayList<>();
        public Map<String, InhMethod> methods = new HashMap<>();
    }

    public static class InhMethod
    {
        public int access;
        public String override; // Class it overrides from
    }
}


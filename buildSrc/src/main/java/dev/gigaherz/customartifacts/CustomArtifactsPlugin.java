package dev.gigaherz.customartifacts;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;

import javax.inject.Inject;

public class CustomArtifactsPlugin implements Plugin<Project>
{
    private final SoftwareComponentFactory softwareComponentFactory;

    @Inject
    public CustomArtifactsPlugin(SoftwareComponentFactory softwareComponentFactory) {
        this.softwareComponentFactory = softwareComponentFactory;
    }

    private Configuration createOutgoingConfiguration(Project project) {
        return project.getConfigurations().create("mappingsExports", cnf -> {
            cnf.setCanBeConsumed(true);
            cnf.setCanBeResolved(false);
            cnf.attributes(it -> {
                it.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                it.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                it.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, Integer.parseInt(JavaVersion.current().getMajorVersion()));
                it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.RESOURCES));
            });
        });
    }

    @Override
    public void apply(Project project) {
        // create an adhoc component
        AdhocComponentWithVariants adhocComponent = softwareComponentFactory.adhoc("mappingsComponent");
        // add it to the list of components that this project declares
        project.getComponents().add(adhocComponent);
        // and register a variant for publication
        adhocComponent.addVariantsFromConfiguration(createOutgoingConfiguration(project), it -> {
            it.mapToMavenScope("runtime");
        });
    }
}

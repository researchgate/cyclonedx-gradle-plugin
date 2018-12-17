package org.cyclonedx.gradle

import com.github.packageurl.PackageURL
import org.cyclonedx.BomGenerator
import org.cyclonedx.BomParser
import org.cyclonedx.model.Component
import org.cyclonedx.util.BomUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.TaskAction

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException

class CycloneBomGeneratoinTask extends DefaultTask {

    @TaskAction
    void cycloneBomGeneration() {
        Set<ResolvedArtifact> firstLevelModuleArtifacts = project.configurations.getByName('runtime').resolvedConfiguration.resolvedArtifacts

        Set<Component> components = new HashSet<>()

        firstLevelModuleArtifacts.each { dep ->
            components.add(convertToComponent(dep))
        }

        try {
            final BomGenerator bomGenerator = new BomGenerator(components)
            bomGenerator.generate()
            final String bomString = bomGenerator.toXmlString()
            final File bomFile = new File(project.getBuildDir(), "cycloneDx/bom.xml")
            project.logger.info('Writing bom file')
            bomFile.getParentFile().mkdirs()
            bomFile.createNewFile()
            bomFile.write(bomString)

            project.logger.info('Validating bom file')
            final BomParser bomParser = new BomParser()
            if (!bomParser.isValid(bomFile)) {
                throw new GradleException('Invalid bom file')
            }
        } catch (ParserConfigurationException | TransformerException | IOException e) {
            throw new GradleException("An error occurred executing " + this.getClass().getName(), e)
        }
    }

    Component convertToComponent(ResolvedArtifact dep) {
        Component component = new Component()
        ModuleVersionIdentifier moduleId = dep.getModuleVersion().id
        component.setGroup(moduleId.group)
        component.setName(moduleId.name)
        component.setVersion(moduleId.version)
        component.setType("library")
        component.setHashes(BomUtils.calculateHashes(dep.file))
        Map<String, String> qualifiers = new TreeMap<>()
        if (dep.type != null) {
            qualifiers.put('type', dep.type)
        }
        if (dep.classifier != null) {
            qualifiers.put('classifier', dep.classifier)
        }
        if (dep.extension != null) {
            qualifiers.put('extension', dep.extension)
        }
        component.setPurl(new PackageURL(PackageURL.StandardTypes.MAVEN, moduleId.group, moduleId.name, moduleId.version, qualifiers, null))
        return component
    }
}

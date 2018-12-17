package org.cyclonedx.gradle

import org.cyclonedx.BomGenerator
import org.cyclonedx.BomParser
import org.cyclonedx.model.Component
import org.cyclonedx.util.BomUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.TaskAction

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException

class CycloneBomGeneratoinTask extends DefaultTask {

    @TaskAction
    void cycloneBomGeneration() {
        Set<ResolvedDependency> firstLevelModuleDependencies = project.configurations.getByName('runtime').resolvedConfiguration.firstLevelModuleDependencies

        Set<Component> components = new HashSet<>()
        firstLevelModuleDependencies.each { dep ->
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

    Component convertToComponent(ResolvedDependency dep) {
        Component component = new Component()
        component.setGroup(dep.getModuleGroup())
        component.setName(dep.getName())
        component.setVersion(dep.getModuleVersion())
        component.setType("library")
//        component.setHashes(BomUtils.calculateHashes(dep.getModuleArtifacts().first().file))
        return component
    }
}

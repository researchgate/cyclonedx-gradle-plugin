package org.cyclonedx.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class CycloneDxBomPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().create('genrateCycloneDxBom', CycloneBomGeneratoinTask)
    }

}

File buildLog = new File(basedir, "build.log")

List<String> lines  = buildLog.readLines()

int deactivationOfDeployPluginIndex = 0
int deactivationOfInstallPluginIndex  = 0
int deployerExtensionIndex  = 0
int installingArtifactIndex  = 0
int deployingArtifactIndex  = 0
for(String line : lines ) {
    if(line.contains("org.apache.maven.plugins:maven-deploy-plugin:deploy has been deactivated.")) {
        deactivationOfDeployPluginIndex = lines.indexOf(line)
    }

    if(line.contains("org.apache.maven.plugins:maven-install-plugin:install has been deactivated.")) {
        deactivationOfInstallPluginIndex = lines.indexOf(line)
    }

    if(line.contains("--- maven-deployer-extension")) {
        deployerExtensionIndex = lines.indexOf(line)
    }

    if(line.contains("Installing artifacts...")) {
        installingArtifactIndex = lines.indexOf(line)
    }

    if(line.contains("Deploying artifacts...")) {
        deployingArtifactIndex = lines.indexOf(line)
    }
}

assert deactivationOfDeployPluginIndex != 0
assert deactivationOfInstallPluginIndex != 0
assert deployerExtensionIndex != 0
assert installingArtifactIndex != 0 && installingArtifactIndex > deployerExtensionIndex
assert deployingArtifactIndex !=0 && deployingArtifactIndex > deployerExtensionIndex

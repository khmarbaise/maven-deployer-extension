package com.soebes.maven.extensions.deployer;

import javax.inject.Inject;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Deployer
{
    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    @Inject
    private InstallationCollector collector;

    public Deployer()
    {
    }

    public void deployArtifacts()
    {
        for ( Artifact itemToDeploy : collector.getInstalledArtifacts() )
        {
            LOGGER.info( "Deploy: " + itemToDeploy.toString() );
        }
    }
}

package com.soebes.maven.extensions.deployer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployer;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DeployProject
{
    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    @Inject
    private ArtifactDeployer deployer;

    /**
     * This will deploy a single project which may contain several artifacts
     * into the appropriate remote repository.
     * 
     * @param buildingRequest {@link ProjectBuildingRequest}
     * @param request {@link DeployRequest}
     * @param artifactRepository {@link ArtifactRepository}
     */
    public void deployProject( ProjectBuildingRequest buildingRequest, DeployRequest request,
                               ArtifactRepository artifactRepository )
    {
        List<Artifact> deployableArtifacts = new ArrayList<Artifact>();

        Artifact artifact = request.getProject().getArtifact();
        String packaging = request.getProject().getPackaging();
        File pomFile = request.getProject().getFile();

        List<Artifact> attachedArtifacts = request.getProject().getAttachedArtifacts();

        LOGGER.debug( "Deployment repo:" + artifactRepository );
        // Deploy the POM
        boolean isPomArtifact = "pom".equals( packaging );
        if ( !isPomArtifact )
        {
            ProjectArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pomFile );
            artifact.addMetadata( metadata );
        }
        else
        {
            artifact.setFile( pomFile );
        }

        if ( request.isUpdateReleaseInfo() )
        {
            artifact.setRelease( true );
        }

        artifact.setRepository( artifactRepository );

        int retryFailedDeploymentCount = request.getRetryFailedDeploymentCount();

        try
        {
            if ( isPomArtifact )
            {
                deployableArtifacts.add( artifact );
            }
            else
            {
                File file = artifact.getFile();

                if ( file != null && file.isFile() )
                {
                    deployableArtifacts.add( artifact );
                }
                else if ( !attachedArtifacts.isEmpty() )
                {
                    throw new IllegalArgumentException( "The packaging plugin for this project did not assign "
                        + "a main file to the project but it has attachments. Change packaging to 'pom'." );
                }
                else
                {
                    throw new IllegalArgumentException( "The packaging for this project did not assign "
                        + "a file to the build artifact" );
                }
            }

            for ( Artifact attached : attachedArtifacts )
            {
                // This is here when AttachedArtifact is used, like m-sources-plugin:2.0.4
                try
                {
                    attached.setRepository( artifactRepository );
                }
                catch ( UnsupportedOperationException e )
                {
                    LOGGER.warn( attached.getId() + " has been attached with deprecated code, "
                        + "try to upgrade the responsible plugin" );
                }

                deployableArtifacts.add( attached );
            }

            deploy( buildingRequest, deployableArtifacts, artifactRepository, retryFailedDeploymentCount );
        }
        catch ( ArtifactDeployerException e )
        {
            throw new IllegalArgumentException( e.getMessage(), e );
        }
    }

    private void deploy( ProjectBuildingRequest request, Collection<Artifact> artifacts,
                         ArtifactRepository deploymentRepository, int retryFailedDeploymentCount )
        throws ArtifactDeployerException
    {

        // for now retry means redeploy the complete artifacts collection
        int retryFailedDeploymentCounter = Math.max( 1, Math.min( 10, retryFailedDeploymentCount ) );
        ArtifactDeployerException exception = null;
        for ( int count = 0; count < retryFailedDeploymentCounter; count++ )
        {
            try
            {
                if ( count > 0 )
                {
                    LOGGER.info( "Retrying deployment attempt " + ( count + 1 ) + " of "
                        + retryFailedDeploymentCounter );
                }

                deployer.deploy( request, deploymentRepository, artifacts );
                exception = null;
                break;
            }
            catch ( ArtifactDeployerException e )
            {
                if ( count + 1 < retryFailedDeploymentCounter )
                {
                    LOGGER.warn( "Encountered issue during deployment: " + e.getLocalizedMessage() );
                    LOGGER.debug( e.getMessage() );
                }
                if ( exception == null )
                {
                    exception = e;
                }
            }
        }
        if ( exception != null )
        {
            throw exception;
        }
    }

}

package com.soebes.maven.extensions.deployer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployer;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
@Singleton
@Named( value = "deployer" )
public class MavenDeployer
    extends AbstractEventSpy
{
    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    @Inject
    private ArtifactDeployer deployer;

    public MavenDeployer()
    {
    }

    @Override
    public void init( Context context )
        throws Exception
    {
        super.init( context );
        logDeployerVersion();
    }

    private void logDeployerVersion()
    {
        LOGGER.info( "" );
        LOGGER.info( " --- maven-deployer-extension:{} --- ", MavenDeployerExtensionVersion.getVersion() );
    }

    @Override
    public void onEvent( Object event )
        throws Exception
    {
        try
        {
            if ( event instanceof ExecutionEvent )
            {
                executionEventHandler( (ExecutionEvent) event );
            }
        }
        catch ( Exception e )
        {
            LOGGER.error( "Exception", e );
        }
    }

    @Override
    public void close()
    {
        LOGGER.debug( "Maven Deployer Extension." );
    }

    private boolean goalsContain( ExecutionEvent executionEvent, String goal )
    {
        return executionEvent.getSession().getGoals().contains( goal );
    }

    private void executionEventHandler( ExecutionEvent executionEvent )
    {
        Type type = executionEvent.getType();
        switch ( type )
        {
            case ProjectDiscoveryStarted:
                break;
            case SessionStarted:
                // Reading of pom files done and structure now there.
                sessionStarted( executionEvent );
                break;
            case SessionEnded:
                // Everything is done.
                sessionEnded( executionEvent );
                break;

            case ForkStarted:
            case ForkFailed:
            case ForkSucceeded:
            case ForkedProjectStarted:
            case ForkedProjectFailed:
            case ForkedProjectSucceeded:
            case MojoStarted:
            case MojoFailed:
            case MojoSucceeded:
            case MojoSkipped:
            case ProjectStarted:
            case ProjectFailed:
            case ProjectSucceeded:
            case ProjectSkipped:
                break;

            default:
                LOGGER.error( "executionEventHandler: {}", type );
                break;
        }

    }

    private void sessionEnded( ExecutionEvent executionEvent )
    {
        if ( goalsContain( executionEvent, "deploy" ) )
        {
            logDeployerVersion();
            deployArtifacts( executionEvent );
        }
        else
        {
            logDeployerVersion();
            LOGGER.info( " skipping." );
        }
    }

    private void sessionStarted( ExecutionEvent executionEvent )
    {
        if ( goalsContain( executionEvent, "deploy" ) )
        {
            removeDeployPluginFromLifeCycle( executionEvent );
        }
    }

    private void removeDeployPluginFromLifeCycle( ExecutionEvent executionEvent )
    {
        removePluginFromLifeCycle( executionEvent, "org.apache.maven.plugins", "maven-deploy-plugin", "deploy" );
    }

    private void removePluginFromLifeCycle( ExecutionEvent executionEvent, String groupId, String artifactId,
                                            String goal )
    {

        boolean removed = false;
        List<MavenProject> sortedProjects = executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            List<Plugin> buildPlugins = mavenProject.getBuildPlugins();
            for ( Plugin plugin : buildPlugins )
            {
                if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                {
                    if ( !removed )
                    {
                        LOGGER.warn( groupId + ":" + artifactId + ":" + goal + " has been deactivated." );
                    }
                    List<PluginExecution> executions = plugin.getExecutions();
                    for ( PluginExecution pluginExecution : executions )
                    {
                        pluginExecution.removeGoal( goal );
                        removed = true;
                    }
                }
            }

        }
    }

    private void deployArtifacts( ExecutionEvent executionEvent )
    {
        List<MavenProject> sortedProjects = executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            LOGGER.info( "Deploying artifact: " + mavenProject.getId() );
            DeployRequest currentExecutionDeployRequest =
                new DeployRequest().setProject( mavenProject ).setUpdateReleaseInfo( true );

            deployProject( executionEvent, currentExecutionDeployRequest );

        }
    }

    private void deployProject( ExecutionEvent executionEvent, DeployRequest request )
    {
        List<Artifact> deployableArtifacts = new ArrayList<Artifact>();

        Artifact artifact = request.getProject().getArtifact();
        String packaging = request.getProject().getPackaging();
        File pomFile = request.getProject().getFile();

        List<Artifact> attachedArtifacts = request.getProject().getAttachedArtifacts();

        ArtifactRepository repo = request.getProject().getDistributionManagementArtifactRepository();

        LOGGER.debug( "Deployment repo:" + repo );
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

        artifact.setRepository( repo );

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
                    attached.setRepository( repo );
                }
                catch ( UnsupportedOperationException e )
                {
                    LOGGER.warn( attached.getId() + " has been attached with deprecated code, "
                        + "try to upgrade the responsible plugin" );
                }

                deployableArtifacts.add( attached );
            }

            deploy( executionEvent, deployableArtifacts, repo, retryFailedDeploymentCount );
        }
        catch ( ArtifactDeployerException e )
        {
            throw new IllegalArgumentException( e.getMessage(), e );
        }
    }

    private void deploy( ExecutionEvent executionEvent, Collection<Artifact> artifacts,
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

                deployer.deploy( executionEvent.getSession().getProjectBuildingRequest(), deploymentRepository,
                                 artifacts );
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

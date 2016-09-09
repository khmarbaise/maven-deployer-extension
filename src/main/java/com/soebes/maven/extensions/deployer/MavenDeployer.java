package com.soebes.maven.extensions.deployer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
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
    private DeployProject deployProject;

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
                sessionStarted( executionEvent );
                break;
            case SessionEnded:
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
        ArtifactRepository repository =
                        executionEvent.getSession().getTopLevelProject().getDistributionManagementArtifactRepository();

        List<MavenProject> sortedProjects = executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            DeployRequest deployRequest = new DeployRequest().setProject( mavenProject ).setUpdateReleaseInfo( true );

            deployProject.deployProject( executionEvent.getSession().getProjectBuildingRequest(), deployRequest,
                                         repository );
        }
    }

}

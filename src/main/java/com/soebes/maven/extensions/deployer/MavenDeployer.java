package com.soebes.maven.extensions.deployer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.deployment.DeployRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
@Named
@Singleton
public class MavenDeployer
    extends AbstractEventSpy
{
    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    @Inject
    public MavenDeployer()
    {
    }

    @Override
    public void init( Context context )
        throws Exception
    {
        super.init( context );
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
            else if ( event instanceof org.eclipse.aether.RepositoryEvent )
            {
            }
            else if ( event instanceof MavenExecutionRequest )
            {
            }
            else if ( event instanceof MavenExecutionResult )
            {
            }
            else if ( event instanceof DependencyResolutionRequest )
            {
            }
            else if ( event instanceof DependencyResolutionResult )
            {
            }
            else
            {
                // TODO: What kind of event we haven't considered?
                LOGGER.debug( "Event {}", event.getClass().getCanonicalName() );
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

    private void executionEventHandler( ExecutionEvent executionEvent )
    {
        Type type = executionEvent.getType();
        switch ( type )
        {
            case ProjectDiscoveryStarted:
                // Start reading the pom files..
                break;
            case SessionStarted:
                LOGGER.info( "Maven Deployer Extension {}", MavenDeployerExtensionVersion.getVersion() + " loaded." );
                //executionEvent.getSession().getGoals()
                // Reading of pom files done and structure now there.
                // executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
                break;
            case SessionEnded:
                // Everything is done.
                deployArtifacts( executionEvent );
                break;

            case ForkStarted:
                break;
            case ForkFailed:
            case ForkSucceeded:
                break;

            case ForkedProjectStarted:
                break;
            case ForkedProjectFailed:
            case ForkedProjectSucceeded:
                break;

            case MojoStarted:
                break;

            case MojoFailed:
            case MojoSucceeded:
            case MojoSkipped:
                break;

            case ProjectStarted:
                break;

            case ProjectFailed:
            case ProjectSucceeded:
            case ProjectSkipped:
                break;

            default:
                LOGGER.error( "executionEventHandler: {}", type );
                break;
        }

    }

    private void deployArtifacts( ExecutionEvent executionEvent )
    {
        List<String> goals = executionEvent.getSession().getGoals();

        if ( goals.contains( "deploy" ) )
        {
            List<MavenProject> sortedProjects =
                executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
            LOGGER.info( "" );
            LOGGER.info( " --- maven-deployer-extension:{} --- ", MavenDeployerExtensionVersion.getVersion() );
            for ( MavenProject mavenProject : sortedProjects )
            {
                LOGGER.info( " Deploying " + mavenProject.getId() );
                DeployRequest currentExecutionDeployRequest =
                                new DeployRequest().setProject( mavenProject ).setUpdateReleaseInfo( true );

            }
        }
        else
        {
            LOGGER.info( "" );
            LOGGER.info( " --- maven-deployer-extension:{} --- ", MavenDeployerExtensionVersion.getVersion() );
            LOGGER.info( " skipping." );
        }
    }

}

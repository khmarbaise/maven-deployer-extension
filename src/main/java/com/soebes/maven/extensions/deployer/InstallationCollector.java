package com.soebes.maven.extensions.deployer;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
@Named
@Singleton
public class InstallationCollector
    extends AbstractEventSpy
{
    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    @Inject
    public InstallationCollector()
    {
        LOGGER.debug( "Installation collector." );
    }

    @Override
    public void init( Context context )
        throws Exception
    {
        super.init( context );

        LOGGER.info( "    __  ___                          ____           __        ____             ______     __                  _");
        LOGGER.info( "   /  |/  /___ __   _____  ____     /  _/___  _____/ /_____ _/ / /__  _____   / ____/  __/ /____  ____  _____(_)___  ____"); 
        LOGGER.info( "  / /|_/ / __ `/ | / / _ \\/ __ \\    / // __ \\/ ___/ __/ __ `/ / / _ \\/ ___/  / __/ | |/_/ __/ _ \\/ __ \\/ ___/ / __ \\/ __ \\");
        LOGGER.info( " / /  / / /_/ /| |/ /  __/ / / /  _/ // / / (__  ) /_/ /_/ / / /  __/ /     / /____>  </ /_/  __/ / / (__  ) / /_/ / / / /");
        LOGGER.info( "/_/  /_/\\__,_/ |___/\\___/_/ /_/  /___/_/ /_/____/\\__/\\__,_/_/_/\\___/_/     /_____/_/|_|\\__/\\___/_/ /_/____/_/\\____/_/ /_/");
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
                repositoryEventHandler( (RepositoryEvent) event );
            }
            else if ( event instanceof MavenExecutionRequest )
            {
                executionRequestEventHandler( (MavenExecutionRequest) event );
            }
            else if ( event instanceof MavenExecutionResult )
            {
                executionResultEventHandler( (MavenExecutionResult) event );
            }
            else if ( event instanceof DependencyResolutionRequest )
            {
                dependencyResolutionRequest( (DependencyResolutionRequest) event );
            }
            else if ( event instanceof DependencyResolutionResult )
            {
                dependencyResolutionResult( (DependencyResolutionResult) event );
            }
            // The following event type is available since Maven 3.3.1+
            // else if ( event instanceof DefaultSettingsBuildingRequest) {
            // DefaultSettingsBuildingRequest r = null;
            // r.getGlobalSettingsFile();
            // r.getGlobalSettingsSource();
            // r.getSystemProperties();
            // r.getUserSettingsFile();
            // r.getUserSettingsSource();
            //
            // r.setGlobalSettingsFile( globalSettingsFile );
            // r.setGlobalSettingsSource( globalSettingsSource );
            // r.setSystemProperties( systemProperties );
            // r.setUserProperties( userProperties );
            // r.setUserSettingsFile( userSettingsFile );
            // r.setUserSettingsSource( userSettingsSource );
            // }
            // The following event type is available since Maven 3.3.1+
            // else if (event instanceof DefaultSettingsBuildingRequest) {
            //
            // DefaultSettingsBuildingRequest r = null;
            // r.getGlobalSettingsSource().getLocation()
            // }
            // The following event type is available since Maven 3.3.1+
            // else if (event instanceof DefaultToolchainsBuildingRequest) {
            // DefaultToolchainsBuildingRequest r = null;
            // r.getGlobalToolchainsSource().
            // }
            // The following event type is available since Maven 3.3.1+
            // else if (event instanceof DefaultToolchainsBuildingResult) {
            // DefaultToolchainsBuildingResult r = null;
            // r.getEffectiveToolchains();
            // r.getProblems();
            // }
            else
            {
                // TODO: What kind of event we haven't considered?
                LOGGER.debug( "MBTP: Event {}", event.getClass().getCanonicalName() );
            }
        }
        catch ( Exception e )
        {
            LOGGER.error( "MBTP: Exception", e );
        }
    }

    @Override
    public void close()
    {
        LOGGER.debug( "MBTP: done." );
    }

    private void dependencyResolutionResult( DependencyResolutionResult event )
    {
        LOGGER.debug( "MBTP: dependencyResolutionResult() {}", event.getResolvedDependencies().size() );
    }

    private void dependencyResolutionRequest( DependencyResolutionRequest event )
    {
        LOGGER.debug( "MBTP: dependencyResolutionRequest()" );
    }

    private void repositoryEventHandler( org.eclipse.aether.RepositoryEvent repositoryEvent )
    {
        EventType type = repositoryEvent.getType();
        switch ( type )
        {
            case ARTIFACT_DOWNLOADING:
                LOGGER.debug( "MBTP: repositoryEventHandler {}", type );
                break;
            case ARTIFACT_DOWNLOADED:
                LOGGER.debug( "MBTP: repositoryEventHandler {}", type );
                break;

            case ARTIFACT_DEPLOYING:
                break;
            case ARTIFACT_DEPLOYED:
                break;

            case ARTIFACT_INSTALLING:
                break;
            case ARTIFACT_INSTALLED:
                break;

            case METADATA_DEPLOYING:
                break;
            case METADATA_DEPLOYED:
                break;

            case METADATA_INSTALLING:
                LOGGER.debug( "MBTP: repositoryEventHandler {}", type );
                break;
            case METADATA_INSTALLED:
                LOGGER.debug( "MBTP: repositoryEventHandler {}", type );
                break;

            case ARTIFACT_RESOLVING:
            case ARTIFACT_RESOLVED:
            case ARTIFACT_DESCRIPTOR_INVALID:
            case ARTIFACT_DESCRIPTOR_MISSING:
            case METADATA_RESOLVED:
            case METADATA_RESOLVING:
            case METADATA_INVALID:
                // Those events are not recorded.
                break;

            default:
                LOGGER.error( "MBTP: repositoryEventHandler {}", type );
                break;
        }
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
                // Reading of pom files done and structure now there.
                // executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
                break;
            case SessionEnded:
                // Everything is done.
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
                LOGGER.error( "MBTP: executionEventHandler: {}", type );
                break;
        }

    }

    private void executionRequestEventHandler( MavenExecutionRequest event )
    {
        LOGGER.debug( "MBTP: executionRequestEventHandler: {}", event.getExecutionListener() );
    }

    private void executionResultEventHandler( MavenExecutionResult event )
    {
    }

}

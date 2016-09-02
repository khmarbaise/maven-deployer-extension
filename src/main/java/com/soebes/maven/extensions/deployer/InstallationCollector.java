package com.soebes.maven.extensions.deployer;

import java.util.Collections;
import java.util.LinkedList;
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
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.artifact.Artifact;
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

    private List<Artifact> installedArtifacts;

    @Inject
    public InstallationCollector()
    {
        installedArtifacts = Collections.<Artifact>synchronizedList( new LinkedList<Artifact>() );
//        LOGGER.info( "Installation collector." );
    }

    public List<Artifact> getInstalledArtifacts()
    {
        return this.installedArtifacts;
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
        LOGGER.debug( "MBTP: done." );
    }

    private void dependencyResolutionResult( DependencyResolutionResult event )
    {
        LOGGER.debug( "MBTP: dependencyResolutionResult() {}", event.getResolvedDependencies().size() );
    }

    private void dependencyResolutionRequest( DependencyResolutionRequest event )
    {
    }

    private void repositoryEventHandler( org.eclipse.aether.RepositoryEvent repositoryEvent )
    {
        EventType type = repositoryEvent.getType();
        switch ( type )
        {
            case ARTIFACT_DOWNLOADING:
                break;
            case ARTIFACT_DOWNLOADED:
                break;

            case ARTIFACT_DEPLOYING:
                break;
            case ARTIFACT_DEPLOYED:
                break;

            case ARTIFACT_INSTALLING:
                break;
            case ARTIFACT_INSTALLED:
                // repositoryEvent.getArtifact().
                installedArtifacts.add( repositoryEvent.getArtifact() );
                break;

            case METADATA_DEPLOYING:
                break;
            case METADATA_DEPLOYED:
                break;

            case METADATA_INSTALLING:
                break;
            case METADATA_INSTALLED:
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
                LOGGER.info( "Maven Deployer Extension {}", MavenDeployerExtensionVersion.getVersion() + " loaded.");
                // Reading of pom files done and structure now there.
                // executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
                break;
            case SessionEnded:
                // Everything is done.
                deployArtifacts();
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

    private void deployArtifacts()
    {
        LOGGER.info( "" );
        LOGGER.info( " --- maven-deployer-extension:{} --- ", MavenDeployerExtensionVersion.getVersion() );
        for ( Artifact itemToDeploy : this.getInstalledArtifacts() )
        {
            LOGGER.info( "Deploy: " + itemToDeploy.toString() );
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

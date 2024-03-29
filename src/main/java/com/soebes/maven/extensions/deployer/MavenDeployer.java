package com.soebes.maven.extensions.deployer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.deploy.ArtifactDeployerException;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstallerException;
import org.apache.maven.shared.transfer.project.NoFileAssignedException;
import org.apache.maven.shared.transfer.project.deploy.ProjectDeployer;
import org.apache.maven.shared.transfer.project.deploy.ProjectDeployerRequest;
import org.apache.maven.shared.transfer.project.install.ProjectInstaller;
import org.apache.maven.shared.transfer.project.install.ProjectInstallerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link EventSpy} implementation will handle the events of SessionEnd to identify the correct point in time to
 * deploy all artifacts of the project into the given remote repository. This will also work if you are using plugins
 * which define their own lifecycle.
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
@Singleton
@Named
public class MavenDeployer
    extends AbstractEventSpy
{
    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProjectDeployer projectDeployer;

    @Inject
    private ProjectInstaller projectInstaller;

    private boolean failure;

    public MavenDeployer()
    {
        this.failure = false;
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
            // We are only interested in the ExecutionEvent.
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
        // TODO: Check if we need to do something here?
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
                if ( this.failure )
                {
                    LOGGER.warn( "The Maven Deployer Extension will not be called based on previous errors." );
                }
                else
                {
                    sessionEnded( executionEvent );
                }
                break;
            case ForkFailed:
            case ForkedProjectFailed:
            case MojoFailed:
            case ProjectFailed:
                // TODO: Can we find out more about the cause of failure?
                LOGGER.debug( "Some failure has occurred." );
                this.failure = true;
                break;

            case ForkStarted:
            case ForkSucceeded:
            case ForkedProjectStarted:
            case ForkedProjectSucceeded:
            case MojoStarted:
            case MojoSucceeded:
            case MojoSkipped:
            case ProjectStarted:
            case ProjectSucceeded:
            case ProjectSkipped:
                break;

            default:
                LOGGER.error( "executionEventHandler: {}", type );
                break;
        }

    }

    /**
     * This will start to deploy all artifacts into remote repository if the goal {@code deploy} has been called.
     * 
     * @param executionEvent
     */
    private void sessionEnded( ExecutionEvent executionEvent )
    {
        logDeployerVersion();

        if ( goalsContain( executionEvent, "install" ) )
        {
            installArtifacts( executionEvent );
        } else if ( goalsContain( executionEvent, "deploy" ) )
        {
            installArtifacts( executionEvent );
            LOGGER.info( "" );
            LOGGER.info( "Deploying artifacts..." );
            deployProjects( executionEvent );
        }
        else
        {
            LOGGER.info( " Deployment has been skipped." );
        }
    }

    private void installArtifacts( ExecutionEvent executionEvent )
    {
        LOGGER.info( "" );
        LOGGER.info( "Installing artifacts..." );
        installProjects( executionEvent );
    }

    private void sessionStarted( ExecutionEvent executionEvent )
    {
        if ( containsLifeCycleDeployPluginGoal( executionEvent, "deploy" ) )
        {
            removeDeployPluginFromLifeCycle( executionEvent );
        }

        if ( containsLifeCycleInstallPluginGoal( executionEvent, "install" ) )
        {
            removeInstallPluginFromLifeCycle( executionEvent );
        }
    }

    private boolean containsLifeCycleDeployPluginGoal( ExecutionEvent executionEvent, String goal )
    {
        return containsLifeCyclePluginGoals( executionEvent, "org.apache.maven.plugins", "maven-deploy-plugin", goal );
    }

    private boolean containsLifeCycleInstallPluginGoal( ExecutionEvent executionEvent, String goal )
    {
        return containsLifeCyclePluginGoals( executionEvent, "org.apache.maven.plugins", "maven-install-plugin", goal );
    }

    private void removeDeployPluginFromLifeCycle( ExecutionEvent executionEvent )
    {
        removePluginFromLifeCycle( executionEvent, "org.apache.maven.plugins", "maven-deploy-plugin", "deploy" );
    }

    private void removeInstallPluginFromLifeCycle( ExecutionEvent executionEvent )
    {
        removePluginFromLifeCycle( executionEvent, "org.apache.maven.plugins", "maven-install-plugin", "install" );
    }

    private boolean containsLifeCyclePluginGoals( ExecutionEvent executionEvent, String groupId, String artifactId,
                                                  String goal )
    {

        boolean result = false;
        List<MavenProject> sortedProjects = executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            List<Plugin> buildPlugins = mavenProject.getBuildPlugins();
            for ( Plugin plugin : buildPlugins )
            {
                if ( groupId.equals( plugin.getGroupId() ) && artifactId.equals( plugin.getArtifactId() ) )
                {
                    List<PluginExecution> executions = plugin.getExecutions();
                    for ( PluginExecution pluginExecution : executions )
                    {
                        if ( pluginExecution.getGoals().contains( goal ) )
                        {
                            result = true;
                        }
                    }
                }
            }
        }
        return result;
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
                LOGGER.debug( "Plugin: " + plugin.getId() );
                List<PluginExecution> printExecutions = plugin.getExecutions();
                for ( PluginExecution pluginExecution : printExecutions )
                {
                    LOGGER.debug( "  -> " + pluginExecution.getGoals() );
                }

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

    private void deployProjects( ExecutionEvent executionEvent )
    {
        // Assumption is to have the distributionManagement in the top level
        // pom file located.
        ArtifactRepository repository =
            executionEvent.getSession().getTopLevelProject().getDistributionManagementArtifactRepository();

        List<MavenProject> sortedProjects = executionEvent.getSession().getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            ProjectDeployerRequest deployRequest =
                new ProjectDeployerRequest().setProject( mavenProject );

            deployProject( executionEvent.getSession().getProjectBuildingRequest(), deployRequest, repository );
        }
    }

    private void installProjects( ExecutionEvent exec )
    {
        List<MavenProject> sortedProjects = exec.getSession().getProjectDependencyGraph().getSortedProjects();
        for ( MavenProject mavenProject : sortedProjects )
        {
            ProjectInstallerRequest pir =
                new ProjectInstallerRequest().setProject( mavenProject );

            installProject( exec.getSession().getProjectBuildingRequest(), pir );
        }

    }

    private void deployProject( ProjectBuildingRequest projectBuildingRequest, ProjectDeployerRequest deployRequest,
                                ArtifactRepository repository )
    {
        try
        {
            projectDeployer.deploy( projectBuildingRequest, deployRequest, repository );
        }
        catch ( IllegalArgumentException e )
        {
            LOGGER.error( "IllegalArgumentException", e );
        }
        catch ( NoFileAssignedException e )
        {
            LOGGER.error( "NoFileAssignedException", e );
        }
        catch ( ArtifactDeployerException e )
        {
            LOGGER.error( "ArtifactDeployerException", e );
        }

    }

    private void installProject( ProjectBuildingRequest pbr, ProjectInstallerRequest pir )
    {
        try
        {
            projectInstaller.install( pbr, pir );
        }
        catch ( IOException e )
        {
            LOGGER.error( "IOException", e );
        }
        catch ( ArtifactInstallerException e )
        {
            LOGGER.error( "ArtifactInstallerException", e );
        }
        catch ( NoFileAssignedException e )
        {
            LOGGER.error( "NoFileAssignedException", e );
        }
    }

}

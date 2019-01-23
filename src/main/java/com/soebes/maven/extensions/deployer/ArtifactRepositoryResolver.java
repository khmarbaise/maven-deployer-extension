package com.soebes.maven.extensions.deployer;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.project.deploy.ProjectDeployerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactRepositoryResolver {

    public static final String ALT_DEPLOYMENT_REPOSITORY = "altDeploymentRepository";
    public static final String ALT_SNAPSHOT_DEPLOYMENT_REPOSITORY = "altSnapshotDeploymentRepository";
    public static final String ALT_RELEASE_DEPLOYMENT_REPOSITORY = "altReleaseDeploymentRepository";
    public static final String RETRY_FAILED_DEPLOYMENT_COUNT = "retryFailedDeploymentCount";


    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.+)" );

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactRepository.class);

    public ArtifactRepository resolveArtifactRepository(ExecutionEvent executionEvent, Properties props)
            throws MojoFailureException, MojoExecutionException {

        //
        // Manually pull in props.
        //
        String altReleaseDeploymentRepository = props.getProperty(ALT_RELEASE_DEPLOYMENT_REPOSITORY);
        String altSnapshotDeploymentRepository = props.getProperty(ALT_SNAPSHOT_DEPLOYMENT_REPOSITORY);
        String altDeploymentRepository = props.getProperty(ALT_DEPLOYMENT_REPOSITORY);
        int retryFailedDeploymentCount = Integer.parseInt(props.getProperty(RETRY_FAILED_DEPLOYMENT_COUNT,"1"));

        LOGGER.debug("Deployment settins from project:\n\t"
                        + "altReleaseDeploymentRepository={}\n\t"
                        + ", altSnapshotDeploymentRepository={}\n\t"
                        + ", altDeploymentRepository={}\n\t"
                        + ", retryFailedDeploymentCount={}",
                altReleaseDeploymentRepository, altSnapshotDeploymentRepository,altDeploymentRepository,retryFailedDeploymentCount);


        ProjectDeployerRequest pdr = new ProjectDeployerRequest()
                .setProject( executionEvent.getSession().getTopLevelProject() )
                .setRetryFailedDeploymentCount( retryFailedDeploymentCount )
                .setAltReleaseDeploymentRepository( altReleaseDeploymentRepository )
                .setAltSnapshotDeploymentRepository( altSnapshotDeploymentRepository )
                .setAltDeploymentRepository( altDeploymentRepository );

        return getDeploymentRepository(pdr);
    }

    /**
     * Code Researched from:  <a href="https://github.com/apache/maven-deploy-plugin/blob/maven-deploy-plugin-3.0.0-M1/src/main/java/org/apache/maven/plugins/deploy/DeployMojo.java>
     *     MavenDeployMojo.java</a>
     * @param pdr
     * @return
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected ArtifactRepository getDeploymentRepository( ProjectDeployerRequest pdr )

            throws MojoExecutionException, MojoFailureException
    {
        MavenProject project = pdr.getProject();
        String altDeploymentRepository = pdr.getAltDeploymentRepository();
        String altReleaseDeploymentRepository = pdr.getAltReleaseDeploymentRepository();
        String altSnapshotDeploymentRepository = pdr.getAltSnapshotDeploymentRepository();

        ArtifactRepository repo = null;

        String altDeploymentRepo;
        if ( ArtifactUtils.isSnapshot( project.getVersion() ) && altSnapshotDeploymentRepository != null )
        {
            altDeploymentRepo = altSnapshotDeploymentRepository;
        }
        else if ( !ArtifactUtils.isSnapshot( project.getVersion() ) && altReleaseDeploymentRepository != null )
        {
            altDeploymentRepo = altReleaseDeploymentRepository;
        }
        else
        {
            altDeploymentRepo = altDeploymentRepository;
        }

        if ( altDeploymentRepo != null )
        {
            LOGGER.info( "Using alternate deployment repository " + altDeploymentRepo );

            Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher( altDeploymentRepo );

            if ( !matcher.matches() )
            {
                throw new MojoFailureException( altDeploymentRepo, "Invalid syntax for repository.",
                        "Invalid syntax for alternative repository. Use \"id::url\"." );
            }
            else
            {
                String id = matcher.group( 1 ).trim();
                String url = matcher.group( 2 ).trim();

                repo = createDeploymentArtifactRepository( id, url );
            }
        }

        if ( repo == null )
        {
            repo = project.getDistributionManagementArtifactRepository();
        }

        if ( repo == null )
        {
            String msg = "Deployment failed: repository element was not specified in the POM inside"
                    + " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

            throw new MojoExecutionException( msg );
        }

        return repo;
    }

    protected ArtifactRepository createDeploymentArtifactRepository( String id, String url )
    {
        return new MavenArtifactRepository( id, url, new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy() );
    }
}

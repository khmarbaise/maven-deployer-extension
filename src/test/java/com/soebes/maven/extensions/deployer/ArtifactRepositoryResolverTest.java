package com.soebes.maven.extensions.deployer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.project.deploy.ProjectDeployerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtifactRepositoryResolverTest {

    @Mock
    private MavenProject mavenProject;

    @Mock
    private ArtifactRepository defaultDistributionManagementRepo;




    @BeforeEach
    public void setUp() {

    }

    @Test
    public void testDefaultDeploymentRepoRequest() throws MojoFailureException, MojoExecutionException {

        when(mavenProject.getDistributionManagementArtifactRepository()).thenReturn(defaultDistributionManagementRepo);
        when(mavenProject.getVersion()).thenReturn("1.0.0");

        //No alt properties
        ProjectDeployerRequest dpr = new ProjectDeployerRequest()
                .setProject(mavenProject);

        ArtifactRepositoryResolver resolver = new ArtifactRepositoryResolver();

        ArtifactRepository resolvedRepo = resolver.getDeploymentRepository(dpr);
        assertEquals(defaultDistributionManagementRepo, resolvedRepo);
    }


    @Test
    public void testAltNormalRepository() throws MojoFailureException, MojoExecutionException {

        when(mavenProject.getVersion()).thenReturn("1.0.0");

        //No alt properties
        ProjectDeployerRequest dpr = new ProjectDeployerRequest()
                .setProject(mavenProject)
                .setAltDeploymentRepository("TestRepo::http://localhost:8080/test");

        ArtifactRepositoryResolver resolver = new ArtifactRepositoryResolver();

        ArtifactRepository resolvedRepo = resolver.getDeploymentRepository(dpr);

        assertEquals("TestRepo", resolvedRepo.getKey());
        assertEquals("http://localhost:8080/test", resolvedRepo.getUrl());

    }

    @Test
    public void testAltSnapshotRepositoryButNormalVersionReturnsNormalRepository()
            throws MojoFailureException, MojoExecutionException {

        when(mavenProject.getDistributionManagementArtifactRepository()).thenReturn(defaultDistributionManagementRepo);
        when(mavenProject.getVersion()).thenReturn("1.0.0");

        //No alt properties
        ProjectDeployerRequest dpr = new ProjectDeployerRequest()
                .setProject(mavenProject)
                .setAltSnapshotDeploymentRepository("TestRepo::http://localhost/");


        ArtifactRepositoryResolver resolver = new ArtifactRepositoryResolver();

        ArtifactRepository resolvedRepo = resolver.getDeploymentRepository(dpr);
        assertEquals(defaultDistributionManagementRepo, resolvedRepo);
    }

    @Test
    public void testAltSnapshotRepository() throws MojoFailureException, MojoExecutionException {
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");

        //No alt properties
        ProjectDeployerRequest dpr = new ProjectDeployerRequest()
                .setProject(mavenProject)
                .setAltSnapshotDeploymentRepository("TestSnapshotRepo::http://localhost:8080/snapshot");

        ArtifactRepositoryResolver resolver = new ArtifactRepositoryResolver();

        ArtifactRepository resolvedRepo = resolver.getDeploymentRepository(dpr);

        assertEquals("TestSnapshotRepo", resolvedRepo.getKey());
        assertEquals("http://localhost:8080/snapshot", resolvedRepo.getUrl());
    }

    @Test
    public void testAltReleaseRepository() throws MojoFailureException, MojoExecutionException {
        when(mavenProject.getVersion()).thenReturn("1.2.0");

        //No alt properties
        ProjectDeployerRequest dpr = new ProjectDeployerRequest()
                .setProject(mavenProject)
                .setAltReleaseDeploymentRepository("TestReleaseRepo::http://localhost:8080/releases");

        ArtifactRepositoryResolver resolver = new ArtifactRepositoryResolver();

        ArtifactRepository resolvedRepo = resolver.getDeploymentRepository(dpr);

        assertEquals("TestReleaseRepo", resolvedRepo.getKey());
        assertEquals("http://localhost:8080/releases", resolvedRepo.getUrl());
    }


    @Test
    public void testNoResolutionThrowsMojoExecutionException() {
        //Attempts resolution of snapshot repo but only deployment repo provided
        when(mavenProject.getVersion()).thenReturn("1.1.0-SNAPSHOT");

        //No alt properties
        ProjectDeployerRequest dpr = new ProjectDeployerRequest()
                .setProject(mavenProject)
                .setAltReleaseDeploymentRepository("TestReleaseRepo::http://localhost:8080/releases");

        ArtifactRepositoryResolver resolver = new ArtifactRepositoryResolver();

        assertThrows(MojoExecutionException.class, () -> resolver.getDeploymentRepository(dpr));
    }


    @Test
    public void testInvalidRepoThrowsMojoFailureExecption() {
        //Attempts resolution of snapshot repo but only deployment repo provided
        when(mavenProject.getVersion()).thenReturn("1.1.0");

        //No alt properties
        ProjectDeployerRequest dpr = new ProjectDeployerRequest()
                .setProject(mavenProject)
                .setAltReleaseDeploymentRepository("http://localhost:8080/releases");

        ArtifactRepositoryResolver resolver = new ArtifactRepositoryResolver();

        assertThrows(MojoFailureException.class, () -> resolver.getDeploymentRepository(dpr));
    }


}
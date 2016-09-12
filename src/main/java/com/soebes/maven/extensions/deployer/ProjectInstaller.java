package com.soebes.maven.extensions.deployer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.artifact.install.ArtifactInstaller;
import org.apache.maven.shared.artifact.install.ArtifactInstallerException;
import org.apache.maven.shared.repository.RepositoryManager;
import org.apache.maven.shared.utils.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ProjectInstaller
{
    private final Logger LOGGER = LoggerFactory.getLogger( getClass() );

    @Inject
    private ArtifactInstaller installer;

    @Inject
    private RepositoryManager repositoryManager;

    private final DualDigester digester = new DualDigester();


    public void installProject( ProjectBuildingRequest buildingRequest, ProjectInstallerRequest request,
                        ArtifactRepository artifactRepository ) throws MojoExecutionException {
        
        MavenProject project = request.getProject();
        boolean createChecksum = request.isCreateChecksum();
        boolean updateReleaseInfo = request.isUpdateReleaseInfo();

        Artifact artifact = project.getArtifact();
        String packaging = project.getPackaging();
        File pomFile = project.getFile();

        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        // TODO: push into transformation
        boolean isPomArtifact = "pom".equals( packaging );

        ProjectArtifactMetadata metadata;

        if ( updateReleaseInfo )
        {
            artifact.setRelease( true );
        }

        try
        {
            Collection<File> metadataFiles = new LinkedHashSet<File>();

            if ( isPomArtifact )
            {
                // installer.install( pomFile, artifact, localRepository );
                installer.install( buildingRequest,
                                   Collections.<Artifact>singletonList( new ProjectArtifact( project ) ) );
                installChecksums( buildingRequest, artifactRepository, artifact, createChecksum );
                addMetaDataFilesForArtifact( artifactRepository, artifact, metadataFiles, createChecksum );
            }
            else
            {
                metadata = new ProjectArtifactMetadata( artifact, pomFile );
                artifact.addMetadata( metadata );

                File file = artifact.getFile();

                // Here, we have a temporary solution to MINSTALL-3 (isDirectory() is true if it went through compile
                // but not package). We are designing in a proper solution for Maven 2.1
                if ( file != null && file.isFile() )
                {
                    installer.install( buildingRequest, Collections.<Artifact>singletonList( artifact ) );
                    installChecksums( buildingRequest, artifactRepository, artifact, createChecksum );
                    addMetaDataFilesForArtifact( artifactRepository, artifact, metadataFiles, createChecksum );
                }
                else if ( !attachedArtifacts.isEmpty() )
                {
                    throw new MojoExecutionException( "The packaging plugin for this project did not assign "
                        + "a main file to the project but it has attachments. Change packaging to 'pom'." );
                }
                else
                {
                    // CHECKSTYLE_OFF: LineLength
                    throw new MojoExecutionException( "The packaging for this project did not assign a file to the build artifact" );
                    // CHECKSTYLE_ON: LineLength
                }
            }

            for ( Artifact attached : attachedArtifacts )
            {
                // installer.install( attached.getFile(), attached, localRepository );
                installer.install( buildingRequest, Collections.singletonList( attached ) );
                installChecksums( buildingRequest, artifactRepository, attached, createChecksum );
                addMetaDataFilesForArtifact( artifactRepository, attached, metadataFiles, createChecksum );
            }

            installChecksums( metadataFiles );
        }
        catch ( ArtifactInstallerException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * Installs the checksums for the specified artifact if this has been enabled in the plugin configuration. This
     * method creates checksums for files that have already been installed to the local repo to account for on-the-fly
     * generated/updated files. For example, in Maven 2.0.4- the <code>ProjectArtifactMetadata</code> did not install
     * the original POM file (cf. MNG-2820). While the plugin currently requires Maven 2.0.6, we continue to hash the
     * installed POM for robustness with regard to future changes like re-introducing some kind of POM filtering.
     *
     * @param artifact The artifact for which to create checksums, must not be <code>null</code>.
     * @param createChecksum {@code true} if checksum should be created, otherwise {@code false}.
     * @throws MojoExecutionException If the checksums could not be installed.
     */
    private void installChecksums( ProjectBuildingRequest buildingRequest, ArtifactRepository artifactRepository, Artifact artifact, boolean createChecksum )
        throws MojoExecutionException
    {
        if ( !createChecksum )
        {
            return;
        }

        File artifactFile = getLocalRepoFile( buildingRequest, artifactRepository, artifact );
        installChecksums( artifactFile );
    }

    // CHECKSTYLE_OFF: LineLength
    protected void addMetaDataFilesForArtifact( ArtifactRepository artifactRepository, Artifact artifact, Collection<File> targetMetadataFiles, boolean createChecksum )
    // CHECKSTYLE_ON: LineLength
    {
        if ( !createChecksum )
        {
            return;
        }

        Collection<ArtifactMetadata> metadatas = artifact.getMetadataList();
        if ( metadatas != null )
        {
            for ( ArtifactMetadata metadata : metadatas )
            {
                File metadataFile = getLocalRepoFile( artifactRepository, metadata );
                targetMetadataFiles.add( metadataFile );
            }
        }
    }

    /**
     * Installs the checksums for the specified metadata files.
     *
     * @param metadataFiles The collection of metadata files to install checksums for, must not be <code>null</code>.
     * @throws MojoExecutionException If the checksums could not be installed.
     */
    protected void installChecksums( Collection<File> metadataFiles )
        throws MojoExecutionException
    {
        for ( File metadataFile : metadataFiles )
        {
            installChecksums( metadataFile );
        }
    }

    /**
     * Installs the checksums for the specified file (if it exists).
     *
     * @param installedFile The path to the already installed file in the local repo for which to generate checksums,
     *                      must not be <code>null</code>.
     * @throws MojoExecutionException If the checksums could not be installed.
     */
    private void installChecksums( File installedFile )
        throws MojoExecutionException
    {
        boolean signatureFile = installedFile.getName().endsWith( ".asc" );
        if ( installedFile.isFile() && !signatureFile )
        {

            LOGGER.debug( "Calculating checksums for " + installedFile );
            digester.calculate( installedFile );
            installChecksum( installedFile, ".md5", digester.getMd5() );
            installChecksum( installedFile, ".sha1", digester.getSha1() );
        }
    }

    /**
     * Installs a checksum for the specified file.
     *
     * @param installedFile The base path from which the path to the checksum files is derived by appending the given
     *                      file extension, must not be <code>null</code>.
     * @param ext           The file extension (including the leading dot) to use for the checksum file, must not be
     *                      <code>null</code>.
     * @param checksum      the checksum to write
     * @throws MojoExecutionException If the checksum could not be installed.
     */
    private void installChecksum( File installedFile, String ext, String checksum )
        throws MojoExecutionException
    {
        File checksumFile = new File( installedFile.getAbsolutePath() + ext );
        LOGGER.debug( "Installing checksum to " + checksumFile );
        try
        {
            //noinspection ResultOfMethodCallIgnored
            checksumFile.getParentFile().mkdirs();
            FileUtils.fileWrite( checksumFile.getAbsolutePath(), "UTF-8", checksum );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to install checksum to " + checksumFile, e );
        }
    }

    /**
     * Gets the path of the specified artifact within the local repository. Note that the returned path need not exist
     * (yet).
     *
     * @param artifact The artifact whose local repo path should be determined, must not be <code>null</code>.
     * @return The absolute path to the artifact when installed, never <code>null</code>.
     */
    private File getLocalRepoFile( ProjectBuildingRequest buildingRequest, ArtifactRepository artifactRepository, Artifact artifact )
    {
        String path = repositoryManager.getPathForLocalArtifact( buildingRequest, artifact );
        return new File( artifactRepository.getBasedir(), path );
    }

    /**
     * Gets the path of the specified artifact metadata within the local repository. Note that the returned path need
     * not exist (yet).
     *
     * @param metadata The artifact metadata whose local repo path should be determined, must not be <code>null</code>.
     * @return The absolute path to the artifact metadata when installed, never <code>null</code>.
     */
    private File getLocalRepoFile( ArtifactRepository artifactRepository, ArtifactMetadata metadata )
    {
        String path = artifactRepository.pathOfLocalRepositoryMetadata( metadata, artifactRepository );
        return new File( artifactRepository.getBasedir(), path );
    }



}

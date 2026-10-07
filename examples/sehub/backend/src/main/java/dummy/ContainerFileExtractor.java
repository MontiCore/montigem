/* (c) https://github.com/MontiCore/monticore */
package dummy;

import de.se_rwth.commons.logging.Log;
import org.apache.commons.io.FilenameUtils;
import sehub.*;
import sehub.Artifact;
import sehub.Directory;
import sehub.DirectoryBuilder;
import sehub.FileArtifact;
import sehub.FileArtifactBuilder;
import sehub.Project;
import sehub.ProjectBuilder;
import sehub.SehubManager;
import umlp.backendrte.common.GemIdentifiable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dummy.ExtractorUtils.DIRECTORY_FILE_TYPE;
import static dummy.ExtractorUtils.NAME_EXTENSION_POS;
import static dummy.ExtractorUtils.SIMPLE_NAME_POS;

public class ContainerFileExtractor extends BaseExtractor {

  private Map<Directory, List<String>> directoryToSubArtifactsMap = new HashMap<>();

  private List<Project> roots = new ArrayList<>();

  private Map<String, Artifact> absolutePathToArtifact = new HashMap<>();

  private Project defaultRoot;

  public ContainerFileExtractor() { }

  /**
   * This method does the extraction for roots, directories and fileartifacts i.e., creating their object representations and
   * furthermore connecting them with each other.
   * @param filetype2Files
   * @return
   */
  @Override
  public Set<GemIdentifiable> doExtract(Map<String, Collection<RootAwareFile>> filetype2Files) {
    Set<GemIdentifiable> runtimes = new HashSet<>();

    roots = createRoots(filetype2Files);
    runtimes.addAll(roots);

    List<Artifact> artifacts = createBaseArtifactsWithoutConnectedDirectories(filetype2Files);

    fillAbsolutePathToArtifactMap(absolutePathToArtifact, artifacts);

    runtimes.addAll(getAllFullyBuiltFileArtifacts(artifacts));
    runtimes.addAll(connectEveryDirectoryToItsSubArtifacts(filetype2Files));

    return runtimes;
  }

  private void fillAbsolutePathToArtifactMap(Map<String, Artifact> stringArtifactMap, List<Artifact> artifacts) {
    artifacts.forEach(a -> {
      if(a.getFullName().equals(a.getRoot().getFullName())) {
        stringArtifactMap.put(a.getFullName(), a);
      } else {
        stringArtifactMap.put(a.getRoot().getFullName()+"/"+a.getFullName(), a);
      }
    });
  }

  private List<FileArtifact> getAllFullyBuiltFileArtifacts(List<Artifact> artifacts) {
    return artifacts.stream().filter(a -> a instanceof FileArtifact).map(fa -> (FileArtifact)fa).collect(Collectors.toList());
  }

  private List<Directory> connectEveryDirectoryToItsSubArtifacts(Map<String, Collection<RootAwareFile>> filetype2Files) {
     if(!filetype2Files.containsKey(DIRECTORY_FILE_TYPE)) {
      return new ArrayList<>();
    }
    Collection<RootAwareFile> directorySystemRepresentations = filetype2Files.get(DIRECTORY_FILE_TYPE);

    connectDirectoryObjectToSubSystemFiles(directorySystemRepresentations);
    return SehubManager.getDirectoryList();
  }

  private void connectDirectoryObjectToSubSystemFiles(Collection<RootAwareFile> directoriesSystemRepresentation) {

    directoriesSystemRepresentation.forEach(dir ->
        getDirectoryOfFile(dir).addAllContainss(
            Arrays.stream(Optional.ofNullable(dir.listFiles()).orElse(new File[0]))
                .map(f -> absolutePathToArtifact.get(ExtractorUtils.transformWindowsPathToLinuxStyle(f.getAbsolutePath()))).collect(Collectors.toList())));
  }

  private Directory getDirectoryOfFile(File f) {
    return (Directory) absolutePathToArtifact.get(ExtractorUtils.transformWindowsPathToLinuxStyle(f.getAbsolutePath()));
  }
  private Directory connectDirectoryToSubArtifactObjects(Map.Entry<Directory, List<File>> directoryToSubSystemArtifacts) {
    Directory directory = directoryToSubSystemArtifacts.getKey();

    directoryToSubSystemArtifacts.getValue().forEach(f ->
        directory.addContains(absolutePathToArtifact.get(ExtractorUtils.transformWindowsPathToLinuxStyle(f.getAbsolutePath()))));
    return directory;
  }

  /**
   * Method creates all directories and fileartifact objects and links from directories to files are remembered in subartifacts
   * @param filetype2Files
   * @return
   */
  private List<Artifact> createBaseArtifactsWithoutConnectedDirectories(Map<String, Collection<RootAwareFile>> filetype2Files) {
    ArrayList<Artifact> artifacts = new ArrayList<>();

    for(String fileType : filetype2Files.keySet()){
      Collection<RootAwareFile> files = filetype2Files.get(fileType);
      if (fileType.equals(DIRECTORY_FILE_TYPE)) {
        artifacts.addAll(createDirectories(files));
      }
      else {
        artifacts.addAll(createFileArtifacts(files));
      }
    }
    return artifacts;
  }

  private List<FileArtifact> createFileArtifacts(Collection<RootAwareFile> files) {
    ArrayList<FileArtifact> fileArtifacts = new ArrayList<>();

    for(RootAwareFile file : files) {
      fileArtifacts.add(createFullyConnnectedFileArtifact(file));
    }
    return fileArtifacts;
  }

  private FileArtifact createFullyConnnectedFileArtifact(RootAwareFile file) {
    FileArtifactBuilder fileArtifactBuilderObject = createFileArtifactObject(file,
        ExtractorUtils.removeRootPathFromAbsolutePath(file.getAbsolutePath(), getRootOfPath(file.getAbsolutePath())));
    linkArtifactWithItsRoot(file, fileArtifactBuilderObject);
    return fileArtifactBuilderObject.build().orElse(null);
  }

  private List<Project> createRoots(Map<String, Collection<RootAwareFile>> filetype2Files) {
    ArrayList<Project> roots = new ArrayList<>();

    // Every kind of File could be a Root
    for(String fileType : filetype2Files.keySet()){
      for(RootAwareFile f : filetype2Files.get(fileType)) {
        if (f.isRoot()) {
          roots.add(createRoot(f));
        }
      }
    }
    return roots;
  }

  private Project createRoot(RootAwareFile f) {
    ProjectBuilder rootBuilder = SehubManager.projectBuilder();

    Project rootArtifact =  rootBuilder
        .fullName(ExtractorUtils.normalizePath(ExtractorUtils.transformWindowsPathToLinuxStyle(f.getAbsolutePath())))
        .name(FilenameUtils.getBaseName(f.getAbsolutePath()))
        .git("")
        .build().orElse(null);
    if(rootArtifact == null) {
      Log.error("Building root failed.");
    }

    return rootArtifact;
  }

  /**
   * Create directories and store possible contains relations for later to resolve them when all
   * Directories are present
   * @param dirs all Directory "files"
   */
  private List<Directory> createDirectories(Collection<RootAwareFile> dirs){
    List<Directory> directories = new ArrayList<>();

    for(RootAwareFile dir : dirs){
      directories.add(createDirectoryAndLinkWithRoot(dir));
    }
    return directories;
  }

  private Directory createDirectoryAndLinkWithRoot(RootAwareFile dir) {
    DirectoryBuilder directoryBuilderObject =
        createDirectoryObject(dir, ExtractorUtils.removeRootPathFromAbsolutePath(dir.getAbsolutePath(), getRootOfPath(dir.getAbsolutePath())));
    linkArtifactWithItsRoot(dir, directoryBuilderObject);
    return directoryBuilderObject.build().orElse(null);
  }

  private void linkArtifactWithItsRoot(RootAwareFile systemRepresentation, Artifact artifactRepresentation) {
    Project root = getRootOfPath(systemRepresentation.getAbsolutePath());

    if(artifactRepresentation instanceof DirectoryBuilder) {
      ((DirectoryBuilder) artifactRepresentation).root(root);
    } if(artifactRepresentation instanceof FileArtifactBuilder) {
      ((FileArtifactBuilder) artifactRepresentation).root(root);
    } else {
      artifactRepresentation.setRoot(root);
    }

    if (systemRepresentation.isRoot()) { // if current dir is root we set the Directory Object as artifact representation
      root.addRootArtifact((Directory) artifactRepresentation); // TODO because of FrontEnd we have to use a directory for now
    }
  }

  private FileArtifactBuilder createFileArtifactObject(RootAwareFile file, String nameStartingFromRoot) {
    FileArtifactBuilder fileArtifactBuilder = SehubManager.fileArtifactBuilder();

    String[] simpleNameAndExtension = ExtractorUtils.splitSimpleNameAndExtension(file.getName());

    return fileArtifactBuilder.fullName(nameStartingFromRoot)
        .name(simpleNameAndExtension[SIMPLE_NAME_POS]+simpleNameAndExtension[NAME_EXTENSION_POS])
        .nameExtension(simpleNameAndExtension[NAME_EXTENSION_POS])
        .simpleName(simpleNameAndExtension[SIMPLE_NAME_POS])
        .modified(ExtractorUtils.toModified(file.lastModified()));
  }

  private Project getRootOfPath(String absolutePathOfArtifact) {
    Optional<Project> rootOpt = roots.stream().filter(r ->
        ExtractorUtils.transformWindowsPathToLinuxStyle(absolutePathOfArtifact).startsWith(r.getFullName())).findFirst();
    if(rootOpt.isEmpty()) {
      Log.error("No root object with path "+ExtractorUtils.transformWindowsPathToLinuxStyle(absolutePathOfArtifact)
          + " was created.");
      return getDefaultRoot();
    } else {
      return rootOpt.get();
    }
  }

  private List<String> getFilesContainedByDirectoryAsStringPath(RootAwareFile dir) {
    return Arrays.stream(dir.listFiles())
        .map(f -> ExtractorUtils.transformWindowsPathToLinuxStyle(f.getAbsolutePath()))
        .collect(Collectors.toList());
  }

  private DirectoryBuilder createDirectoryObject(RootAwareFile dir, String nameStartingFromRoot) {
    DirectoryBuilder directoryBuilder = SehubManager.directoryBuilder();

    return directoryBuilder.simpleName(dir.getName())
        .nameExtension("Folder")
        .modified(ExtractorUtils.toModified(dir.lastModified()))
        .name(dir.getName())
        .fullName(nameStartingFromRoot);
  }

  private Project getDefaultRoot() {
    if(defaultRoot == null) {
      ProjectBuilder rootBuilder = SehubManager.projectBuilder();
      defaultRoot = rootBuilder.git("").fullName("DEFAULT_ERROR_ROOT").name("").build().orElse(null);
    }
    return this.defaultRoot;
  }
}

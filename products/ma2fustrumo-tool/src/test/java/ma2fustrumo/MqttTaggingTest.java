package ma2fustrumo;

import de.monticore.tagging.TagRepository;
import de.monticore.tagging.tags.TagsMill;
import ma2fustrumo.pojo.SimpleFunction;
import ma2fustrumo.pojo.SimplePort;
import ma2fustrumo.pojo.SimpleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttTaggingTest {

  @TempDir
  Path modelPath;

  @Test
  void loadsMqttMetadataFromTagsOnInstancesPortsAndVariables() throws IOException {
    writeModel("Root", """
        package mqtttest;

        component Root {
          Source source;
          Sink sink;
          source.value -> sink.value;
        }
        """);
    writeModel("Source", """
        package mqtttest;

        component Source {
          port out int value;
          int sampleCount = 0;
        }
        """);
    writeModel("Sink", """
        package mqtttest;

        component Sink {
          port in int value;
        }
        """);
    Files.writeString(modelPath.resolve("Mqtt.tags"), """
        tags Mqtt {
          tag mqtttest.Root.source with mqttNamespace = "factory/source/";
          tag mqtttest.Source.value with mqtt = "temperature";
          tag mqtttest.Source.value
            with mqttConverter = "mqtttest.MqttConverters.toInteger";
          tag mqtttest.Source.sampleCount with mqtt = "sample-count";
          tag mqtttest.Source.sampleCount
            with mqttConverter = "mqtttest.MqttConverters.toInteger";
          tag mqtttest.Sink with mqttNamespace = "factory/sink/";
        }
        """);
    Files.writeString(modelPath.resolve("Unrelated.tags"), """
        tags Unrelated {
          tag mqtttest.Root with unrelated;
        }
        """);

    SimpleFunction root = transform("mqtttest.Root");
    SimpleFunction source = root.getSubfunctions().get("source");
    SimpleFunction sink = root.getSubfunctions().get("sink");
    SimplePort value = source.getOutPorts().get(0);
    SimpleState sampleCount = source.getStates().get(0);

    assertEquals("factory/source/", source.getMetadata("mqttNamespace"));
    assertEquals("factory/sink/", sink.getMetadata("mqttNamespace"));
    assertEquals("temperature", value.getMetadata("mqtt"));
    assertEquals("mqtttest.MqttConverters.toInteger", value.getMetadata("mqttConverter"));
    assertEquals("sample-count", sampleCount.getMetadata("mqtt"));
    assertEquals("mqtttest.MqttConverters.toInteger", sampleCount.getMetadata("mqttConverter"));
    assertFalse(TagRepository.getLoadedTagUnits().iterator().hasNext());
  }

  @Test
  void stereotypesRemainSupportedAndOverrideTags() throws IOException {
    writeModel("Root", """
        package mqtttest;

        component Root {
          <<mqttNamespace="stereotype/">>
          Source source;
          Sink sink;
          source.value -> sink.value;
        }
        """);
    writeModel("Source", """
        package mqtttest;

        component Source {
          port <<mqtt="stereotypeTopic",
                 mqttConverter="mqtttest.StereotypeConverters.toInteger">>
               out int value;
        }
        """);
    writeModel("Sink", """
        package mqtttest;

        component Sink {
          port in int value;
        }
        """);
    Files.writeString(modelPath.resolve("Mqtt.tags"), """
        tags Mqtt {
          tag mqtttest.Root.source with mqttNamespace = "tag/";
          tag mqtttest.Source.value with mqtt = "tagTopic";
          tag mqtttest.Source.value
            with mqttConverter = "mqtttest.TagConverters.toInteger";
        }
        """);

    SimpleFunction root = transform("mqtttest.Root");
    SimpleFunction source = root.getSubfunctions().get("source");
    SimplePort value = source.getOutPorts().get(0);

    assertTrue(source.hasMetadata("mqttNamespace"));
    assertEquals("stereotype/", source.getMetadata("mqttNamespace"));
    assertEquals("stereotypeTopic", value.getMetadata("mqtt"));
    assertEquals("mqtttest.StereotypeConverters.toInteger", value.getMetadata("mqttConverter"));
  }

  @Test
  void loadsTagModelsFromSymbolArchives() throws IOException {
    Path archive = modelPath.resolve("mqtt-symbols.jar");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      output.putNextEntry(new ZipEntry("mqtt/Archived.tags"));
      output.write("""
                       tags Archived {
                         tag mqtttest.Source.value with mqtt = "archivedTopic";
                       }
                       """.getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
    }

    TagsMill.reset();
    TagsMill.init();
    TagLoader.load(new String[]{archive.toString()});
    try {
      assertTrue(TagRepository.getLoadedTagUnits().iterator().hasNext());
    } finally {
      TagLoader.clear();
    }
    assertFalse(TagRepository.getLoadedTagUnits().iterator().hasNext());
  }

  protected void writeModel(String name, String content) throws IOException {
    Path packagePath = modelPath.resolve("mqtttest");
    Files.createDirectories(packagePath);
    Files.writeString(packagePath.resolve(name + ".arc"), content);
  }

  protected SimpleFunction transform(String mainComponent) {
    MA2FuStruMoTool tool = new MA2FuStruMoTool();
    tool.init();
    tool.run(new String[]{
        "-i", modelPath.toString(), "-main", mainComponent, "-c2mc"
    });
    return tool.getResult();
  }
}

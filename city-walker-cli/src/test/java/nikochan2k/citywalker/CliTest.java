package nikochan2k.citywalker;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class CliTest {

	@Test
	void test1() {
		Cli cli = new Cli();
		CommandLine cmd = new CommandLine(cli);
		URL url = CliTest.class.getResource("53392633_bldg_6697_op2.gml");
		String path = url.getFile();
		File file = new File(path);
		File json = new File(file.getParent(), "53392633_bldg_6697_op2.json");
		json.delete();
		int result = cmd.execute("-t=geojson", path);
		assertEquals(result, 0);
		assertEquals(true, json.exists());
	}

	@Test
	void test2() {
		Cli cli = new Cli();
		CommandLine cmd = new CommandLine(cli);
		URL url = CliTest.class.getResource("53392633_bldg_6697_op2.gml");
		String path = url.getFile();
		File file = new File(path);
		File parent = file.getParentFile();
		String glob = parent.getAbsolutePath() + cli.sep + "*.gml";
		File json = new File(parent, "53392633_bldg_6697_op2.json");
		json.delete();
		int result = cmd.execute("-t=geojson", glob);
		assertEquals(result, 0);
		assertEquals(true, json.exists());
	}

	@Test
	void test3() {
		Cli cli = new Cli();
		CommandLine cmd = new CommandLine(cli);
		URL url = CliTest.class.getResource("53392633_bldg_6697_op2.gml");
		String path = url.getFile();
		File file = new File(path);
		File parent = file.getParentFile();
		File gp = parent.getParentFile();
		String glob = gp.getAbsolutePath() + cli.sep + "**" + cli.sep + "*.gml";
		File json = new File(parent, "53392633_bldg_6697_op2.json");
		json.delete();
		int result = cmd.execute("-t=geojson", glob);
		assertEquals(result, 0);
		assertEquals(true, json.exists());
	}

	@Test
	void test4() {
		Cli cli = new Cli();
		CommandLine cmd = new CommandLine(cli);
		URL url = CliTest.class.getResource("53392633_bldg_6697_op2.gml");
		String path = url.getFile();
		File file = new File(path);
		File json = new File(file.getParent(), "53392633_bldg_6697_op2.json");
		json.delete();
		int result = cmd.execute("-t=geojson", "-f", "-n", path);
		assertEquals(result, 0);
		assertEquals(true, json.exists());
	}

	@Test
	void test5() throws IOException {
		Cli cli = new Cli();
		CommandLine cmd = new CommandLine(cli);
		URL url = CliTest.class.getResource("53392633_bldg_6697_op2.gml");
		String path = url.getFile();
		File tempFile = File.createTempFile("test", ".tmp");
		File outputDir = tempFile.getParentFile();
		tempFile.delete();
		File json = new File(outputDir, "53392633_bldg_6697_op2.json");
		json.delete();
		int result = cmd.execute("-t=geojson", "-o=" + outputDir.getAbsolutePath(), path);
		assertEquals(result, 0);
		assertEquals(true, json.exists());
	}

}

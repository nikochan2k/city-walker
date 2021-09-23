package nikochan2k.citywalker;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.reflections.Reflections;

import com.esotericsoftware.wildcard.Paths;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "city-walker", mixinStandardHelpOptions = true, version = "0.1.0", description = "Convert CityGML to various formats")
public class Cli implements Callable<Integer> {

	private static final Logger LOGGER = Logger.getLogger(Cli.class.getName());

	private final FileSystem fs;
	final String sep;
	private Set<Factory> factories;

	@Option(names = { "-f", "--flipXY" }, description = "flip X and Y coordinate")
	boolean flipXY;

	@Parameters(paramLabel = "FILE", description = "Glob pattern of file path.")
	private String[] globs;

	@Option(names = { "-s", "--src" }, description = "Source SRS (Default: Try to detect, or EPSG:4326")
	String inputSRS;

	@Option(names = { "-n", "--no-attr" }, description = "No attribute except for measuredHeight")
	boolean noAttributes;

	@Option(names = { "-o", "--output" }, description = "Output directory (Default: the same directory with input file")
	File outputDir;

	@Option(names = { "-d", "--dst" }, description = "Destination SRS (Default: EPSG:4326)")
	String outputSRS;

	@Option(names = { "-t", "--type" }, description = "Output format type", required = true)
	String type;

	public Cli() {
		fs = FileSystems.getDefault();
		sep = fs.getSeparator();
	}

	@Override
	public Integer call() throws Exception {
		Factory factory = null;
		for (Factory f : getFactories()) {
			if (f.getTypeName().equalsIgnoreCase(type.trim())) {
				factory = f;
				break;
			}
		}
		if (factory == null) {
			System.err.println("Output type not found: " + type.trim());
			return 1;
		}

		Parser parser = new Parser(factory);
		Paths paths = new Paths();
		for (String glob : globs) {
			if (glob.contains("*")) {
				if (glob.contains("/") && "\\".equals(sep)) {
					glob = glob.replace('/', '\\');
				} else if (glob.contains("\\") && "/".equals(sep)) {
					glob = glob.replace('\\', '/');
				}
				int index = glob.indexOf("*") - 1;
				File dir = new File("." + sep);
				String pattern = "";
				for (int i = index; 0 <= i; i--) {
					if (sep.equals("" + glob.charAt(i))) {
						String dirPath = glob.substring(0, i);
						dir = new File(dirPath);
						pattern = glob.substring(i + 1);
						break;
					}
				}
				if (!dir.exists() || !dir.isDirectory()) {
					LOGGER.warning("Not found: " + dir.getAbsolutePath());
					continue;
				}
				Paths result = paths.glob(dir.getAbsolutePath(), pattern);
				for (Iterator<File> ite = result.fileIterator(); ite.hasNext();) {
					File file = ite.next();
					parser.parse(file);
				}
			} else {
				File file = new File(glob);
				if (!file.exists() || !file.isFile()) {
					LOGGER.warning("Not found: " + file.getAbsolutePath());
					continue;
				}
				parser.parse(file);
			}
		}

		return 0;
	}

	private Set<? extends Factory> getFactories() {
		if (factories != null) {
			return factories;
		}

		Reflections reflections = new Reflections(Thread.currentThread().getContextClassLoader());
		Set<Class<? extends Factory>> factoryClasses = reflections.getSubTypesOf(Factory.class);
		factories = new HashSet<Factory>(factoryClasses.size());
		for (Class<? extends Factory> fc : factoryClasses) {
			try {
				Factory factory = fc.newInstance();
				factory.setOutputDir(outputDir);
				factory.setNoAttributes(noAttributes);
				factory.setInputSRS(inputSRS);
				factory.setOutputSRS(outputSRS);
				factory.setFlipXY(flipXY);
				factories.add(factory);
			} catch (InstantiationException | IllegalAccessException | SecurityException | IllegalArgumentException e) {
				LOGGER.warning(e.toString());
			}
		}
		return factories;
	}

	public static void main(String... args) {
		int exitCode = new CommandLine(new Cli()).execute(args);
		System.exit(exitCode);
	}
}

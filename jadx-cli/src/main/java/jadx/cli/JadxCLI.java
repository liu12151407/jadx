package jadx.cli;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.impl.AnnotatedCodeWriter;
import jadx.api.impl.NoOpCodeCache;
import jadx.api.impl.SimpleCodeWriter;
import jadx.api.security.JadxSecurityFlag;
import jadx.api.security.impl.JadxSecurity;
import jadx.cli.LogHelper.LogLevelEnum;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.commons.app.JadxCommonEnv;
import jadx.core.utils.exceptions.JadxArgsValidateException;
import jadx.plugins.tools.JadxExternalPluginsLoader;

public class JadxCLI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxCLI.class);

	public static void main(String[] args) {
		int result = 1;
		try {
			result = execute(args);
		} finally {
			System.exit(result);
		}
	}

	public static int execute(String[] args) {
		try {
			JadxCLIArgs jadxArgs = new JadxCLIArgs();
			if (jadxArgs.processArgs(args)) {
				return processAndSave(jadxArgs);
			}
			return 0;
		} catch (JadxArgsValidateException e) {
			LOG.error("Incorrect arguments: {}", e.getMessage());
			return 1;
		} catch (Throwable e) {
			LOG.error("Process error:", e);
			return 1;
		}
	}

	private static int processAndSave(JadxCLIArgs cliArgs) {
		LogHelper.initLogLevel(cliArgs);
		LogHelper.setLogLevelsForLoadingStage();
		JadxArgs jadxArgs = cliArgs.toJadxArgs();
		jadxArgs.setCodeCache(new NoOpCodeCache());
		jadxArgs.setPluginLoader(new JadxExternalPluginsLoader());
		jadxArgs.setFilesGetter(JadxFilesGetter.INSTANCE);
		initCodeWriterProvider(jadxArgs);
		applyEnvVars(jadxArgs);
		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
			jadx.load();
			if (checkForErrors(jadx)) {
				return 1;
			}
			LogHelper.setLogLevelsForDecompileStage();
			if (!SingleClassMode.process(jadx, cliArgs)) {
				save(jadx);
			}
			int errorsCount = jadx.getErrorsCount();
			if (errorsCount != 0) {
				jadx.printErrorsReport();
				LOG.error("finished with errors, count: {}", errorsCount);
				return 1;
			}
			LOG.info("done");
			return 0;
		}
	}

	private static void initCodeWriterProvider(JadxArgs jadxArgs) {
		switch (jadxArgs.getOutputFormat()) {
			case JAVA:
				jadxArgs.setCodeWriterProvider(SimpleCodeWriter::new);
				break;
			case JSON:
				// needed for code offsets and source lines
				jadxArgs.setCodeWriterProvider(AnnotatedCodeWriter::new);
				break;
		}
	}

	private static void applyEnvVars(JadxArgs jadxArgs) {
		Set<JadxSecurityFlag> flags = JadxSecurityFlag.all();
		boolean modified = false;
		boolean disableXmlSecurity = JadxCommonEnv.getBool("JADX_DISABLE_XML_SECURITY", false);
		if (disableXmlSecurity) {
			flags.remove(JadxSecurityFlag.SECURE_XML_PARSER);
			// TODO: not related to 'xml security', but kept for compatibility
			flags.remove(JadxSecurityFlag.VERIFY_APP_PACKAGE);
			modified = true;
		}
		// TODO: migrate 'ZipSecurity'
		if (modified) {
			jadxArgs.setSecurity(new JadxSecurity(flags));
		}
	}

	private static boolean checkForErrors(JadxDecompiler jadx) {
		if (jadx.getRoot().getClasses().isEmpty()) {
			if (jadx.getArgs().isSkipResources()) {
				LOG.error("Load failed! No classes for decompile!");
				return true;
			}
			if (!jadx.getArgs().isSkipSources()) {
				LOG.warn("No classes to decompile; decoding resources only");
				jadx.getArgs().setSkipSources(true);
			}
		}
		if (jadx.getErrorsCount() > 0) {
			LOG.error("Load with errors! Check log for details");
			// continue processing
			return false;
		}
		return false;
	}

	private static void save(JadxDecompiler jadx) {
		if (LogHelper.getLogLevel() == LogLevelEnum.QUIET) {
			jadx.save();
		} else {
			LOG.info("processing ...");
			jadx.save(500, (done, total) -> {
				int progress = (int) (done * 100.0 / total);
				System.out.printf("INFO  - progress: %d of %d (%d%%)\r", done, total, progress);
			});
			// dumb line clear :)
			System.out.print("                                                             \r");
		}
	}
}

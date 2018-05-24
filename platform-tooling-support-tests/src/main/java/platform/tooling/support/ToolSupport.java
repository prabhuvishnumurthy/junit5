/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

public class ToolSupport {

	private final Tool tool;
	private final String version;

	private final Path toolPath = Paths.get("build", "test-tools");
	private final Path workPath = Paths.get("build", "test-workspace");

	public ToolSupport(Tool tool, String version) {
		this.tool = tool;
		this.version = version;
	}

	/** Get build tool. */
	public Path init() throws Exception {
		// trivial case: use "gradlew" from "junit5" main project
		if (tool.equals(Tool.GRADLE) && version.isEmpty()) {
			var executable = "gradlew";
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				executable += ".bat";
			}
			return Paths.get("..", executable).normalize().toAbsolutePath();
		}

		// download
		var toolArchive = tool.createArchive(version);
		var toolUri = tool.createUri(version);
		var toolArchivePath = toolPath.resolve(toolArchive);
		if (Files.notExists(toolArchivePath)) {
			FileUtils.copyURLToFile(toolUri.toURL(), toolArchivePath.toFile(), 2000, 2000);
		}

		// extract
		var jarTool = ToolProvider.findFirst("jar").orElseThrow();
		var stringWriter = new StringWriter();
		var printWriter = new PrintWriter(stringWriter);
		jarTool.run(printWriter, printWriter, "--list", "--file", toolArchivePath.toString());
		var toolFolderName = stringWriter.toString().split("\\R")[0];
		var toolFolderPath = toolPath.resolve(toolFolderName);
		if (Files.notExists(toolFolderPath)) {
			jarTool.run(System.out, System.err, "--extract", "--file", toolArchivePath.toString());
			FileUtils.moveDirectoryToDirectory(Paths.get(toolFolderName).toFile(), toolPath.toFile(), true);
		}
		return toolFolderPath.resolve(tool.createExecutable()).toAbsolutePath();
	}

	public ToolReport run(String projectName, Path executable, Object... args) throws Exception {
		var report = new ToolReport();

		// unroll to clean "build/test-workspace/${name}" workspace directory
		var project = Paths.get("projects", projectName);
		var workspace = workPath.resolve(projectName);
		FileUtils.deleteQuietly(workspace.toFile());
		FileUtils.copyDirectory(project.toFile(), workspace.toFile());

		// execute build and collect data
		report.stdout = workspace.resolve("stdout.txt");
		report.stderr = workspace.resolve("stderr.txt");

		var command = new ArrayList<String>();
		command.add(executable.toString());
		Stream.of(args).forEach(arg -> command.add(arg.toString()));
		var process = new ProcessBuilder(command) //
				.directory(workspace.toFile()) //
				.redirectOutput(report.stdout.toFile()) //
				.redirectError(report.stderr.toFile()) //
				.start();
		report.status = process.waitFor();
		return report;
	}
}

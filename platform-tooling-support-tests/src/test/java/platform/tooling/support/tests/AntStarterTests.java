/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import platform.tooling.support.Tool;
import platform.tooling.support.ToolSupport;

@DisplayName(AntStarterTests.PROJECT)
class AntStarterTests {

	static final String PROJECT = "ant-starter";

	@Test
	@EnabledIfSystemProperty(named = "platform.tooling.support.tests.enabled", matches = "true")
	@DisplayName("ant-1.10.3")
	void ant_1_10_3() throws Exception {
		var ant = new ToolSupport(Tool.ANT, "1.10.3");
		var executable = ant.init();

		// patch ant installation with "junit-platform-console-standalone.jar"
		var standalone = Paths.get("..", "junit-platform-console-standalone", "build",
			"libs").normalize().toAbsolutePath();

		var report = ant.run(PROJECT, executable, "-verbose", "-lib", standalone);

		// assert
		assertEquals(0, report.getStatus());
		assertLinesMatch(List.of(">> HEAD >>", //
			"test.junit.launcher:", //
			">>>>", //
			"     \\[echo\\] Test run finished after [\\d]+ ms", //
			">>>>", //
			"test.console.launcher:", //
			">>>>", //
			"     \\[java\\] Test run finished after [\\d]+ ms", //
			">> TAIL >>"), Files.readAllLines(report.getStdout()));
		assertEquals(0, Files.size(report.getStderr()), "stderr is not empty");
	}
}

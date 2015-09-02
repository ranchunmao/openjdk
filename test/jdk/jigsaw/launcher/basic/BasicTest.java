/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @library ../../lib /lib/testlibrary
 * @modules jdk.jartool/sun.tools.jar
 * @build BasicTest CompilerUtils jdk.testlibrary.ProcessTools
 * @run testng BasicTest
 * @summary Basic test of starting an application as a module
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jdk.testlibrary.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


@Test
public class BasicTest {

    private static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE = "test";

    // the module main class
    private static final String MAIN_CLASS = "jdk.test.Main";


    @BeforeTest
    public void compileTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve(TEST_MODULE),
                                    MODS_DIR.resolve(TEST_MODULE));

        assertTrue(compiled, "test module did not compile");
    }


    /**
     * The initial module is loaded from an exploded module
     */
    public void testRunFromExplodedModule() throws Exception {
        String modulepath = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // java -mp mods -m $TESTMODULE/$MAINCLASS
        int exitValue
            = executeTestJava("-mp", modulepath,
                              "-m", mid)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    /**
     * The initial module is loaded from a modular JAR file
     */
    public void testRunFromJar() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mlib");

        // jar --create ...
        String classes = MODS_DIR.resolve(TEST_MODULE).toString();
        String jar = dir.resolve("m.jar").toString();
        String[] args = {
            "--create",
            "--archive=" + jar,
            "--main-class=" + MAIN_CLASS,
            "-C", classes, "."
        };
        boolean success
            = new sun.tools.jar.Main(System.out, System.out, "jar")
                .run(args);
        assertTrue(success);

        // java -mp mods -m $TESTMODULE
        int exitValue
            = executeTestJava("-mp", dir.toString(),
                              "-m", TEST_MODULE)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);

    }

    /**
     * Attempt to run an unknown module
     */
    public void testRunWithUnknownModule() throws Exception {
        String modulepath = MODS_DIR.toString();

        // java -mp mods -m $TESTMODULE
        int exitValue
            = executeTestJava("-mp", modulepath,
                              "-m", "rhubarb")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue != 0);
    }

}

/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.rules.coercer;

import static org.junit.Assert.assertThat;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.model.BuildTargetPattern;
import com.facebook.buck.parser.BuildTargetPatternParser;
import com.facebook.buck.rules.CellPathResolver;
import com.facebook.buck.rules.FakeCellPathResolver;
import com.facebook.buck.rules.macros.Macro;
import com.facebook.buck.rules.macros.StringWithMacrosUtils;
import com.facebook.buck.testutil.FakeProjectFilesystem;
import com.facebook.buck.versions.TargetNodeTranslator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class StringWithMacrosTypeCoercerTest {

  private static final ProjectFilesystem FILESYSTEM = new FakeProjectFilesystem();
  private static final CellPathResolver CELL_PATH_RESOLVER = new FakeCellPathResolver(FILESYSTEM);
  private static final Path BASE_PATH = Paths.get("");

  @Test
  public void plainString() throws CoerceFailedException {
    StringWithMacrosTypeCoercer coercer =
        StringWithMacrosTypeCoercer.from(ImmutableMap.of(), ImmutableList.of());
    assertThat(
        coercer.coerce(CELL_PATH_RESOLVER, FILESYSTEM, BASE_PATH, "test string"),
        Matchers.equalTo(StringWithMacrosUtils.format("test string")));
  }

  @Test
  public void embeddedMacro() throws CoerceFailedException {
    StringWithMacrosTypeCoercer coercer =
        StringWithMacrosTypeCoercer.from(
            ImmutableMap.of("test", TestMacro.class),
            ImmutableList.of(new TestMacroTypeCoercer()));
    assertThat(
        coercer.coerce(CELL_PATH_RESOLVER, FILESYSTEM, BASE_PATH, "string with $(test arg) macro"),
        Matchers.equalTo(
            StringWithMacrosUtils.format(
                "string with %s macro",
                new TestMacro(ImmutableList.of("arg")))));
    assertThat(
        coercer.coerce(CELL_PATH_RESOLVER, FILESYSTEM, BASE_PATH, "string with $(test arg)"),
        Matchers.equalTo(
            StringWithMacrosUtils.format(
                "string with %s",
                new TestMacro(ImmutableList.of("arg")))));
    assertThat(
        coercer.coerce(CELL_PATH_RESOLVER, FILESYSTEM, BASE_PATH, "$(test arg) macro"),
        Matchers.equalTo(
            StringWithMacrosUtils.format(
                "%s macro",
                new TestMacro(ImmutableList.of("arg")))));
    assertThat(
        coercer.coerce(CELL_PATH_RESOLVER, FILESYSTEM, BASE_PATH, "$(test arg)"),
        Matchers.equalTo(
            StringWithMacrosUtils.format(
                "%s",
                new TestMacro(ImmutableList.of("arg")))));
  }

  @Test
  public void multipleMacros() throws CoerceFailedException {
    StringWithMacrosTypeCoercer coercer =
        StringWithMacrosTypeCoercer.from(
            ImmutableMap.of(
                "test1", TestMacro.class,
                "test2", TestMacro.class),
            ImmutableList.of(new TestMacroTypeCoercer()));
    assertThat(
        coercer.coerce(
            CELL_PATH_RESOLVER,
            FILESYSTEM,
            BASE_PATH,
            "first $(test1 arg1) second $(test2 arg2)"),
        Matchers.equalTo(
            StringWithMacrosUtils.format(
                "first %s second %s",
                new TestMacro(ImmutableList.of("arg1")),
                new TestMacro(ImmutableList.of("arg2")))));
  }

  private static class TestMacro implements Macro {

    private final ImmutableList<String> args;

    TestMacro(ImmutableList<String> args) {
      this.args = args;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TestMacro testMacro = (TestMacro) o;

      return args.equals(testMacro.args);
    }

    @Override
    public int hashCode() {
      return args.hashCode();
    }

    @Override
    public Optional<Macro> translateTargets(
        CellPathResolver cellPathResolver,
        BuildTargetPatternParser<BuildTargetPattern> pattern,
        TargetNodeTranslator translator) {
      return Optional.empty();
    }

  }

  private static class TestMacroTypeCoercer implements MacroTypeCoercer<TestMacro> {

    @Override
    public boolean hasElementClass(Class<?>[] types) {
      return false;
    }

    @Override
    public Class<TestMacro> getOutputClass() {
      return TestMacro.class;
    }

    @Override
    public void traverse(TestMacro macro, TypeCoercer.Traversal traversal) {
    }

    @Override
    public TestMacro coerce(
        CellPathResolver cellRoots,
        ProjectFilesystem filesystem,
        Path pathRelativeToProjectRoot,
        ImmutableList<String> args)
        throws CoerceFailedException {
      return new TestMacro(args);
    }

  }

}

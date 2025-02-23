/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.cli;

import com.google.common.collect.ImmutableList;
import io.trino.client.ClientTypeSignature;
import io.trino.client.Column;
import org.testng.annotations.Test;

import java.io.StringWriter;
import java.util.List;

import static io.trino.client.ClientStandardTypes.BIGINT;
import static io.trino.client.ClientStandardTypes.VARBINARY;
import static io.trino.client.ClientStandardTypes.VARCHAR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

public class TestMarkdownTablePrinter
{
    @Test
    public void testMarkdownPrinting()
            throws Exception
    {
        List<Column> columns = ImmutableList.<Column>builder()
                .add(column("first", VARCHAR))
                .add(column("last", VARCHAR))
                .add(column("quantity", BIGINT))
                .build();
        StringWriter writer = new StringWriter();
        OutputPrinter printer = new MarkdownTablePrinter(columns, writer);

        printer.printRows(rows(
                        row("hello", "world", 123),
                        row("a", null, 4.5),
                        row("b", null, null),
                        row("some long\ntext that\ndoes not\nfit on\none line", "more\ntext", 4567),
                        row("bye | not **& <a>**", "done", -15)),
                true);
        printer.finish();

        String expected = "" +
                "| first                                                    | last         | quantity |\n" +
                "| -------------------------------------------------------- | ------------ | --------:|\n" +
                "| hello                                                    | world        |      123 |\n" +
                "| a                                                        | NULL         |      4.5 |\n" +
                "| b                                                        | NULL         |     NULL |\n" +
                "| some long<br>text that<br>does not<br>fit on<br>one line | more<br>text |     4567 |\n" +
                "| bye \\| not \\*\\*& \\<a\\>\\*\\*                               | done         |      -15 |\n";

        assertEquals(writer.getBuffer().toString(), expected);
    }

    @Test
    public void testMarkdownPrintingOneRow()
            throws Exception
    {
        List<Column> columns = ImmutableList.<Column>builder()
                .add(column("first", VARCHAR))
                .add(column("last", VARCHAR))
                .build();
        StringWriter writer = new StringWriter();
        OutputPrinter printer = new MarkdownTablePrinter(columns, writer);

        printer.printRows(rows(row("a long line\nwithout wrapping", "text")), true);
        printer.finish();

        String expected = "" +
                "| first                           | last |\n" +
                "| ------------------------------- | ---- |\n" +
                "| a long line<br>without wrapping | text |\n";

        assertEquals(writer.getBuffer().toString(), expected);
    }

    @Test
    public void testMarkdownPrintingNoRows()
            throws Exception
    {
        List<Column> columns = ImmutableList.<Column>builder()
                .add(column("first", VARCHAR))
                .add(column("last", VARCHAR))
                .build();
        StringWriter writer = new StringWriter();
        OutputPrinter printer = new MarkdownTablePrinter(columns, writer);

        printer.finish();

        String expected = "";

        assertEquals(writer.getBuffer().toString(), expected);
    }

    @Test
    public void testMarkdownPrintingHex()
            throws Exception
    {
        List<Column> columns = ImmutableList.<Column>builder()
                .add(column("first", VARCHAR))
                .add(column("binary", VARBINARY))
                .add(column("last", VARCHAR))
                .build();
        StringWriter writer = new StringWriter();
        OutputPrinter printer = new MarkdownTablePrinter(columns, writer);

        printer.printRows(rows(
                        row("hello", bytes("hello"), "world"),
                        row("a", bytes("some long text that is more than 16 bytes"), "b"),
                        row("cat", bytes(""), "dog")),
                true);
        printer.finish();

        String expected = "" +
                "| first | binary                                                                                                                           | last  |\n" +
                "| ----- | -------------------------------------------------------------------------------------------------------------------------------- | ----- |\n" +
                "| hello | 68 65 6c 6c 6f                                                                                                                   | world |\n" +
                "| a     | 73 6f 6d 65 20 6c 6f 6e 67 20 74 65 78 74 20 74<br>68 61 74 20 69 73 20 6d 6f 72 65 20 74 68 61 6e<br>20 31 36 20 62 79 74 65 73 | b     |\n" +
                "| cat   |                                                                                                                                  | dog   |\n";

        assertEquals(writer.getBuffer().toString(), expected);
    }

    @Test
    public void testMarkdownPrintingWideCharacters()
            throws Exception
    {
        List<Column> columns = ImmutableList.<Column>builder()
                .add(column("go\u7f51", VARCHAR))
                .add(column("last", VARCHAR))
                .add(column("quantity\u7f51", BIGINT))
                .build();
        StringWriter writer = new StringWriter();
        OutputPrinter printer = new MarkdownTablePrinter(columns, writer);

        printer.printRows(rows(
                        row("hello", "wide\u7f51", 123),
                        row("some long\ntext \u7f51\ndoes not\u7f51\nfit", "more\ntext", 4567),
                        row("bye", "done", -15)),
                true);
        printer.finish();

        String expected = "" +
                "| go\u7f51                                      | last         | quantity\u7f51 |\n" +
                "| ----------------------------------------- | ------------ | ----------:|\n" +
                "| hello                                     | wide\u7f51       |        123 |\n" +
                "| some long<br>text \u7f51<br>does not\u7f51<br>fit | more<br>text |       4567 |\n" +
                "| bye                                       | done         |        -15 |\n";

        assertEquals(writer.getBuffer().toString(), expected);
    }

    static Column column(String name, String type)
    {
        return new Column(name, type, new ClientTypeSignature(type));
    }

    static List<?> row(Object... values)
    {
        return asList(values);
    }

    static List<List<?>> rows(List<?>... rows)
    {
        return asList(rows);
    }

    static byte[] bytes(String s)
    {
        return s.getBytes(UTF_8);
    }
}

package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Aleksey Buzanov
 */

@RestController
@RequestMapping(value = "/java")
@Slf4j
public class ClassExecutorController {

    private static final String TEMPLATE_TO_CLEAR = "Picked up JAVA_TOOL_OPTIONS: -Xmx300m -Xss512k -XX:CICompilerCount=2 -Dfile.encoding=UTF-8";

    private String compileFile(final File file,
                               final InputStream inputStream,
                               final String[] args) throws Exception {
        log.info("compileFile({})", file.getName());
        final List<String> compileCommand = Arrays.asList(
                "javac", file.getPath());

        log.info("Compile command: {}", compileCommand);

        List<String> execCommand = new ArrayList<String>(Arrays.asList(
                "java", "-classpath", file.getParent()
                , file.getName().replace(".java", "")));
        execCommand.addAll(Arrays.asList(args));

        log.info("Exec command: {}", execCommand);

        final String compileOutput = new ProcessExecutor()
                .timeout(10000, TimeUnit.MILLISECONDS)
                .command(compileCommand)
                .destroyOnExit()
                .readOutput(true)
                .execute()
                .outputString();

        log.info("Compile output: " + compileOutput);

        final String execOutput = new ProcessExecutor()
                .timeout(10000, TimeUnit.MILLISECONDS)
                .command(execCommand)
                .destroyOnExit()
                .redirectInput(inputStream)
                .readOutput(true)
                .execute()
                .outputString();

        log.info("Exec output: " + execOutput);
        StringBuilder builder = new StringBuilder();
        builder.append(compileOutput.replace(TEMPLATE_TO_CLEAR, "").replace("\n","").trim());
        if (!builder.toString().isEmpty()) {
            builder.append("\n");
        }
        builder.append(execOutput.replace(TEMPLATE_TO_CLEAR, "").replace("\n","").trim());

        return builder.toString();
    }

    @PostMapping(value = "/compile", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String compileString(@RequestHeader(value = "args", required = false, defaultValue = "") String[] args,
                                @RequestHeader(value = "input", required = false, defaultValue = "") String[] input,
                                @RequestBody String code) throws Exception {

        if (code == null)
            throw new NullPointerException("Файл пустой");

        String classname = "";
        String[] s1 = code.split(" ");
        for (int i = 0; i < s1.length; i++) {
            if ("class".equals(s1[i]) && i + 1 < s1.length) {

                classname = s1[i + 1].split("[{]")[0];
                break;
            }
        }
        if (classname.isEmpty()) {
            throw new NullPointerException("Нет объявления класса в коде");
        }

        final File file = new File(System.getProperty("java.io.tmpdir") +
                System.getProperty("file.separator") +
                UUID.randomUUID().toString() +
                System.getProperty("file.separator") +
                classname.concat(".java"));
        FileUtils.forceMkdirParent(file);
        if (!file.exists())
            file.createNewFile();
        try (BufferedOutputStream stream = new BufferedOutputStream
                (new FileOutputStream(file))) {
            stream.write(code.getBytes());
        }
        final String str = Arrays
                .stream(input)
                .map(s -> s.concat(System.lineSeparator()))
                .collect(Collectors.joining());
        InputStream is = new ByteArrayInputStream(str.getBytes());
        String output = compileFile(file, is, args);
        FileUtils.deleteDirectory(file.getParentFile());
        return output;
    }

//    @PostMapping(value = "/loadMainClass")
//    public ResponseEntity<String> loadMainClass(MultipartHttpServletRequest data) throws Exception {
//        final MultipartFile mfile = data.getFile("file");
//        if (mfile == null)
//            throw new NullPointerException("Файл пустой");
//        byte[] bytes = mfile.getBytes();
//
//        final java.lang.ClassLoader classLoader = ClassLoader.getSystemClassLoader();
//
//        final Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
//        theUnsafeField.setAccessible(true);
//        final Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
//        final Class<?> myCustomInvocator = unsafe.defineClass(null, bytes, 0,
//                bytes.length, classLoader, null);
//
//        Reflection.runMainInSameVM(myCustomInvocator, null);
//
//        return ResponseEntity.ok("Выполнено успешно");
//    }
}

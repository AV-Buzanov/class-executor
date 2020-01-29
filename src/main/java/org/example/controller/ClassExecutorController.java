package org.example.controller;

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
public class ClassExecutorController {

    private String compileFile(final File file,
                               final InputStream inputStream,
                               final String[] args) throws Exception {
        final List<String> compileCommand = Arrays.asList("cmd", "/c",
                "\"C:\\Program Files\\Java\\jdk1.8.0_192\\bin\\javac.exe\" " + file.getPath());

        List<String> execCommand = new ArrayList<String>(Arrays.asList("cmd", "/c"
                , "java", "-classpath", file.getParent()
                , file.getName().replace(".java", "")));
        execCommand.addAll(Arrays.asList(args));

        final String compileOutput = new ProcessExecutor()
                .timeout(10000, TimeUnit.MILLISECONDS)
                .command(compileCommand)
                .readOutput(true)
                .execute()
                .outputString();

        final String execOutput = new ProcessExecutor()
                .timeout(10000, TimeUnit.MILLISECONDS)
                .command(execCommand)
                .redirectInput(inputStream)
                .readOutput(true)
                .execute()
                .outputString();
        return compileOutput.concat(execOutput);
    }

    @PostMapping(value = "/compile", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String compileString(@RequestHeader("args") String[] args,
                                @RequestHeader("input") String[] input,
                                @RequestHeader("classname") String classname,
                                @RequestBody String code) throws Exception {
        if (code == null)
            throw new NullPointerException("Файл пустой");
        final File file = new File(System.getProperty("user.home") +
                System.getProperty("file.separator") +
                UUID.randomUUID().toString() +
                System.getProperty("file.separator") +
                classname.concat(".java"));
        FileUtils.forceMkdirParent(file);
        if (!file.exists())
            file.createNewFile();
        byte[] bytes = code.getBytes();
        try (BufferedOutputStream stream = new BufferedOutputStream
                (new FileOutputStream(file))) {
            stream.write(bytes);
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

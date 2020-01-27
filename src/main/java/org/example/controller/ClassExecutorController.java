package org.example.controller;

import org.aspectj.util.Reflection;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.zeroturnaround.exec.ProcessExecutor;
import sun.misc.Unsafe;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Aleksey Buzanov
 */

@RestController
@RequestMapping(value = "/file")
public class ClassExecutorController {

    @PostMapping(value = "/compile")
    public String compile(MultipartHttpServletRequest data) throws Exception {
        data.getFileNames().forEachRemaining(System.out::println);
        MultipartFile mfile = data.getFile("file");
        if (mfile == null)
            throw new NullPointerException("Файл пустой");
        File file = new File("D:\\com\\" + mfile.getOriginalFilename());
        if (!file.exists())
            file.createNewFile();
        byte[] bytes = mfile.getBytes();
        BufferedOutputStream stream =
                new BufferedOutputStream(new FileOutputStream(file));
        stream.write(bytes);
        stream.close();
        List<String> commands = Arrays.asList("cmd", "/c", "D:\\com\\compile.bat");
// or Arrays.asList("/bin/sh", "yourScript.sh");

        String output = new ProcessExecutor()
                .command(commands)
                .readOutput(true)
                .execute()
                .outputString();
//        int i = Runtime.getRuntime().exec("cmd java -jar D:\\com\\ClassExecutor-1.0-SNAPSHOT.jar").exitValue();

        return "ok"+ output;
    }

    @PostMapping(value = "/loadCallable")
    public List<Object> loadCallable(MultipartHttpServletRequest data) throws Exception {
        final MultipartFile mfile = data.getFile("file");
        if (mfile == null)
            throw new NullPointerException("Файл пустой");
        byte[] bytes = mfile.getBytes();

        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        final Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        final Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
        final Class<?> myCustomInvocator = unsafe.defineClass(null, bytes, 0,
                bytes.length, classLoader, null);
        boolean isCallable = Arrays.stream(myCustomInvocator.getInterfaces()).anyMatch(s->"java.util.concurrent.Callable".equals(s.getName()));
        if (!isCallable)
            throw new NullPointerException("Класс не Callable");

        Integer count = Integer.parseInt(data.getRequestHeaders().get("count").get(0));

        ExecutorService executorService = Executors.newFixedThreadPool(count);
        List<Future> list = new ArrayList<>();

        Callable<Object> callable = (Callable<Object>) myCustomInvocator.newInstance();

        for (int i=0; i<count; i++){
            list.add(executorService.submit(callable));
        }

         Stream<Object> objectStream = list.stream().map(future -> {
             try {
                 return future.get();
             } catch (InterruptedException | ExecutionException e) {
                 e.printStackTrace();
             }
             return null;
         });
        return objectStream.collect(Collectors.toList());
    }

    @PostMapping(value = "/loadMainClass")
    public ResponseEntity<String> loadMainClass(MultipartHttpServletRequest data) throws Exception {
        final MultipartFile mfile = data.getFile("file");
        if (mfile == null)
            throw new NullPointerException("Файл пустой");
        byte[] bytes = mfile.getBytes();

        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        final Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        final Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
        final Class<?> myCustomInvocator = unsafe.defineClass(null, bytes, 0,
                bytes.length, classLoader, null);

        Reflection.runMainInSameVM(myCustomInvocator, null);

        return ResponseEntity.ok("Выполнено успешно");

    }

    @PostMapping(value = "/loadAndSave")
    public ResponseEntity<String> loadAndSave(MultipartHttpServletRequest data) throws Exception {
        data.getFileNames().forEachRemaining(System.out::println);
        MultipartFile mfile = data.getFile("file");
        if (mfile == null)
            throw new NullPointerException("Файл пустой");
        File file = new File("D:\\com\\" + mfile.getOriginalFilename());
        if (!file.exists())
            file.createNewFile();
        byte[] bytes = mfile.getBytes();
        BufferedOutputStream stream =
                new BufferedOutputStream(new FileOutputStream(file));
        stream.write(bytes);
        stream.close();

        return ResponseEntity.ok("Файл успешно загружен");

    }
}

package org.example.controller;

import org.aspectj.util.Reflection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import sun.misc.Unsafe;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

/**
 * @author Aleksey Buzanov
 */

@RestController
@RequestMapping(value = "/file")
public class ClassExecutorController {

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

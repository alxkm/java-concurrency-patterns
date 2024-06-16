package org.alxkm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException {
        List<Class<?>> classesByPackage = PackageUtils.getClassesByPackage("ua.com.alxkm");
        StringBuilder sb = new StringBuilder();
        for (Class<?> clazz : classesByPackage) {
            sb.append(clazz.getPackageName()+"    "+clazz.getSimpleName()).append("\n");
        }
        System.out.println(sb);
    }
}

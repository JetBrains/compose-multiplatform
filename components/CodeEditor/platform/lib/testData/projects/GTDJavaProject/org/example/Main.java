package org.example;

import org.example.a.ClassA;

public class Main {

    private static ClassA classA;

    public static void main(String[] args) {
        classA = new ClassA();
        classA.set(5);
        System.out.println(classA.field);
    }
}

// (class type) 94 (ClassA.java) 37
// (local field) 165 (Main.java) 99
// (constructor) 177 (ClassA.java) 58
// (local field) 195 (Main.java) 99
// (method) 201 (ClassA.java) 166
// (local field) 240 (Main.java) 99
// (field) 244 (ClassA.java) 86

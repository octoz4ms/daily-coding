package com.octo.cssb;

public class HelloService {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void sayHello() {
        System.out.println("Hello " + name);
    }

    public HelloService(String name) {
        this.name = name;
    }
}

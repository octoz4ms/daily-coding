package com.example.ssd.controller;

import com.example.ssd.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/product")
public class ProductController {

    @PostMapping("/import/excel")
    public String importExcel(@RequestParam("file") MultipartFile file,
                              @ModelAttribute User user,
                              @RequestParam("path") String path) {
        System.out.println("importExcel start..." + file.getOriginalFilename());
        System.out.println(user);
        System.out.println(path);
        return "success";
    }

    @PostMapping("/import/excel/v2")
    public String importExcelV2(@RequestParam("file") MultipartFile file,
                              @RequestPart("user") User user,
                              @RequestParam("path") String path) {
        System.out.println("importExcel start..." + file.getOriginalFilename());
        System.out.println(user);
        System.out.println(path);
        return "success";
    }
}

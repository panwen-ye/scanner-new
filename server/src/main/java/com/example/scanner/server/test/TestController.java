package com.example.scanner.server.test;


import com.example.scanner.server.mappers.ScannerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private ScannerMapper scannerMapper;

    @PostMapping("/fail")
    public void fail(@RequestBody String sql) {
        scannerMapper.executeSql(sql);
    }
}

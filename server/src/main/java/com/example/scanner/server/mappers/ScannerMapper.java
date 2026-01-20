package com.example.scanner.server.mappers;


import com.example.scanner.server.domain.ScanTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScannerMapper {


    void executeSql(@Param("sql") String sql);


    ScanTask queryScanTask(@Param("taskId") long taskId);
}

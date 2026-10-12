package org.example.mybatisplus.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.mybatisplus.entity.BatchDemo;
import org.example.mybatisplus.mapper.BatchDemoMapper;
import org.example.mybatisplus.service.BatchDemoService;
import org.springframework.stereotype.Service;

@Service
public class BatchDemoServiceImpl extends ServiceImpl<BatchDemoMapper, BatchDemo> implements BatchDemoService {
}

package com.jerome.demo.service.impl;

import com.jerome.mvcframework.annotation.LsjService;
import com.jerome.demo.service.IDemoService;

/**
 * @author liusj
 */
@LsjService
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name;
    }
}

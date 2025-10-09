/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.payload.response;

import java.io.Serializable;

/**
 *
 * @author MalickMoro-Samah
 */
public class ApiResponse implements Serializable {

    private String status;
    private String msg;
    private String code;
    private Object data;
    private boolean found;
    private boolean success;

    public ApiResponse() {
    }

    public ApiResponse(String status, String msg, String code, Object data, boolean found) {
        this.status = status;
        this.msg = msg;
        this.code = code;
        this.data = data;
        this.found = found;
    }
    
    public ApiResponse(String status, String msg, String code, Object data, boolean found, boolean success) {
        this.status = status;
        this.msg = msg;
        this.code = code;
        this.data = data;
        this.found = found;
        this.success = success;
    }

    public String getStatus() {
        return this.status;
    }

    public String getMsg() {
        return this.msg;
    }

    public String getCode() {
        return this.code;
    }

    public Object getData() {
        return this.data;
    }

    public boolean isFound() {
        return this.found;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public boolean isSuccess() {
        return success;
    }
    

    @Override
    public String toString() {
        return "ApiResponse(status=" + this.getStatus() + ", msg=" + this.getMsg() + ", code=" + this.getCode() + ", data=" + this.getData() + ", found=" + this.isFound() + ")";
    }
}


package com.myproject.app.model;

/**
 * Created by fatahzull on 06/02/2017.
 */

public interface IMultipartProgressListener {
    void transferred(long transferred, int progress);
}
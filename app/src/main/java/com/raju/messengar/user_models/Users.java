package com.raju.messengar.user_models;

import com.google.common.util.concurrent.Service;

import java.io.Serializable;

public class Users implements Serializable {
    //Serializable interface is a markup interface which means the class which implements this interface don't have to implement any methods
    //And another thing is serialization is a process that converts state of class into byte stream and deserialization is a reverse process
    //Serialization allows us to transfer objects through a network by converting it into a byte stream. It also helps in preserving the state of the object
    public String name,email,image,token,id;
}

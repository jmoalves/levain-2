package com.github.jmoalves.levain.service;

/**
 * Exception thrown when attempting to install a package that is already
 * installed.
 */
public class AlreadyInstalledException extends RuntimeException {
    public AlreadyInstalledException(String message) {
        super(message);
    }
}

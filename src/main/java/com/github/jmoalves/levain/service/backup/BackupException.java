package com.github.jmoalves.levain.service.backup;

/**
 * Exception thrown when backup operations fail.
 */
public class BackupException extends RuntimeException {
    
    public BackupException(String message) {
        super(message);
    }
    
    public BackupException(String message, Throwable cause) {
        super(message, cause);
    }
}

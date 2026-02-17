package com.github.jmoalves.levain.service.backup;

/**
 * Exception thrown when there is insufficient disk space for backup operation.
 */
public class InsufficientSpaceException extends BackupException {
    
    private final long required;
    private final long available;
    
    public InsufficientSpaceException(long required, long available) {
        super(String.format("Insufficient disk space. Required: %d bytes, Available: %d bytes",
            required, available));
        this.required = required;
        this.available = available;
    }
    
    public long getRequired() {
        return required;
    }
    
    public long getAvailable() {
        return available;
    }
}

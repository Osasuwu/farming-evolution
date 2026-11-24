package io.github.osasuwu.farmingevolution.breeding;

/** Pack data that cannot be used; the message names the entry and what is wrong with it. */
public final class PackDataException extends RuntimeException {
    public PackDataException(String message) {
        super(message);
    }
}

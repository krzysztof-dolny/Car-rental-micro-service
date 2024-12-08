package org.bp.carrental.exceptions;

public class CarException extends Exception {

	public CarException() {
	}

	public CarException(String message) {
		super(message);
	}

	public CarException(Throwable cause) {
		super(cause);
	}

	public CarException(String message, Throwable cause) {
		super(message, cause);
	}

	public CarException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}

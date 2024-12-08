package org.bp.carrental.exceptions;

public class ParkingException extends Exception {

	public ParkingException() {
	}

	public ParkingException(String message) {
		super(message);
	}

	public ParkingException(Throwable cause) {
		super(cause);
	}

	public ParkingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParkingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}

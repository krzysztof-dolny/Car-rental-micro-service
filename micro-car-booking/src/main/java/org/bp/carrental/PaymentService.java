package org.bp.carrental;


import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.bp.carrental.model.BookCarRentalRequest;
import org.bp.carrental.model.BookingInfo;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
	private HashMap<String, PaymentData> payments;
	
	@PostConstruct
	void init() {
		payments=new HashMap<>();
	}
	
	public static class PaymentData {
		BookCarRentalRequest bookCarRentalRequest;
		BookingInfo carBookingInfo;
		BookingInfo parkingBookingInfo;
		public boolean isReady() {
			return bookCarRentalRequest!=null && carBookingInfo!=null && parkingBookingInfo!=null;
		}
	}
	
	public synchronized boolean addBookCarRentalRequest(String bookCarRentalId, BookCarRentalRequest bookCarRentalRequest) {
		PaymentData paymentData = getPaymentData(bookCarRentalId);
		paymentData.bookCarRentalRequest=bookCarRentalRequest;		
		return paymentData.isReady();
	}
	

	public synchronized boolean addBookingInfo(String bookCarRentalId, BookingInfo bookingInfo, String serviceType) {
		PaymentData paymentData = getPaymentData(bookCarRentalId);
		if (serviceType.equals("parking"))
			paymentData.parkingBookingInfo=bookingInfo;
		else 
			paymentData.carBookingInfo=bookingInfo;		
		return paymentData.isReady();
	}	
	
	
	public synchronized PaymentData getPaymentData(String bookCarRentalId) {
		PaymentData paymentData = payments.get(bookCarRentalId);
		if (paymentData==null) {
			paymentData = new PaymentData();
			payments.put(bookCarRentalId, paymentData);
		}
		return paymentData;
	}
}
package org.bp.carrental;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import static org.apache.camel.model.rest.RestParamType.body;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.bp.carrental.model.BookCarRentalRequest;
import org.bp.carrental.model.BookingInfo;
import org.bp.carrental.model.Utils;
import org.springframework.stereotype.Component;
import org.bp.carrental.exceptions.ParkingException;
import org.bp.carrental.exceptions.CarException;
import org.bp.carrental.model.ExceptionResponse;
import org.bp.carrental.state.ProcessingEvent;
import org.bp.carrental.state.ProcessingState;
import org.bp.carrental.state.StateService;


@Component
public class CarRentalBookingService extends RouteBuilder {

	@org.springframework.beans.factory.annotation.Autowired
	BookingIdentifierService bookingIdentifierService;
	
	@org.springframework.beans.factory.annotation.Autowired
	PaymentService paymentService;
	
	@org.springframework.beans.factory.annotation.Autowired
	StateService parkingStateService;
	
	@org.springframework.beans.factory.annotation.Autowired
	StateService carStateService;
	
	@org.springframework.beans.factory.annotation.Value("${carrental.kafka.server}")
	private String carrentalKafkaServer;
	@org.springframework.beans.factory.annotation.Value("${carrental.service.type}")
	private String carrentalServiceType;

	
	@Override
	public void configure() throws Exception {
		if (carrentalServiceType.equals("all") || carrentalServiceType.equals("car"))
		bookCarExceptionHandlers();
		if (carrentalServiceType.equals("all") || carrentalServiceType.equals("parking"))
		bookParkingExceptionHandlers();
		if (carrentalServiceType.equals("all") || carrentalServiceType.equals("gateway"))
		gateway();
		if (carrentalServiceType.equals("all") || carrentalServiceType.equals("car"))
		car();
		if (carrentalServiceType.equals("all") || carrentalServiceType.equals("parking"))
		parking();
		if (carrentalServiceType.equals("all") || carrentalServiceType.equals("payment"))
		payment();
	}
	
	private void bookCarExceptionHandlers() {
		onException(CarException.class)
		.process((exchange) -> {
			ExceptionResponse er = new ExceptionResponse();
			er.setTimestamp(OffsetDateTime.now());
			Exception cause =
					exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
			er.setMessage(cause.getMessage());
			exchange.getMessage().setBody(er);
		}
		)
		 .marshal().json()
		.to("stream:out")
		.setHeader("serviceType", constant("car"))
		.to("kafka:CarRentalBookingFailTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType)
		 //Processing to CSV 
		.process(exchange -> {
            ExceptionResponse exceptionResponse = exchange.getMessage().getBody(ExceptionResponse.class);
            String csvData = String.format("\"%s\"; \"%s\"%n", "Brak samochodow", "w tym kraju");
            exchange.getMessage().setBody(csvData);
        })
        .to("file:///app/data?fileName=carrental_notifications.csv&fileExist=Append")
		// END
		.handled(true);
	}

	
	private void bookParkingExceptionHandlers() {
		onException(ParkingException.class)
		.process((exchange) -> {
			ExceptionResponse er = new ExceptionResponse();
			er.setTimestamp(OffsetDateTime.now());
			Exception cause =
					exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
			er.setMessage(cause.getMessage());
			exchange.getMessage().setBody(er);
		}
		)
		 .marshal().json()
		.to("stream:out")
		.setHeader("serviceType", constant("parking"))
		.to("kafka:CarRentalBookingFailTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType)
		// Processing to CSV 
		.process(exchange -> {
            ExceptionResponse exceptionResponse = exchange.getMessage().getBody(ExceptionResponse.class);
            String csvData = String.format("\"%s\"; \"%s\"%n", "Brak mozliwosci wypozyczania", "w tym miescie");
            exchange.getMessage().setBody(csvData);
         })
        .to("file:///app/data?fileName=carrental_notifications.csv&fileExist=Append")
		// END 
		.handled(true)
		;
	}
	
	// TODO
	// Dodanie metod notificationToFile i logExceptionToFile
	//
	
	private void gateway() {
		restConfiguration()
		 .component("servlet")
		 .bindingMode(RestBindingMode.json)
		 .dataFormatProperty("prettyPrint", "true")
		 .enableCORS(true)
		 .contextPath("/api")
		 // turn on swagger api-doc
		 .apiContextPath("/api-doc")
		 .apiProperty("api.title", "Micro Car booking API")
		 .apiProperty("api.version", "1.0.0");

		rest("/carrental").description("Micro Car booking REST service")
		.consumes("application/json")
		.produces("application/json")
		.post("/booking").description("Book a car").type(BookCarRentalRequest.class).outType(BookingInfo.class)
		.param().name("body").type(body).description("The car to book").endParam()
		.responseMessage().code(200).message("Car successfully booked").endResponseMessage()
		.to("direct:bookCarRental");
		
		
		// Dodanie punktu końcowego REST
		rest("/notification")
	    .description("Notification endpoint")
	    .get()
	    .produces("application/json")
	    .to("direct:getNotification");
		//END
		
		from("direct:bookCarRental").routeId("bookCarRental")
		.log("bookCarRental fired")
		.process((exchange) -> {
			exchange.getMessage().setHeader("bookingCarRentalId",
					bookingIdentifierService.getBookingIdentifier());
		})
		.to("direct:CarRentalBookRequest")
		.to("direct:bookRequester");
		
		from("direct:bookRequester").routeId("bookRequester")
		.log("bookRequester fired")
		.process(
		(exchange) -> {
		exchange.getMessage().setBody(Utils.prepareBookingInfo(
		exchange.getMessage().getHeader("bookingCarRentalId", String.class), null));
		});
		
		from("direct:CarRentalBookRequest").routeId("CarRentalBookRequest")
		.log("brokerTopic fired")
		.marshal().json()
		.to("kafka:CarRentalReqTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType)
		;
	}

	private void car() {
		from("kafka:CarRentalReqTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType).routeId("bookCar")
		.log("fired bookCar")
		.unmarshal().json(JsonLibrary.Jackson, BookCarRentalRequest.class)
		.process(
		(exchange) -> {
			String bookingCarRentalId =
					exchange.getMessage().getHeader("bookingCarRentalId", String.class);
				ProcessingState previousState =
						carStateService.sendEvent(bookingCarRentalId, ProcessingEvent.START);
				if (previousState!=ProcessingState.CANCELLED) { 
					BookingInfo bi = new BookingInfo();
					bi.setId(bookingIdentifierService.getBookingIdentifier());
					BookCarRentalRequest btr= exchange.getMessage().getBody(BookCarRentalRequest.class);
					if (btr!=null && btr.getCar()!=null
							&& btr.getCar().getCountry()!=null ) {
						String country = btr.getCar().getCountry();
						if (country.equals("Poland")) {
							bi.setCost(new BigDecimal(500));
						}
						else if (country.equals("Germany")){
							bi.setCost(new BigDecimal(1000));
						}
						else {
							throw new CarException("Not serviced destination: "+country);
						}
					}
					exchange.getMessage().setBody(bi);
					previousState = carStateService.sendEvent(bookingCarRentalId,
							ProcessingEvent.FINISH);
				}
				exchange.getMessage().setHeader("previousState", previousState);
			}
			)
			.marshal().json()
			.to("stream:out")
			.choice()
				.when(header("previousState").isEqualTo(ProcessingState.CANCELLED))
				.to("direct:bookCarCompensationAction")
			.otherwise()
				.setHeader("serviceType", constant("car"))
				.to("kafka:BookingInfoTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType)
			.endChoice()
			;
		
		from("kafka:CarRentalBookingFailTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType).routeId("bookCarCompensation")
				.log("fired bookCarCompensation")
				.unmarshal().json(JsonLibrary.Jackson, ExceptionResponse.class)
				.choice()
					.when(header("serviceType").isNotEqualTo("car"))
					.process((exchange) -> {
						String bookingCarRentalId = exchange.getMessage().getHeader("bookingCarRentalId",
								String.class);
						ProcessingState previousState = carStateService.sendEvent(bookingCarRentalId,
								ProcessingEvent.CANCEL);
						exchange.getMessage().setHeader("previousState", previousState);
					})
					.choice()
					.when(header("previousState").isEqualTo(ProcessingState.FINISHED))
					.to("direct:bookCarCompensationAction")
				.endChoice()
			.endChoice();
				
			from("direct:bookCarCompensationAction").routeId("bookCarCompensationAction")
			.log("fired bookCarCompensationAction")
			.to("stream:out");
	}
	
	private void parking() {
		from("kafka:CarRentalReqTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType).routeId("bookParking")
		.log("fired bookParking")
		.unmarshal().json(JsonLibrary.Jackson, BookCarRentalRequest.class)
		.process(
				(exchange) -> {
					String bookingCarRentalId =
							exchange.getMessage().getHeader("bookingCarRentalId", String.class);
							ProcessingState previousState =
							parkingStateService.sendEvent(bookingCarRentalId, ProcessingEvent.START);
							if (previousState!=ProcessingState.CANCELLED) {
								BookingInfo bi = new BookingInfo();
								bi.setId(bookingIdentifierService.getBookingIdentifier());
								BookCarRentalRequest btr= exchange.getMessage().getBody(BookCarRentalRequest.class);
								if (btr!=null && btr.getParking()!=null && btr.getParking().getFrom()!=null
										&& btr.getParking().getFrom().getAirport()!=null) {
									String from=btr.getParking().getFrom().getAirport();
									if (from.equals("Berlin")) {
										bi.setCost(new BigDecimal(900));
									}
									else if (from.equals("Warsaw")) {
										throw new ParkingException("Not serviced city: "+from);
									}
									else {
										bi.setCost(new BigDecimal(600));
									}
								}
								exchange.getMessage().setBody(bi);
								previousState = parkingStateService.sendEvent(bookingCarRentalId,
										ProcessingEvent.FINISH);
										 }
							exchange.getMessage().setHeader("previousState", previousState);
				}
				)
		.marshal().json()
		.to("stream:out")
		.choice()
			.when(header("previousState").isEqualTo(ProcessingState.CANCELLED))
			.to("direct:bookParkingCompensationAction")
		.otherwise()
			.setHeader("serviceType", constant("parking"))
			.to("kafka:BookingInfoTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType)
		.endChoice()
		;
		
		from("kafka:CarRentalBookingFailTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType).routeId("bookParkingCompensation")
		.log("fired bookParkingCompensation")
		.unmarshal().json(JsonLibrary.Jackson, ExceptionResponse.class)
		.choice()
			.when(header("serviceType").isNotEqualTo("parking"))
			.process((exchange) -> {
				String bookingCarRentalId = exchange.getMessage().getHeader("bookingCarRentalId", String.class);
				ProcessingState previousState = parkingStateService.sendEvent(bookingCarRentalId,
						ProcessingEvent.CANCEL);
				exchange.getMessage().setHeader("previousState", previousState);
			})
			.choice()
			.when(header("previousState").isEqualTo(ProcessingState.FINISHED))
			.to("direct:bookParkingCompensationAction")
			.endChoice()
		.endChoice();
		
		from("direct:bookParkingCompensationAction").routeId("bookParkingCompensationAction")
		.log("fired bookParkingCompensationAction")
		.to("stream:out");
	}
	
	// Dodanie funkcji notification
	private void notification() {
	    rest("/notification")
	        .description("Notification endpoint")
	        .get()
	        .produces("application/json")
	        .to("direct:getNotification");
	    
	    from("direct:getNotification").routeId("getNotification")
	    .log("Fetching notification data")
	    .setBody(exchange -> {
	        // Assuming notification data is set earlier in the process (for example, after payment)
	        BookingInfo bookingInfo = exchange.getMessage().getBody(BookingInfo.class);
	        if (bookingInfo != null) {
	            return bookingInfo;  // Return the data as the response
	        } else {
	            return "No booking information available.";  // Fallback response
	        }
	    })
	    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
	    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));  // Set proper HTTP response code

	}
	//END
	
	private void payment() {
		from("kafka:BookingInfoTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType).routeId("paymentBookingInfo")
		.log("fired paymentBookingInfo")
		.unmarshal().json(JsonLibrary.Jackson, BookingInfo.class)
		.process(
				(exchange) -> {
					String bookingCarRentalId =
							exchange.getMessage().getHeader("bookingCarRentalId", String.class);
					boolean isReady= paymentService.addBookingInfo(
							bookingCarRentalId,
							exchange.getMessage().getBody(BookingInfo.class),
							exchange.getMessage().getHeader("serviceType", String.class));
					exchange.getMessage().setHeader("isReady", isReady);
				}
				)
		.choice()
			.when(header("isReady").isEqualTo(true)).to("direct:finalizePayment")
		.endChoice();
		
		from("kafka:CarRentalReqTopic?brokers=" + carrentalKafkaServer + "&groupId=" + carrentalServiceType).routeId("paymentCarRentalReq")
		.log("fired paymentCarRentalReq")
		.unmarshal().json(JsonLibrary.Jackson, BookCarRentalRequest.class) 
		.process(
				(exchange) -> {
					String bookingCarRentalId = exchange.getMessage().getHeader("bookingCarRentalId", String.class);
					 boolean isReady= paymentService.addBookCarRentalRequest(
							bookingCarRentalId,
							exchange.getMessage().getBody(BookCarRentalRequest.class));
					 exchange.getMessage().setHeader("isReady", isReady);
				}
				)
		.choice()
			.when(header("isReady").isEqualTo(true)).to("direct:finalizePayment")
		.endChoice();
		
		from("direct:finalizePayment").routeId("finalizePayment")
		.log("fired finalizePayment")
		.process(
				(exchange) -> {
					String bookingCarRentalId = exchange.getMessage().
							getHeader("bookingCarRentalId", String.class);
					PaymentService.PaymentData paymentData =
							paymentService.getPaymentData(bookingCarRentalId);
					BigDecimal carCost=paymentData.carBookingInfo.getCost();
					BigDecimal parkingCost=paymentData.parkingBookingInfo.getCost();
					BigDecimal totalCost=carCost.add(parkingCost);
					BookingInfo carrentalBookingInfo = new BookingInfo();
					carrentalBookingInfo.setId(bookingCarRentalId);
					carrentalBookingInfo.setCost(totalCost);
					exchange.getMessage().setBody(carrentalBookingInfo);
				}
				)
		.to("direct:notification")
		;
		
		// Zakomentowany oryginalny kod
		//from("direct:notification").routeId("notification")
		//.log("fired notification")
		//.to("stream:out");
		// END
		
		from("direct:notification").routeId("notification")
	    .log("fired notification")
	    .to("stream:out")
	    .log("Converting notification data to CSV format")
	    .process(exchange -> {
	        BookingInfo bookingInfo = exchange.getMessage().getBody(BookingInfo.class);
	        String csvData = String.format("\"%s\"; %s%n", 
	                          bookingInfo.getId(), 
	                          bookingInfo.getCost()); // %n for platform-independent newline
	        exchange.getMessage().setBody(csvData);
	    })
	    .log("Generated CSV data: ${body}")
	    .to("file:///app/data?fileName=carrental_notifications.csv&fileExist=Append");
	}
}

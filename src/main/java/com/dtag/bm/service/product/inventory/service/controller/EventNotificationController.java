package com.dtag.bm.service.product.inventory.service.controller;



import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dtag.bm.service.product.inventory.service.model.EventData;
import com.dtag.bm.service.product.inventory.service.service.EventService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Stopwatch;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/productInventoryManagement/v1")
@JsonIgnoreProperties(ignoreUnknown = true)

public class EventNotificationController {
	
	private static Logger logger = LoggerFactory.getLogger(EventNotificationController.class);
	
	@Autowired
	private EventService  eventService ;
	
	private final String className = this.getClass().getSimpleName();
	
	/**
	 * This will create Event
	 * 
	 * @return eventData
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	
	@PostMapping("/hub")
	public ResponseEntity<?> createNotification(@RequestBody EventData eventData) {
		ResponseEntity<?> responseEntity = null;
		try {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
			
		logger.info(className+ " " +methodName);
		Stopwatch stopwatch = Stopwatch.createStarted();
					
		EventData data = eventService.createNotification(eventData);
		stopwatch.stop();
		logger.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		responseEntity = new ResponseEntity<>(data, HttpStatus.CREATED);
		}catch(Exception exception) {
			exception.printStackTrace();
		}
		return responseEntity;
	}
	/**
	 * This will delete EventById
	 * 
	 * @return response
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@DeleteMapping("/deleteById/{id}")
	public ResponseEntity<?> deleteEvent(@PathVariable(value ="id")String id) {
		ResponseEntity<?> responseEntity = null;
		try {
			final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
			
			logger.info(className+ " " +methodName);
			Stopwatch stopwatch = Stopwatch.createStarted();
			
		String message = eventService.deleteEvent(id);
		stopwatch.stop();
		logger.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS)+200+"");
		
		responseEntity = new ResponseEntity<>(message, HttpStatus.OK);
		}catch(Exception exception) {
			exception.printStackTrace();
		}
		return responseEntity;
	}
	
	/**
	 * This will get AllEvents
	 * 
	 * @return List of EventData
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@GetMapping("/hub/getEvents")
	public ResponseEntity<?> getEvents() {
		ResponseEntity<?> responseEntity = null;
		try {
        final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
			
		logger.info(className+ " " +methodName);
		Stopwatch stopwatch = Stopwatch.createStarted();
			
		List<EventData> eventDatas = eventService.getEvents();
		stopwatch.stop();
		logger.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		responseEntity = new ResponseEntity<>(eventDatas, HttpStatus.OK);
		}catch(Exception exception) {
			exception.printStackTrace();
		}
		return responseEntity;
	}
}

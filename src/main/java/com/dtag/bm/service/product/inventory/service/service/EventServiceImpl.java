package com.dtag.bm.service.product.inventory.service.service;



import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.dtag.bm.service.product.inventory.service.model.EventData;
import com.dtag.bm.service.product.inventory.service.model.Product;
import com.dtag.bm.service.product.inventory.service.model.ProductOrderNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Stopwatch;


@Service
public class EventServiceImpl implements EventService,ApplicationListener<ProductOrderNotification> {
	private static Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);
	
	private final String className = this.getClass().getSimpleName();
	
	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public EventData createNotification(EventData eventData) {
		logger.info(className+ " " +"save");
		Stopwatch stopwatch = Stopwatch.createStarted();
		
		mongoTemplate.save(eventData);

		stopwatch.stop();
		logger.info(className+" "+"save"+" "+stopwatch.elapsed(TimeUnit.MILLISECONDS));
		logger.info("Response " + eventData);
		return eventData;
	}

	@Override
	public String deleteEvent(String id) {
		Query query = new Query();
		query.addCriteria(Criteria.where("id").is(id));
		
		logger.info(className+ " " +"remove");
		Stopwatch stopwatch = Stopwatch.createStarted();
		mongoTemplate.remove(query, EventData.class);
		stopwatch.stop();
		logger.info(className+" "+"remove"+" "+stopwatch.elapsed(TimeUnit.MILLISECONDS));
	
		return  "deleted successfully";
		
	}

	@Override
	public List<EventData> getEvents() {
	 return mongoTemplate.findAll(EventData.class);
	}

	@Override
	public void onApplicationEvent(ProductOrderNotification event) {
		ProductOrderNotification productOrderNotification = (ProductOrderNotification) event;
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

		try {
			String jsonString = mapper.writeValueAsString(productOrderNotification);
			logger.info("Event " + jsonString);
			List<EventData> EventData = getEvents();
			for (EventData EventData2 : EventData) {
				String callback = EventData2.getCallback();
				logger.info("callback url :" + callback);
				// Logic to post event on callback url goes here

			}

		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
